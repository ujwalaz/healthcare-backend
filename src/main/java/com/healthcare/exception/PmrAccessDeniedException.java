package com.healthcare.exception;

import com.healthcare.constants.MessageCode;
import lombok.Getter;

@Getter
public class PmrAccessDeniedException extends RuntimeException {

    private final MessageCode messageCode;

    public PmrAccessDeniedException() {
        super(MessageCode.PMR_ACCESS_DENIED.getDefaultMessage());
        this.messageCode = MessageCode.PMR_ACCESS_DENIED;
    }

    public PmrAccessDeniedException(String detail) {
        super(detail);
        this.messageCode = MessageCode.PMR_ACCESS_DENIED;
    }
}
