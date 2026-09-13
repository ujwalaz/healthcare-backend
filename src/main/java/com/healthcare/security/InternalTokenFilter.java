package com.healthcare.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.constants.MessageCode;
import com.healthcare.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Guards/augments endpoints with a static shared-secret {@code X-Internal-Token} header instead of
 * (or in addition to) a per-user JWT.
 *
 * <p>Two modes are supported, matched by exact path + method:
 * <ul>
 *   <li><b>Internal-only</b> ({@link #INTERNAL_ONLY_PATHS}): the header is <b>required</b>.
 *       Requests without a valid header are rejected here, before reaching Spring Security's
 *       normal authentication chain (e.g. the cross-hospital today-slots-summary dashboard feed).</li>
 *   <li><b>Internal-or-JWT</b> ({@link #INTERNAL_OR_JWT_PATHS}): the header is <b>optional</b>.
 *       If present and valid, the request is authenticated here (bypassing JWT) with a synthetic
 *       {@code ROLE_INTERNAL} principal. If absent/invalid, the request simply falls through to the
 *       normal JWT-based authentication/authorization chain — i.e. a valid JWT still works.</li>
 * </ul>
 */
@Component
@Slf4j
public class InternalTokenFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Internal-Token";

    private static final Set<String> INTERNAL_ONLY_PATHS = Set.of(
            "/api/appointments/slots/today-summary"
    );

    private static final Set<String> INTERNAL_OR_JWT_PATHS = Set.of(
            "/api/doctors"
    );

    private final String expectedToken;
    private final ObjectMapper objectMapper;

    public InternalTokenFilter(@Value("${app.internal-token:InternalWebApp}") String expectedToken,
                                ObjectMapper objectMapper) {
        this.expectedToken = expectedToken;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        boolean hasValidToken = expectedToken.equals(request.getHeader(HEADER_NAME));

        if (INTERNAL_ONLY_PATHS.contains(path)) {
            if (!hasValidToken) {
                log.warn("Rejected request to {} — missing or invalid {} header", path, HEADER_NAME);
                writeUnauthorized(response);
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        if (INTERNAL_OR_JWT_PATHS.contains(path) && hasValidToken) {
            authenticateAsInternal();
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateAsInternal() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "internal", null, List.of(new SimpleGrantedAuthority("ROLE_INTERNAL")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.error(MessageCode.AUTH_INTERNAL_TOKEN_INVALID)));
    }
}