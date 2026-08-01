package com.kiko.app;

interface BodyTransport {
    BodyCapabilities getCapabilities();

    BodyEvent send(BodyCommand command, long nowMs);

    BodyEvent tick(long nowMs);

    BodyEvent disconnect();

    boolean isActive();
}
