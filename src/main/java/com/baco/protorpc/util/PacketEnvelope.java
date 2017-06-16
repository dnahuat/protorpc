package com.baco.protorpc.util;

import java.io.Serializable;

/**
 * Contenedor de paquete
 */
public class PacketEnvelope implements Serializable {

    private static final Long serialVersionUID = 8l;
    private final Object packet;

    public PacketEnvelope(final Object packet) {
        this.packet = packet;
    }

    public final Object getPacket() {
        return packet;
    }

}
