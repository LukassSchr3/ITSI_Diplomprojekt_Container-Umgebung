package itsi.api.steuerung.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import itsi.api.steuerung.dto.PolicyUpdateRequest;
import itsi.api.steuerung.dto.PolicyUpdateResponse;
import itsi.api.steuerung.service.PolicyManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/policies")
@Tag(name = "Policy Management", description = "Dynamic Cedar Policy Management (Runtime updates)")
@Slf4j
@ConditionalOnBean(PolicyManagementService.class)
public class PolicyController {

    private final PolicyManagementService policyManagementService;

    public PolicyController(PolicyManagementService policyManagementService) {
        this.policyManagementService = policyManagementService;
    }

    @GetMapping("/current")
    @Operation(summary = "Get Current Policy", description = "Returns the currently active Cedar policy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> getCurrentPolicy() {
        log.info("Fetching current Cedar policy");
        String policy = policyManagementService.getCurrentPolicy();
        return ResponseEntity.ok(policy);
    }

    @PostMapping("/update")
    @Operation(summary = "Update Policy", description = "Updates the Cedar policy at runtime (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PolicyUpdateResponse> updatePolicy(@RequestBody PolicyUpdateRequest request) {
        log.info("Attempting to update Cedar policy");

        try {
            boolean success = policyManagementService.updatePolicy(request.getPolicy());

            if (success) {
                log.info("Policy updated successfully");
                return ResponseEntity.ok(
                    new PolicyUpdateResponse(true, "Policy updated successfully", null)
                );
            } else {
                log.warn("Policy update failed");
                return ResponseEntity.badRequest().body(
                    new PolicyUpdateResponse(false, "Policy update failed", "Invalid policy syntax")
                );
            }
        } catch (Exception e) {
            log.error("Error updating policy", e);
            return ResponseEntity.internalServerError().body(
                new PolicyUpdateResponse(false, "Error updating policy", e.getMessage())
            );
        }
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate Policy", description = "Validates Cedar policy syntax without applying it")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PolicyUpdateResponse> validatePolicy(@RequestBody PolicyUpdateRequest request) {
        log.info("Validating Cedar policy");

        try {
            boolean isValid = policyManagementService.validatePolicy(request.getPolicy());

            if (isValid) {
                return ResponseEntity.ok(
                    new PolicyUpdateResponse(true, "Policy syntax is valid", null)
                );
            } else {
                return ResponseEntity.badRequest().body(
                    new PolicyUpdateResponse(false, "Invalid policy syntax", null)
                );
            }
        } catch (Exception e) {
            log.error("Error validating policy", e);
            return ResponseEntity.badRequest().body(
                new PolicyUpdateResponse(false, "Policy validation failed", e.getMessage())
            );
        }
    }

    @PostMapping("/reload")
    @Operation(summary = "Reload Policy", description = "Reloads policy from file/database")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PolicyUpdateResponse> reloadPolicy() {
        log.info("Reloading Cedar policy");

        try {
            policyManagementService.reloadPolicy();
            return ResponseEntity.ok(
                new PolicyUpdateResponse(true, "Policy reloaded successfully", null)
            );
        } catch (Exception e) {
            log.error("Error reloading policy", e);
            return ResponseEntity.internalServerError().body(
                new PolicyUpdateResponse(false, "Error reloading policy", e.getMessage())
            );
        }
    }
}

