package itsi.api.steuerung.service;

import com.cedarpolicy.AuthorizationEngine;
import com.cedarpolicy.BasicAuthorizationEngine;
import com.cedarpolicy.model.AuthorizationRequest;
import com.cedarpolicy.model.AuthorizationResponse;
import com.cedarpolicy.model.exception.AuthException;
import com.cedarpolicy.model.exception.InternalException;
import com.cedarpolicy.model.slice.Entity;
import com.cedarpolicy.model.slice.PolicySet;
import com.cedarpolicy.model.slice.Slice;
import com.cedarpolicy.value.EntityUID;
import com.cedarpolicy.value.PrimLong;
import com.cedarpolicy.value.PrimString;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("classpath:policy/CedarPolicy.cedar")
    private Resource policyResource;

    public CedarService() {
    }

    @PostConstruct
    private void initialize() {
        try {
            logger.info("Initializing Cedar authorization engine...");
            this.authEngine = new BasicAuthorizationEngine();
            loadPolicies();
            logger.info("Cedar authorization engine initialized successfully");
        } catch (UnsatisfiedLinkError e) {
            logger.error("Failed to load Cedar native library. Make sure the cedar_java_ffi library is in your java.library.path");
            logger.error("Cedar authorization will be BYPASSED (fail-open mode). This is NOT recommended for production!");
            this.authEngine = null;
        } catch (Throwable e) {
            logger.error("Failed to initialize Cedar engine: {}", e.getMessage());
            logger.error("Cedar authorization will be BYPASSED (fail-open mode). This is NOT recommended for production!");
            this.authEngine = null;
        }
    }

    private void loadPolicies() {
        try {
            if (policyResource == null || !policyResource.exists()) {
                logger.warn("Cedar policy file not found");
                return;
            }
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
     * Lädt Policies zur Laufzeit neu (für dynamische Policy-Updates)
     */
    public void reloadPolicies(String policyContent) {
        try {
            if (authEngine == null) {
                logger.warn("Cannot reload policies - Cedar engine not initialized");
                return;
            }
            this.policySet = PolicySet.parsePolicies(policyContent);
            logger.info("Cedar policies reloaded successfully at runtime");
        } catch (InternalException e) {
            logger.error("Failed to reload Cedar policies", e);
            throw new RuntimeException("Failed to reload Cedar policies", e);
        }
    }

    /**
     * Helper to bypass checks if engine is not ready
     */
    private boolean isBypassMode(String contextInfo) {
        if (authEngine == null) {
            logger.warn("Cedar engine not initialized. Bypassing authorization check: {}", contextInfo);
            return true; // Fail-open for development/demo, change to false for strict security
        }
        return false;
    }

    /**
     * Überprüft ob ein User basierend auf den JWT Claims autorisiert ist (Standard Read Access)
     */
    public boolean isUserAuthorized(Claims claims) {
        if (isBypassMode("User Authorization " + claims.getSubject())) return true;

        try {
            EntityUID principalUID = EntityUID.parse("User::\"" + claims.getSubject() + "\"").orElseThrow();

            Map<String, com.cedarpolicy.value.Value> principalAttrs = extractPrincipalAttributes(claims);
            Entity principal = new Entity(principalUID, principalAttrs, new HashSet<>());

            EntityUID actionUID = EntityUID.parse("Action::\"read\"").orElseThrow();
            EntityUID resourceUID = EntityUID.parse("Resource::\"system\"").orElseThrow();

            return checkAuthorization(principal, actionUID, resourceUID, Collections.emptySet());

        } catch (Exception e) {
            logger.error("Error during Cedar authorization check for user: {}", claims.getSubject(), e);
            return false;
        }
    }

    /**
     * Prüft Zugriff für Container-Operationen (Ownership Check)
     */
    public boolean checkContainerAccess(Claims claims, String action, Integer targetUserId) {
        if (isBypassMode("Container Access " + action)) return true;

        try {
            EntityUID principalUID = EntityUID.parse("User::\"" + claims.getSubject() + "\"").orElseThrow();
            Map<String, com.cedarpolicy.value.Value> principalAttrs = extractPrincipalAttributes(claims);
            Entity principal = new Entity(principalUID, principalAttrs, new HashSet<>());

            EntityUID actionUID = EntityUID.parse("Action::\"" + action + "\"").orElseThrow();

            // Resource: "request" vom Typ "Container", welche "ownerId" hat
            EntityUID resourceUID = EntityUID.parse("Container::\"request\"").orElseThrow();
            Map<String, com.cedarpolicy.value.Value> resourceAttrs = new HashMap<>();
            resourceAttrs.put("ownerId", new PrimLong(targetUserId.longValue()));
            Entity resource = new Entity(resourceUID, resourceAttrs, new HashSet<>());

            return checkAuthorization(principal, actionUID, resourceUID, Set.of(resource));

        } catch (Exception e) {
            logger.error("Error checking container access", e);
            return false;
        }
    }

    private Map<String, com.cedarpolicy.value.Value> extractPrincipalAttributes(Claims claims) {
        Map<String, com.cedarpolicy.value.Value> attrs = new HashMap<>();
        attrs.put("rolle", new PrimString(claims.get("rolle", String.class)));
        attrs.put("username", new PrimString(claims.getSubject()));

        if (claims.get("klasse") != null) {
            attrs.put("klasse", new PrimString(claims.get("klasse", String.class)));
        }

        if (claims.get("userId") != null) {
            attrs.put("userId", new PrimLong(claims.get("userId", Integer.class).longValue()));
        }

        if (claims.get("ablaufJahr") != null) {
            attrs.put("ablaufJahr", new PrimLong(convertAblaufJahr(claims.get("ablaufJahr"))));
        }

        return attrs;
    }

    private long convertAblaufJahr(Object ablaufJahrObj) {
        if (ablaufJahrObj instanceof Timestamp ts) {
            return ts.toLocalDateTime().getYear();
        } else if (ablaufJahrObj instanceof Number) {
            return ((Number) ablaufJahrObj).longValue();
        }
        return Long.MAX_VALUE;
    }

    private boolean checkAuthorization(Entity principal, EntityUID action, EntityUID resource, Set<Entity> extraEntities) throws AuthException {
        Map<String, com.cedarpolicy.value.Value> context = new HashMap<>();
        context.put("currentYear", new PrimLong(Year.now().getValue()));

        Set<Entity> entities = new HashSet<>();
        entities.add(principal);
        entities.addAll(extraEntities);

        Slice slice = new com.cedarpolicy.model.slice.BasicSlice(policySet, entities);

        AuthorizationRequest request = new AuthorizationRequest(
            Optional.of(principal.getEUID()),
            action,
            Optional.of(resource),
            Optional.of(context),
            Optional.empty(),
            false
        );

        AuthorizationResponse response = authEngine.isAuthorized(request, slice);
        logger.debug("Cedar Decision for {} on {}: {}", principal.getEUID(), resource, response.isAllowed());
        return response.isAllowed();
    }
}
