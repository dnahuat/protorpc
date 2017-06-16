package com.baco.protorpc.util;

import com.baco.protorpc.exceptions.ProtoTransportException;
import io.protostuff.*;
import io.protostuff.runtime.DefaultIdStrategy;
import io.protostuff.runtime.Delegate;
import io.protostuff.runtime.RuntimeEnv;
import io.protostuff.runtime.RuntimeSchema;
import org.iq80.snappy.SnappyInputStream;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

/**
 * Proto buffer packet input stream
 */
public class ProtoPacketInputStream implements Closeable {

    private static final Long serialVersionUID = 1l;
    private final InputStream is;
    private final Schema<PacketEnvelope> schema;
    private boolean closed = false;

    public ProtoPacketInputStream(final InputStream is) {
        this.is = is;
        DefaultIdStrategy dis = (DefaultIdStrategy) RuntimeEnv.ID_STRATEGY;
        dis.registerDelegate(TIMESTAMP_DELEGATE);
        dis.registerDelegate(DATE_DELEGATE);
        dis.registerDelegate(TIME_DELEGATE);
        schema = RuntimeSchema.getSchema(
                PacketEnvelope.class);
    }

    public PacketEnvelope readPacket() throws IOException {
        if (!closed) {
            if(is.available() > 0) {
                PacketEnvelope packet = null;
                packet = schema.newMessage();
                ProtostuffIOUtil.mergeFrom(is, packet, schema);
                return packet;
            } else {
                return null;
            }
        } else {
            throw new IOException("ProtoPacketOutputStream is already closed.");
        }
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        is.close();
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
