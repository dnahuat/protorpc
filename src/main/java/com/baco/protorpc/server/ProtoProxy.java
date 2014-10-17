/*
 *   Copyright (c) 2012, Deiby Dathat Nahuat Uc
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  3. All advertising materials mentioning features or use of this software
 *  must display the following acknowledgement:
 *  This product includes software developed by Deiby Dathat Nahuat.
 *  4. Neither the name of Deiby Dathat Nahuat Uc nor the
 *  names of its contributors may be used to endorse or promote products
 *  derived from this software without specific prior written permission.

 *  THIS SOFTWARE IS PROVIDED BY DEIBY DATHAT NAHUAT UC ''AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL DEIBY DATHAT NAHUAT UC BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package com.baco.protorpc.server;

import com.baco.protorpc.api.SessionValidator;
import com.baco.protorpc.exceptions.ClientRequestNullException;
import com.baco.protorpc.exceptions.MethodDoesntExistsException;
import com.baco.protorpc.exceptions.ProtoException;
import com.baco.protorpc.exceptions.RemoteServerException;
import com.baco.protorpc.exceptions.WrongNumberOfArgumentsException;
import com.baco.protorpc.util.ProtoBufferPool;
import com.baco.protorpc.util.ProtoConfig;
import com.baco.protorpc.util.ProtoEncoders;
import com.baco.protorpc.util.RequestEnvelope;
import com.baco.protorpc.util.ResponseEnvelope;
import io.protostuff.Input;
import io.protostuff.JsonIOUtil;
import io.protostuff.LinkedBuffer;
import io.protostuff.Output;
import io.protostuff.Pipe;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.WireFormat;
import io.protostuff.WireFormat.FieldType;
import io.protostuff.runtime.DefaultIdStrategy;
import io.protostuff.runtime.Delegate;
import io.protostuff.runtime.RuntimeEnv;
import io.protostuff.runtime.RuntimeSchema;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import org.iq80.snappy.SnappyInputStream;
import org.iq80.snappy.SnappyOutputStream;

/**
 * CHANGELOG ---------- 2012-02-09 : First version
 */
/**
 * Service execution proxy
 *
 * @author deiby_nahuat
 */
public class ProtoProxy {

    private final Object srvImplementation;
    private final SessionValidator[] sessionValidators;
    private final Map<String, Method> methodMap;

    protected ProtoProxy(final Object srvImplementation,
            final Class srvDescriptor,
            final SessionValidator[] sessionValidators) throws IllegalArgumentException {
        this.srvImplementation = srvImplementation;
        this.sessionValidators = sessionValidators;
        this.methodMap = new ConcurrentHashMap<String, Method>();
        /*
         * Fill method map
         */
        Method[] methods = srvDescriptor.getMethods();
        for (Method method : methods) {
            methodMap.put(ProtoEncoders.getMethodNameAsSha1(method), method);
        }

        if (srvImplementation == null) {
            throw new IllegalArgumentException(
                    "Service Implementation cannot be null");
        }
        if (!srvDescriptor.isAssignableFrom(srvImplementation.getClass())) {
            throw new IllegalArgumentException(
                    "Service " + srvImplementation + " doesn't implements " + srvDescriptor.
                    getName());
        }
    }

    /**
     * Invokes the requested method in the server implementation
     *
     * @param servletRequest Client request
     * @param is Request Input
     * @param os Response Output
     * @throws Exception
     */
    public void invoke(HttpServletRequest servletRequest, InputStream is,
            OutputStream os) throws Exception {
        LinkedBuffer buffer = null;
        try {
            String serMode = ProtoConfig.getSerializationMode();
            Boolean compEnabled = ProtoConfig.isCompressionEnabled();
            Boolean isJsonNumeric = ProtoConfig.isJSONNumerical();

            buffer = ProtoBufferPool.takeBuffer();
            buffer.clear();

            /**
             * Obtain decompressed input stream
             */
            InputStream sis;
            if (compEnabled) {
                sis = new SnappyInputStream(is);
            } else {
                sis = is;
            }
            /**
             * Prepare schemas
             */
            DefaultIdStrategy dis = (DefaultIdStrategy) RuntimeEnv.ID_STRATEGY;
            dis.registerDelegate(TIMESTAMP_DELEGATE);
            dis.registerDelegate(DATE_DELEGATE);
            dis.registerDelegate(TIME_DELEGATE);
            Schema<RequestEnvelope> schema = RuntimeSchema.getSchema(
                    RequestEnvelope.class);
            Schema<ResponseEnvelope> schemaResp = RuntimeSchema.getSchema(
                    ResponseEnvelope.class);
            /**
             * Obtain request from decompressed input stream
             */
            RequestEnvelope request = schema.newMessage();
            if (serMode.equals(ProtoConfig.BINARY_MODE)) {
                ProtostuffIOUtil.mergeFrom(sis, request, schema);
            } else {
                JsonIOUtil.mergeFrom(sis, request, schema, isJsonNumeric);
            }
            sis.close();
            /**
             * Obtain compressed output stream
             */
            OutputStream sos;
            if (compEnabled) {
                sos = new SnappyOutputStream(os);
            } else {
                sos = os;
            }
            /*
             * Check request validity
             */
            if (request == null || request.getMethodName() == null || request.
                    getMethodName().trim().isEmpty()) {
                ResponseEnvelope response = new ResponseEnvelope(1, null,
                        new ClientRequestNullException(null));
                try {
                    if (serMode.equals(ProtoConfig.BINARY_MODE)) {
                        ProtostuffIOUtil.writeTo(sos, response, schemaResp,
                                buffer);
                    } else {
                        JsonIOUtil.writeTo(sos, response, schemaResp,
                                isJsonNumeric, buffer);
                    }
                } finally {
                    buffer.clear();
                    sos.close();
                }
                return;
            }
            /*
             * Check if method exists
             */
            if (methodMap.get(request.getMethodName()) == null) {
                ResponseEnvelope response = new ResponseEnvelope(1, null,
                        new MethodDoesntExistsException(request.getMethodName(),
                                new IllegalArgumentException(
                                        "Protoservice, requested method doesn't exists"
                                ).fillInStackTrace()));
                try {
                    if (serMode.equals(ProtoConfig.BINARY_MODE)) {
                        ProtostuffIOUtil.writeTo(sos, response, schemaResp,
                                buffer);
                    } else {
                        JsonIOUtil.writeTo(sos, response, schemaResp,
                                isJsonNumeric, buffer);
                    }
                } finally {
                    buffer.clear();
                    sos.close();
                }
                return;
            }
            /**
             * Obtain request attributes
             */
            Object[] values = request.getValues();
            Method method = methodMap.get(request.getMethodName());
            Class<?>[] args = method.getParameterTypes();
            /*
             * Check if number of arguments are equal to number of attrs on the
             * stored method
             */
            if (values.length != args.length) {
                ResponseEnvelope response = new ResponseEnvelope(1, null,
                        new WrongNumberOfArgumentsException(request.
                                getMethodName(),
                                new IllegalArgumentException(
                                        "Protoservice, wrong number of arguments in request"
                                ).fillInStackTrace()));
                try {
                    if (serMode.equals(ProtoConfig.BINARY_MODE)) {
                        ProtostuffIOUtil.writeTo(sos, response, schemaResp,
                                buffer);
                    } else {
                        JsonIOUtil.writeTo(sos, response, schemaResp,
                                isJsonNumeric, buffer);
                    }
                } finally {
                    buffer.clear();
                    sos.close();
                }
                return;
            }
            /**
             * Process session validators
             */
            if (sessionValidators != null && sessionValidators.length > 0) {
                try {
                    for (SessionValidator sv : sessionValidators) {
                        sv.checkSessionValid(request.getSession());
                    }
                } catch (ProtoException ex) {
                    /**
                     * Si ocurre algun error se devuelve el motivo
                     */
                    ResponseEnvelope response = new ResponseEnvelope(1, null, ex);
                    try {
                        if (serMode.equals(ProtoConfig.BINARY_MODE)) {
                            ProtostuffIOUtil.writeTo(sos, response, schemaResp,
                                    buffer);
                        } else {
                            JsonIOUtil.writeTo(sos, response, schemaResp,
                                    isJsonNumeric, buffer);
                        }
                    } finally {
                        buffer.clear();
                        sos.close();
                    }
                    return;
                }
            }
            /*
             * Invoke method
             */
            Object result = null;
            try {
                /**
                 * Call proxy method
                 */
                result = method.invoke(srvImplementation, values);
            } catch (Exception e) {
                Throwable e1 = e;
                /**
                 * Desenvolver ProtoException
                 */
                if (e1 instanceof InvocationTargetException) {
                    e1 = ((InvocationTargetException) e).getTargetException();
                }
                /**
                 * Preparacion de respuesta
                 */
                ResponseEnvelope response = new ResponseEnvelope(1, null,
                        new RemoteServerException(e1));
                try {
                    if (serMode.equals(ProtoConfig.BINARY_MODE)) {
                        ProtostuffIOUtil.writeTo(sos, response, schemaResp,
                                buffer);
                    } else {
                        JsonIOUtil.writeTo(sos, response, schemaResp,
                                isJsonNumeric, buffer);
                    }
                } finally {
                    buffer.clear();
                    sos.close();
                }
                return;
            }
            /*
             * Write response to output
             */
            ResponseEnvelope response = new ResponseEnvelope(0, result, null);
            try {
                if (serMode.equals(ProtoConfig.BINARY_MODE)) {
                    ProtostuffIOUtil.writeTo(sos, response, schemaResp,
                            buffer);
                } else {
                    JsonIOUtil.writeTo(sos, response, schemaResp, isJsonNumeric,
                            buffer);
                }
            } finally {
                buffer.clear();
                sos.close();
            }
        } finally {
            if (buffer != null) {
                ProtoBufferPool.returnBuffer(buffer);
            }
        }
    }

    /**
     * Obtains the stacktrace of a Throwable in a printable format
     *
     * @param
     * @return
     */
    private static String getStackTrace(Throwable throwable) {
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        throwable.printStackTrace(printWriter);
        return writer.toString();
    }

    /**
     * TIMESTAMP DELEGATE
     */
    static final Delegate<Timestamp> TIMESTAMP_DELEGATE = new Delegate<Timestamp>() {

        public FieldType getFieldType() {
            return FieldType.FIXED64;
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
