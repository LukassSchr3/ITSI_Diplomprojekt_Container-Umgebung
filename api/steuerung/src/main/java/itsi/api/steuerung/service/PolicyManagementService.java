package itsi.api.steuerung.service;

import com.cedarpolicy.model.exception.InternalException;
import com.cedarpolicy.model.slice.PolicySet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Service für die dynamische Verwaltung von Cedar-Policies zur Laufzeit
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "cedar.enabled", havingValue = "true")
public class PolicyManagementService {

    private final CedarService cedarService;

    @Value("classpath:policy/CedarPolicy.cedar")
    private Resource policyResource;

    private String currentPolicyContent;

    public PolicyManagementService(CedarService cedarService) {
        this.cedarService = cedarService;
        loadInitialPolicy();
    }

    private void loadInitialPolicy() {
        try {
            if (policyResource != null && policyResource.exists()) {
                this.currentPolicyContent = new String(
                    policyResource.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
                );
                log.info("Initial policy loaded successfully");
            }
        } catch (IOException e) {
            log.error("Failed to load initial policy", e);
        }
    }

    /**
     * Gibt die aktuell aktive Policy zurück
     */
    public String getCurrentPolicy() {
        return currentPolicyContent != null ? currentPolicyContent : "// No policy loaded";
    }

    /**
     * Validiert eine Policy-Syntax
     */
    public boolean validatePolicy(String policyContent) {
        try {
            PolicySet.parsePolicies(policyContent);
            return true;
        } catch (InternalException e) {
            log.warn("Invalid policy syntax: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Aktualisiert die Policy zur Laufzeit
     */
    public boolean updatePolicy(String newPolicyContent) {
        // 1. Validiere die neue Policy
        if (!validatePolicy(newPolicyContent)) {
            log.warn("Cannot update policy - invalid syntax");
            return false;
        }

        try {
            // 2. Speichere die neue Policy (optional: in DB oder Datei)
            savePolicyToFile(newPolicyContent);

            // 3. Aktualisiere in-memory Cache
            this.currentPolicyContent = newPolicyContent;

            // 4. Lade die neue Policy in den CedarService
            cedarService.reloadPolicies(newPolicyContent);

            log.info("Policy updated successfully at runtime");
            return true;

        } catch (Exception e) {
            log.error("Failed to update policy", e);
            return false;
        }
    }

    /**
     * Lädt die Policy neu (z.B. aus Datei oder DB)
     */
    public void reloadPolicy() {
        loadInitialPolicy();
        if (currentPolicyContent != null) {
            cedarService.reloadPolicies(currentPolicyContent);
        }
    }

    /**
     * Speichert Policy in eine Datei (für Persistenz)
     * In Produktion: Nutze eine Datenbank!
     */
    private void savePolicyToFile(String policyContent) throws IOException {
        try {
            // Versuche, die Policy-Datei zu überschreiben
            Path policyPath = Path.of(policyResource.getURI());
            Files.writeString(policyPath, policyContent,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Policy saved to file: {}", policyPath);
        } catch (Exception e) {
            log.warn("Could not save policy to file (resource might be in JAR): {}", e.getMessage());
            // In Production: Save to database instead
        }
    }
}

