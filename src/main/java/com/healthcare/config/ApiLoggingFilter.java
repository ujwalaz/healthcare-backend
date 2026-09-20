package com.healthcare.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Logs every API request on entry (method, path, query, body) and on exit
 * (status, response body, total time taken). Runs just after
 * {@link CorrelationIdFilter} so entry/exit lines carry the correlation id.
 *
 * Request bodies are cached eagerly via {@link CachedBodyHttpServletRequest}
 * so they can be logged before the controller consumes them; multipart
 * requests (file uploads) are excluded from body logging/caching since their
 * payloads are large binary blobs and don't need to be re-read here.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ApiLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("com.healthcare.api");

    private static final int MAX_BODY_LOG_LENGTH = 2000;
    private static final Pattern SENSITIVE_FIELD_PATTERN = Pattern.compile(
            "(\"(?:password|token|secret|authorization)\"\\s*:\\s*)\"[^\"]*\"",
            Pattern.CASE_INSENSITIVE);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        boolean isMultipart = request.getContentType() != null
                && request.getContentType().toLowerCase().startsWith("multipart/");

        HttpServletRequest requestToUse = isMultipart ? request : new CachedBodyHttpServletRequest(request);
        ContentCachingResponseWrapper responseToUse = new ContentCachingResponseWrapper(response);

        String queryString = StringUtils.hasText(request.getQueryString()) ? "?" + request.getQueryString() : "";
        String requestBody = isMultipart
                ? "[multipart upload - body not logged]"
                : mask(new String(((CachedBodyHttpServletRequest) requestToUse).getCachedBody(), StandardCharsets.UTF_8));

        log.info("--> {} {}{} body={}", request.getMethod(), request.getRequestURI(), queryString, truncate(requestBody));

        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(requestToUse, responseToUse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            String responseBody = mask(new String(responseToUse.getContentAsByteArray(), StandardCharsets.UTF_8));
            log.info("<-- {} {}{} status={} durationMs={} body={}",
                    request.getMethod(), request.getRequestURI(), queryString,
                    responseToUse.getStatus(), duration, truncate(responseBody));
            responseToUse.copyBodyToResponse();
        }
    }

    private String mask(String body) {
        if (!StringUtils.hasText(body)) {
            return body;
        }
        return SENSITIVE_FIELD_PATTERN.matcher(body).replaceAll("$1\"***\"");
    }

    private String truncate(String body) {
        if (body == null || body.length() <= MAX_BODY_LOG_LENGTH) {
            return body;
        }
        return body.substring(0, MAX_BODY_LOG_LENGTH) + "...[truncated]";
    }
}
