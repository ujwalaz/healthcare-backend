package com.healthcare.security;

import com.healthcare.repository.AdminUserRepository;
import com.healthcare.repository.DoctorRepository;
import com.healthcare.repository.PatientRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AdminUserRepository adminUserRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            try {
                String jti = jwtUtil.extractJti(token);
                if (tokenBlacklistService.isRevoked(jti)) {
                    log.warn("Rejected revoked JWT");
                    filterChain.doFilter(request, response);
                    return;
                }

                Long userId = jwtUtil.extractUserId(token);
                String role = jwtUtil.extractRole(token);
                Long hospitalId = jwtUtil.extractHospitalId(token);
                Integer tokenPasswordVersion = jwtUtil.extractPasswordVersion(token);

                if (tokenPasswordVersion != null && !isCurrentPasswordVersion(userId, role, tokenPasswordVersion)) {
                    log.warn("Rejected JWT issued before password reset for userId={}, role={}", userId, role);
                    filterChain.doFilter(request, response);
                    return;
                }

                JwtClaims claims = new JwtClaims(userId, role, hospitalId);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                claims,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                log.error("Could not set user authentication in security context: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isCurrentPasswordVersion(Long userId, String role, Integer tokenVersion) {
        return switch (role) {
            case "PATIENT" -> patientRepository.findById(userId)
                    .map(account -> tokenVersion.equals(account.getPasswordVersion())).orElse(false);
            case "DOCTOR" -> doctorRepository.findById(userId)
                    .map(account -> tokenVersion.equals(account.getPasswordVersion())).orElse(false);
            case "ADMIN" -> adminUserRepository.findById(userId)
                    .map(account -> tokenVersion.equals(account.getPasswordVersion())).orElse(false);
            default -> false;
        };
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}