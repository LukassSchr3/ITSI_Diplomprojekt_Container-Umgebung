package itsi.api.steuerung.service;

import com.cedarpolicy.AuthorizationEngine;
import com.cedarpolicy.BasicAuthorizationEngine;
import com.cedarpolicy.model.exception.InternalException;
import com.cedarpolicy.model.slice.PolicySet;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@ConditionalOnProperty(name = "cedar.enabled", havingValue = "true", matchIfMissing = false)
public class CedarService {

    private static final Logger logger = LoggerFactory.getLogger(CedarService.class);

    private AuthorizationEngine authEngine;
    private PolicySet policySet;

    @Value("classpath:policy/CedarPolicy.cedar")
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
}