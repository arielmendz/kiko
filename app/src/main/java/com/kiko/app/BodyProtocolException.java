package com.kiko.app;

final class BodyProtocolException extends Exception {
    BodyProtocolException(String reason) {
        super(reason);
    }

    BodyProtocolException(String reason, Throwable cause) {
        super(reason, cause);
    }
}
