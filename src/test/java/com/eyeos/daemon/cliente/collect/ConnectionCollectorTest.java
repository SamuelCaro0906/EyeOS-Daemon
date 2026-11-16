package com.eyeos.daemon.cliente.collect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConnectionCollectorTest {

    @Test
    void convierteBytesAIp() {
        byte[] ipv4 = {(byte) 8, (byte) 8, (byte) 8, (byte) 8};
        assertEquals("8.8.8.8", ConnectionCollector.textoIp(ipv4));
    }

    @Test
    void bytesInvalidosDevuelvenVacio() {
        assertEquals("", ConnectionCollector.textoIp(null));
        assertEquals("", ConnectionCollector.textoIp(new byte[0]));
    }
}
