package com.kiko.app;

interface BodyWireLink {
    byte[] write(byte[] commandPayload, long nowMs);

    byte[] tick(long nowMs);

    byte[] disconnect();
}
