package com.healthcare.annotation;

import com.healthcare.constants.MessageCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the {@link MessageCode} used to envelope a successful *ApiImpl response body
 * into {@link com.healthcare.dto.ApiResponse} via {@code ApiResponseEnvelopeAdvice}.
 * Only annotated handler methods are wrapped; methods without this annotation are
 * returned as-is (used for endpoints that intentionally have no response body).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ApiMessage {
    MessageCode value();
}
