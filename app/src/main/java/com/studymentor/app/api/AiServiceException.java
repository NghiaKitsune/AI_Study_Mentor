package com.studymentor.app.api;

import java.io.IOException;

public class AiServiceException extends IOException {
    public static final String MISSING_KEY = "MISSING_KEY";
    public static final String HTTP_ERROR = "HTTP_ERROR";
    public static final String RATE_LIMIT = "RATE_LIMIT";
    public static final String EMPTY_RESPONSE = "EMPTY_RESPONSE";
    public static final String PARSE_ERROR = "PARSE_ERROR";
    public static final String SAFETY_BLOCK = "SAFETY_BLOCK";

    public final String code;
    public final int httpStatus;

    public AiServiceException(String code, String message) {
        this(code, message, 0, null);
    }

    public AiServiceException(String code, String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}

