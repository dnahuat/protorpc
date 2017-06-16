package com.baco.protorpc.util;

import io.protostuff.*;
import io.protostuff.runtime.DefaultIdStrategy;
import io.protostuff.runtime.Delegate;
import io.protostuff.runtime.RuntimeEnv;
import io.protostuff.runtime.RuntimeSchema;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

/**
 * Proto buffer packet output stream
 */
public class ProtoPacketOutputStream implements Closeable {

    private final OutputStream os;
    private final Schema<PacketEnvelope> schema;
    private LinkedBuffer buffer = null;
    private boolean closed = false;

    public ProtoPacketOutputStream(OutputStream os) {
        this.os = os;
        closed = false;
        DefaultIdStrategy dis = (DefaultIdStrategy) RuntimeEnv.ID_STRATEGY;
        dis.registerDelegate(TIMESTAMP_DELEGATE);
        dis.registerDelegate(DATE_DELEGATE);
        dis.registerDelegate(TIME_DELEGATE);
        schema = RuntimeSchema.getSchema(
                PacketEnvelope.class);
    }

    public void writePacket(PacketEnvelope packet) throws IOException {
        try {
            if (!closed) {
                if (buffer == null) {
                    buffer = ProtoBufferPool.takeBuffer();
                }
                buffer.clear();
                try {
                    ProtostuffIOUtil.writeDelimitedTo(os, packet, schema, buffer);
                } finally {
                    buffer.clear();
                }
            } else {
                throw new IOException("ProtoPacketOutputStream is already closed.");
            }
        } catch (InterruptedException ex) {
            throw new IOException("Couldn't take proto buffer from pool.");
        }
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        try {
            this.os.flush();
            this.os.close();
        } finally {
            buffer.clear();
            try {
                ProtoBufferPool.returnBuffer(buffer);
            } catch (InterruptedException ex) {
                throw new IOException("Couldn't return proto buffer to pool.");
            }
        }
    }

    static final Delegate<Timestamp> TIMESTAMP_DELEGATE = new Delegate<Timestamp>() {

        public WireFormat.FieldType getFieldType() {
            return WireFormat.FieldType.FIXED64;
        }

        public Class<?> typeClass() {
            return Timestamp.class;
        }

        public Timestamp readFrom(Input input) throws IOException {
            return new Timestamp(input.readFixed64());
        }

        public void writeTo(Output output, int number, Timestamp value,
                            boolean repeated)
                throws IOException {
            output.writeFixed64(number, value.getTime(), repeated);
        }

        public void transfer(Pipe pipe, Input input, Output output, int number,
                             boolean repeated)
                throws IOException {
            output.writeFixed64(number, input.readFixed64(), repeated);
        }

    };

    static final Delegate<Time> TIME_DELEGATE = new Delegate<Time>() {

        public WireFormat.FieldType getFieldType() {
            return WireFormat.FieldType.FIXED64;
        }

        public Class<?> typeClass() {
            return Time.class;
        }

        public Time readFrom(Input input) throws IOException {
            return new Time(input.readFixed64());
        }

        public void writeTo(Output output, int number, Time value,
                            boolean repeated)
                throws IOException {
            output.writeFixed64(number, value.getTime(), repeated);
        }

        public void transfer(Pipe pipe, Input input, Output output, int number,
                             boolean repeated)
                throws IOException {
            output.writeFixed64(number, input.readFixed64(), repeated);
        }

    };

    static final Delegate<Date> DATE_DELEGATE = new Delegate<Date>() {

        public WireFormat.FieldType getFieldType() {
            return WireFormat.FieldType.FIXED64;
        }

        public Class<?> typeClass() {
            return Date.class;
        }

        public Date readFrom(Input input) throws IOException {
            return new Date(input.readFixed64());
        }

        public void writeTo(Output output, int number, Date value,
                            boolean repeated)
                throws IOException {
            output.writeFixed64(number, value.getTime(), repeated);
        }

        public void transfer(Pipe pipe, Input input, Output output, int number,
                             boolean repeated)
                throws IOException {
            output.writeFixed64(number, input.readFixed64(), repeated);
        }

    };


}
