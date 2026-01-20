package itsi.api.steuerung.service;

import com.cedarpolicy.AuthorizationEngine;
import com.cedarpolicy.BasicAuthorizationEngine;
import com.cedarpolicy.model.AuthorizationRequest;
import com.cedarpolicy.model.AuthorizationResponse;
import com.cedarpolicy.model.exception.InternalException;
import com.cedarpolicy.model.slice.Entity;
import com.cedarpolicy.model.slice.PolicySet;
import com.cedarpolicy.model.slice.Slice;
import com.cedarpolicy.value.EntityUID;
import com.cedarpolicy.value.PrimLong;
import com.cedarpolicy.value.PrimString;
import com.cedarpolicy.value.Value;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Year;
import java.util.*;

@Service
@ConditionalOnProperty(name = "cedar.enabled", havingValue = "true")
public class CedarService {

    private static final Logger logger = LoggerFactory.getLogger(CedarService.class);

    private AuthorizationEngine authEngine;
    private PolicySet policySet;

    @org.springframework.beans.factory.annotation.Value("classpath:policy/CedarPolicy.cedar")
    private Resource policyResource;

    public CedarService() {
        // Konstruktor bleibt leer - Initialisierung erfolgt in @PostConstruct
    }

    @PostConstruct
    private void initialize() {
        try {
            logger.info("Initializing Cedar authorization engine...");
            this.authEngine = new BasicAuthorizationEngine();
            loadPolicies();
            logger.info("Cedar authorization engine initialized successfully");
        } catch (UnsatisfiedLinkError e) {
            logger.error("Failed to load Cedar native library. Make sure the cedar_java_ffi library is in your java.library.path", e);
            throw new RuntimeException("Cedar native library not found", e);
        }
    }

    private void loadPolicies() {
        try {
            String policies = new String(
                    policyResource.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );
            this.policySet = PolicySet.parsePolicies(policies);
            logger.info("Cedar policies loaded successfully");
        } catch (IOException e) {
            logger.error("Failed to load Cedar policies from classpath", e);
            throw new RuntimeException("Failed to load Cedar policies", e);
        } catch (InternalException e) {
            logger.error("Failed to parse Cedar policies", e);
            throw new RuntimeException("Failed to parse Cedar policies", e);
        }
    }

    /**
     * Überprüft ob ein User basierend auf den JWT Claims autorisiert ist
     * @param claims Die JWT Token Claims
     * @return true wenn der User autorisiert ist (z.B. nicht abgelaufen)
     */
    public boolean isUserAuthorized(Claims claims) {
        try {
            // Erstelle Principal Entity aus JWT Claims
            EntityUID principalUID = EntityUID.parse("User::\"" + claims.getSubject() + "\"")
                .orElseThrow(() -> new IllegalArgumentException("Invalid principal UID"));

            Map<String, Value> principalAttrs = new HashMap<>();
            principalAttrs.put("rolle", new PrimString(claims.get("rolle", String.class)));
            if (claims.get("klasse") != null) {
                principalAttrs.put("klasse", new PrimString(claims.get("klasse", String.class)));
            }
            principalAttrs.put("username", new PrimString(claims.getSubject()));

            // Ablaufjahr für Zugriffsprüfung
            if (claims.get("ablaufJahr") != null) {
                Object ablaufJahrObj = claims.get("ablaufJahr");
                long ablaufJahr;

                if (ablaufJahrObj instanceof Timestamp ts) {
                    ablaufJahr = ts.toLocalDateTime().getYear();
                } else if (ablaufJahrObj instanceof Long) {
                    ablaufJahr = (Long) ablaufJahrObj;
                } else if (ablaufJahrObj instanceof Integer) {
                    ablaufJahr = ((Integer) ablaufJahrObj).longValue();
                } else {
                    logger.warn("Unexpected type for ablaufJahr: {}", ablaufJahrObj.getClass());
                    ablaufJahr = Long.MAX_VALUE; // Fallback: nie ablaufen
                }

                principalAttrs.put("ablaufJahr", new PrimLong(ablaufJahr));
            }

            Entity principal = new Entity(principalUID, principalAttrs, new HashSet<>());

            // Einfache Autorisierungsprüfung: Kann der User grundsätzlich zugreifen?
            EntityUID actionUID = EntityUID.parse("Action::\"access\"")
                .orElseThrow(() -> new IllegalArgumentException("Invalid action UID"));
            EntityUID resourceUID = EntityUID.parse("Resource::\"api\"")
                .orElseThrow(() -> new IllegalArgumentException("Invalid resource UID"));

            // Context mit aktuellem Jahr
            Map<String, Value> context = new HashMap<>();
            context.put("currentYear", new PrimLong(Year.now().getValue()));

            // Erstelle Slice mit Policies und Entities (via BasicSlice Konstruktor)
            Slice slice = new com.cedarpolicy.model.slice.BasicSlice(policySet, Set.of(principal));

            // Autorisierungsanfrage mit korrekter Konstruktor-API (Optional params)
            AuthorizationRequest request = new AuthorizationRequest(
                Optional.of(principalUID),
                actionUID,
                Optional.of(resourceUID),
                Optional.of(context),
                Optional.empty(), // Schema
                false // Schema validation
            );

            AuthorizationResponse response = authEngine.isAuthorized(request, slice);

            logger.debug("Authorization check for user {}: {}", claims.getSubject(), response.isAllowed());
            return response.isAllowed();

        } catch (Exception e) {
            logger.error("Error during Cedar authorization check for user: {}", claims.getSubject(), e);
            // Bei Fehler: konservativ ablehnen
            return false;
        }
    }

    /**
     * Überprüft ob ein User eine bestimmte Aktion auf einer Ressource ausführen darf
     */
    public boolean isAuthorized(Claims claims, String action, String resourceType, String resourceId) {
        try {
            EntityUID principalUID = EntityUID.parse("User::\"" + claims.getSubject() + "\"")
                .orElseThrow(() -> new IllegalArgumentException("Invalid principal UID"));

            Map<String, Value> principalAttrs = new HashMap<>();
            principalAttrs.put("rolle", new PrimString(claims.get("rolle", String.class)));
            if (claims.get("klasse") != null) {
                principalAttrs.put("klasse", new PrimString(claims.get("klasse", String.class)));
            }
            principalAttrs.put("username", new PrimString(claims.getSubject()));

            if (claims.get("ablaufJahr") != null) {
                Object ablaufJahrObj = claims.get("ablaufJahr");
                long ablaufJahr;

                if (ablaufJahrObj instanceof Timestamp ts) {
                    ablaufJahr = ts.toLocalDateTime().getYear();
                } else if (ablaufJahrObj instanceof Long) {
                    ablaufJahr = (Long) ablaufJahrObj;
                } else if (ablaufJahrObj instanceof Integer) {
                    ablaufJahr = ((Integer) ablaufJahrObj).longValue();
                } else {
                    ablaufJahr = Long.MAX_VALUE;
                }

                principalAttrs.put("ablaufJahr", new PrimLong(ablaufJahr));
            }

            Entity principal = new Entity(principalUID, principalAttrs, new HashSet<>());

            EntityUID actionUID = EntityUID.parse("Action::\"" + action + "\"")
                .orElseThrow(() -> new IllegalArgumentException("Invalid action UID"));
            EntityUID resourceUID = EntityUID.parse(resourceType + "::\"" + resourceId + "\"")
                .orElseThrow(() -> new IllegalArgumentException("Invalid resource UID"));

            Map<String, Value> context = new HashMap<>();
            context.put("currentYear", new PrimLong(Year.now().getValue()));

            // Erstelle Slice mit Policies und Entities (via BasicSlice Konstruktor)
            Slice slice = new com.cedarpolicy.model.slice.BasicSlice(policySet, Set.of(principal));

            // Autorisierungsanfrage mit korrekter Konstruktor-API (Optional params)
            AuthorizationRequest request = new AuthorizationRequest(
                Optional.of(principalUID),
                actionUID,
                Optional.of(resourceUID),
                Optional.of(context),
                Optional.empty(), // Schema
                false // Schema validation
            );

            AuthorizationResponse response = authEngine.isAuthorized(request, slice);

            logger.debug("Authorization check for user {} on {}::{} with action {}: {}",
                claims.getSubject(), resourceType, resourceId, action, response.isAllowed());

            return response.isAllowed();

        } catch (Exception e) {
            logger.error("Error during Cedar authorization check", e);
            return false;
        }
    }
}
