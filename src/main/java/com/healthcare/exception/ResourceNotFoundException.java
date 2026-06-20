package com.healthcare.exception;

import com.healthcare.constants.MessageCode;
import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final MessageCode messageCode;

    public ResourceNotFoundException(MessageCode messageCode) {
        super(messageCode.getDefaultMessage());
        this.messageCode = messageCode;
    }

    public ResourceNotFoundException(MessageCode messageCode, String detail) {
        super(detail);
        this.messageCode = messageCode;
    }
}
