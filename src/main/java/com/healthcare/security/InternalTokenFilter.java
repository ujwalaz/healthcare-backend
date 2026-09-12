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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Guards internal-only endpoints (e.g. the cross-hospital today-slots-summary dashboard feed)
 * with a static shared-secret header instead of a per-user JWT. Requests to a guarded path
 * without a matching {@code X-Internal-Token} header are rejected before reaching Spring Security's
 * normal authentication/authorization chain.
 */
@Component
@Slf4j
public class InternalTokenFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Internal-Token";

    private static final String GUARDED_PATH = "/api/appointments/slots/today-summary";

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
        if (!isGuarded(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader(HEADER_NAME);
        if (token == null || !token.equals(expectedToken)) {
            log.warn("Rejected request to {} — missing or invalid {} header", request.getRequestURI(), HEADER_NAME);
            writeUnauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isGuarded(HttpServletRequest request) {
        return HttpMethod.GET.matches(request.getMethod()) && GUARDED_PATH.equals(request.getRequestURI());
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.error(MessageCode.AUTH_INTERNAL_TOKEN_INVALID)));
    }
}