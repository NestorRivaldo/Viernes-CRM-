package com.viernes.shared.exception;

public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSegundos;

    public RateLimitExceededException(String mensaje, long retryAfterSegundos) {
        super(mensaje);
        this.retryAfterSegundos = retryAfterSegundos;
    }

    public long getRetryAfterSegundos() {
        return retryAfterSegundos;
    }
}