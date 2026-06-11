package com.firstclub.firstclub.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class MembershipException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public MembershipException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
