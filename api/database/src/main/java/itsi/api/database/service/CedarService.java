package itsi.api.database.service;

import com.cedarpolicy.AuthorizationEngine;
import com.cedarpolicy.BasicAuthorizationEngine;
import com.cedarpolicy.model.exception.InternalException;
import com.cedarpolicy.model.slice.PolicySet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class CedarService {

    private final AuthorizationEngine authEngine;
    private PolicySet policySet;

    @Value("classpath:policies/CedarPolicy.cedar")
    private Resource policyResource;

    public CedarService() {
        this.authEngine = new BasicAuthorizationEngine();
        loadPolicies();
    }

    private void loadPolicies() {
        try {
            String policies = new String(
                    policyResource.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );
            this.policySet = PolicySet.parsePolicies(policies);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Cedar policies", e);
        } catch (InternalException e) {
            throw new RuntimeException("Failed to parse Cedar policies", e);
        }
    }
}