package com.healthcare.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.healthcare.constants.MessageCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;

    private ApiResponse(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(MessageCode messageCode, T data) {
        return new ApiResponse<>(true, messageCode.getCode(), messageCode.getDefaultMessage(), data);
    }

    public static <T> ApiResponse<T> ok(MessageCode messageCode, String message, T data) {
        return new ApiResponse<>(true, messageCode.getCode(), message, data);
    }

    public static <T> ApiResponse<T> created(MessageCode messageCode, T data) {
        return new ApiResponse<>(true, messageCode.getCode(), messageCode.getDefaultMessage(), data);
    }

    public static <T> ApiResponse<T> error(MessageCode messageCode) {
        return new ApiResponse<>(false, messageCode.getCode(), messageCode.getDefaultMessage(), null);
    }

    public static <T> ApiResponse<T> error(MessageCode messageCode, String message) {
        return new ApiResponse<>(false, messageCode.getCode(), message, null);
    }
}
