package itsi.api.database.service;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("securityService")
public class SecurityService {

    public boolean isOwner(Object userId) {
        if (userId == null) {
            return false;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getDetails() == null) {
            return false;
        }
        Claims claims = (Claims) auth.getDetails();
        Object claimUserId = claims.get("userId");
        if (claimUserId == null) {
            return false;
        }
        return Long.valueOf(userId.toString()).equals(Long.valueOf(claimUserId.toString()));
    }
}
