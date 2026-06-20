package com.healthcare.exception;

import com.healthcare.constants.MessageCode;
import lombok.Getter;

@Getter
public class AppDeniedException extends RuntimeException {

    private final MessageCode messageCode;

    public AppDeniedException(MessageCode messageCode) {
        super(messageCode.getDefaultMessage());
        this.messageCode = messageCode;
    }

    public AppDeniedException(MessageCode messageCode, String detail) {
        super(detail);
        this.messageCode = messageCode;
    }
}
