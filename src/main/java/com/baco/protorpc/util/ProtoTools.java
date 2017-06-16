package com.baco.protorpc.util;

import com.baco.protorpc.exceptions.ProtoTransportException;
import io.protostuff.*;
import io.protostuff.runtime.DefaultIdStrategy;
import io.protostuff.runtime.Delegate;
import io.protostuff.runtime.RuntimeEnv;
import io.protostuff.runtime.RuntimeSchema;
import org.iq80.snappy.SnappyInputStream;
import org.iq80.snappy.SnappyOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tools to serialize/deserialize
 */
public class ProtoTools {

    private static final Long serialVersionUID = 1l;
    private final Schema<PacketEnvelope> schema;
    private OutputStream outputStream;

    public ProtoTools() {
        /**
         * Prepare schemas for wrappers
         */
        DefaultIdStrategy dis = (DefaultIdStrategy) RuntimeEnv.ID_STRATEGY;
        dis.registerDelegate(TIMESTAMP_DELEGATE);
        dis.registerDelegate(DATE_DELEGATE);
        dis.registerDelegate(TIME_DELEGATE);
        schema = RuntimeSchema.getSchema(
                PacketEnvelope.class);
    }

    public final byte[] serializeData(final PacketEnvelope packet) throws Throwable {
        LinkedBuffer buffer = null;
        try {
            buffer = ProtoBufferPool.takeBuffer();
            /*
             * Initialize current buffer
             */
            buffer.clear();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            SnappyOutputStream sos = new SnappyOutputStream(bos);
            try {
                ProtostuffIOUtil.writeDelimitedTo(sos, packet, schema, buffer);
                sos.flush();
                sos.close();
                return bos.toByteArray();
            } catch (IOException ex) {
                ProtoTransportException pex = new ProtoTransportException(
                        "Error while writing protocol initiation to server.", ex);
                throw pex;
            } finally {
                buffer.clear();
            }
        } catch (InterruptedException ex) {
            ProtoTransportException pex = new ProtoTransportException(
                    "Couldn't obtain a free buffer to write to remote. Max capacity reached.",
                    ex);
            throw pex;
        } finally {
            if (buffer != null) {
                ProtoBufferPool.returnBuffer(buffer);
            }
        }
    }

    public final PacketEnvelope deserializeData(final byte[] data) throws Throwable {
        if(data == null || data.length == 0) {
            throw  new IllegalArgumentException("Binary data cannot be null or empty.");
        }
        LinkedBuffer buffer = null;
        PacketEnvelope packet = null;
        try {
            buffer = ProtoBufferPool.takeBuffer();
            /*
             * Initialize current buffer
             */
            buffer.clear();

            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            SnappyInputStream sis = new SnappyInputStream(bis);

            packet = schema.newMessage();
            ProtostuffIOUtil.mergeFrom(data, packet, schema);
            sis.close();
            return packet;
        } catch (InterruptedException ex) {
            ProtoTransportException pex = new ProtoTransportException(
                    "Couldn't obtain a free buffer to write to remote. Max capacity reached.",
                    ex);
            throw pex;
        } finally {
            if (buffer != null) {
                ProtoBufferPool.returnBuffer(buffer);
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
