package com.healthcare.config;

import com.healthcare.annotation.ApiMessage;
import com.healthcare.dto.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Wraps the raw DTOs returned by generated *ApiImpl controllers (whose method signatures
 * are dictated by the OpenAPI-generated *Api interfaces and therefore return bare DTOs)
 * into the project-wide {@link ApiResponse} envelope, keyed off the {@link ApiMessage}
 * annotation present on the handler method. Handlers without {@link ApiMessage} (e.g.
 * endpoints with no response body) are left untouched.
 */
@RestControllerAdvice
public class ApiResponseEnvelopeAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.getMethodAnnotation(ApiMessage.class) != null;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                   ServerHttpRequest request, ServerHttpResponse response) {
        ApiMessage apiMessage = returnType.getMethodAnnotation(ApiMessage.class);
        return ApiResponse.ok(apiMessage.value(), body);
    }
}
