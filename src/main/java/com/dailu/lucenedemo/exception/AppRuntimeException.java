package com.dailu.lucenedemo.exception;

public class AppRuntimeException extends RuntimeException {

    public AppRuntimeException() {
        super();
    }

    public AppRuntimeException(String msg) {
        super(msg);
    }

    public AppRuntimeException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

    public AppRuntimeException(Throwable throwable) {
        super(throwable);
    }
}
