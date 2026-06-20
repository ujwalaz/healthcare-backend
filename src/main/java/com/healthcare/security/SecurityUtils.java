package com.healthcare.security;

import com.healthcare.constants.MessageCode;
import com.healthcare.exception.AppDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;

public class SecurityUtils {

    private SecurityUtils() {}

    public static JwtClaims getCurrentClaims() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() instanceof JwtClaims claims) {
            return claims;
        }
        throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED, "No authentication found in security context");
    }

    public static void requireRole(String... roles) {
        JwtClaims claims = getCurrentClaims();
        boolean hasRole = Arrays.asList(roles).contains(claims.role());
        if (!hasRole) {
            throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED,
                    "Role " + claims.role() + " is not authorized for this operation");
        }
    }

    public static void requireSameHospital(Long hospitalId) {
        JwtClaims claims = getCurrentClaims();
        if (claims.hospitalId() == null || !claims.hospitalId().equals(hospitalId)) {
            throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED,
                    "You do not have access to resources of this hospital");
        }
    }

    public static void requireSamePatient(Long patientId) {
        JwtClaims claims = getCurrentClaims();
        if (!claims.userId().equals(patientId)) {
            throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED,
                    "You can only access your own resources");
        }
    }
}
