package com.healthcare.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
public class Fast2SmsOtpClient {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String templateId;

    public Fast2SmsOtpClient(ObjectMapper mapper, @Value("${FAST2SMS_API_KEY:}") String apiKey,
                             @Value("${FAST2SMS_OTP_TEMPLATE_ID:}") String templateId) {
        this.mapper = mapper;
        this.apiKey = apiKey.trim();
        this.templateId = templateId.trim();
    }

    public void requireConfigured() {
        if (apiKey.isBlank() || templateId.isBlank()) throw unavailable();
    }

    public boolean send(String mobile, int expiryMinutes) {
        return post("send", Map.of("mobile", mobile, "otp_id", templateId,
                "otp_length", 6, "otp_expiry", expiryMinutes));
    }

    public boolean verify(String mobile, String otp) {
        return post("verify", Map.of("mobile", mobile, "otp", otp));
    }

    private boolean post(String action, Map<String, ?> body) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://www.fast2sms.com/dev/otp/" + action))
                    .timeout(Duration.ofSeconds(7))
                    .header("Authorization", apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if ("verify".equals(action) && (status == 400 || status == 404)) return false;
            if (status != 200) throw unavailable();
            if (response.body().isBlank()) return true;
            var payload = mapper.readTree(response.body());
            return payload != null && payload.isObject()
                    && payload.path("return").asBoolean(true) && payload.path("success").asBoolean(true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (IOException | IllegalArgumentException e) {
            throw unavailable();
        }
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SMS verification is temporarily unavailable");
    }
}