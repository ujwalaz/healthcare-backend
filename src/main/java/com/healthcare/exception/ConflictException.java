package com.healthcare.exception;

import com.healthcare.constants.MessageCode;
import lombok.Getter;

@Getter
public class ConflictException extends RuntimeException {

    private final MessageCode messageCode;

    public ConflictException(MessageCode messageCode) {
        super(messageCode.getDefaultMessage());
        this.messageCode = messageCode;
    }

    public ConflictException(MessageCode messageCode, String detail) {
        super(detail);
        this.messageCode = messageCode;
    }
}
