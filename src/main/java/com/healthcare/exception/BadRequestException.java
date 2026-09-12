package com.healthcare.exception;

import com.healthcare.constants.MessageCode;
import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {

    private final MessageCode messageCode;

    public BadRequestException(MessageCode messageCode) {
        super(messageCode.getDefaultMessage());
        this.messageCode = messageCode;
    }

    public BadRequestException(MessageCode messageCode, String detail) {
        super(detail);
        this.messageCode = messageCode;
    }
}