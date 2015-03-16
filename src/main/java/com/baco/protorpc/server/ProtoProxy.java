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
import com.baco.protorpc.util.ProtoHandshake;
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
import java.util.Calendar;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
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
            Schema<ProtoHandshake> schemaProto = RuntimeSchema.getSchema(
                    ProtoHandshake.class);

            buffer = ProtoBufferPool.takeBuffer();
            buffer.clear();

            /**
             * LOG BLOCK
             */
            String requestUuid = UUID.randomUUID().toString();
            Integer stepCounter = 0;
            long elapsedTime = 0l;
            long startedTime = System.currentTimeMillis();
            String messageLog = new StringBuilder(stepCounter.toString())
                    .append(". Request from ")
                    .append(servletRequest.getRemoteAddr())
                    .append(" started.")
                    .append(" Request packed size=")
                    .append(servletRequest.getContentLength())
                    .append(". Assigned channel UUID=")
                    .append(requestUuid)
                    .append(". Elapsed time=")
                    .append(elapsedTime)
                    .toString();
            Logger.getLogger("ProtoProxyServer").log(Level.INFO, messageLog);
            stepCounter++;
            /**
             * END LOG BLOCK
             */
            /**
             * Read protocol handshake
             */
            ProtoHandshake ph = schemaProto.newMessage();
            ProtostuffIOUtil.mergeDelimitedFrom(is, ph, schemaProto, buffer);
            buffer.clear();

            /**
             * LOG BLOCK
             */
            elapsedTime = System.currentTimeMillis() - startedTime;
            messageLog = new StringBuilder(stepCounter.toString())
                    .append(".(")
                    .append(requestUuid)
                    .append(") - Handshake begin packet from ")
                    .append(servletRequest.getRemoteAddr())
                    .append(" received.")
                    .append(" Handshake begin message[")
                    .append("compressed=").append(ph.getCompressed()).append(",")
                    .append("data_encoding=").append(ph.getRequestedProtocol()==0?"protostuff,":"json,")
                    .append("json_numerical=").append(ph.getJsonNumerical()).append("]")
                    .append(". Elapsed time=")
                    .append(elapsedTime)
                    .toString();
            Logger.getLogger("ProtoProxyServer").log(Level.INFO, messageLog);
            stepCounter++;
            /**
             * END LOG BLOCK
             */

            /**
             * Obtain decompressed input stream
             */
            InputStream sis;
            if (ph.getCompressed()) {
                sis = new SnappyInputStream(is);
            } else {
                sis = is;
            }

            /**
             * Obtain request from decompressed input stream
             */
            RequestEnvelope request = schema.newMessage();
            if (ph.getRequestedProtocol() == 0) {
                ProtostuffIOUtil.mergeFrom(sis, request, schema);
            } else {
                JsonIOUtil.
                        mergeFrom(sis, request, schema, ph.getJsonNumerical());
            }
            sis.close();

            /**
             * LOG BLOCK
             */
            elapsedTime = System.currentTimeMillis() - startedTime;
            messageLog = new StringBuilder(stepCounter.toString())
                    .append(".(")
                    .append(requestUuid)
                    .append(") - Request envelope unpacked successfully")
                    .append(". Elapsed time=")
                    .append(elapsedTime)
                    .toString();
            Logger.getLogger("ProtoProxyServer").log(Level.INFO, messageLog);
            stepCounter++;
            /**
             * END LOG BLOCK
             */

            /**
             * Obtain compressed output stream
             */
            OutputStream sos;
            if (ph.getCompressed()) {
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
                    if (ph.getRequestedProtocol() == 0) {
                        ProtostuffIOUtil.writeTo(sos, response, schemaResp,
                                buffer);
                    } else {
                        JsonIOUtil.writeTo(sos, response, schemaResp,
                                ph.getJsonNumerical(), buffer);
                    }
                } finally {
                    buffer.clear();
                    sos.close();
                }

                /**
                 * LOG BLOCK
                 */
                elapsedTime = System.currentTimeMillis() - startedTime;
                messageLog = new StringBuilder(stepCounter.toString())
                        .append(".(")
                        .append(requestUuid)
                        .append(") - Request envelope was invalid. Request was ABORTED.")
                        .append(" Applied checks=[")
                        .append("request_not_null=").append(request == null?"failed,":"passed,")
                        .append("methodname_not_empty=").append(request == null?"failed"
                                                        :(request.getMethodName()==null?"failed"
                                                        :(request.getMethodName().trim().isEmpty()?"failed":"passed")))
                        .append("]")
                        .append(". Elapsed time=")
                        .append(elapsedTime)
                        .toString();
                Logger.getLogger("ProtoProxyServer").log(Level.SEVERE, messageLog);
                stepCounter++;
                /**
                 * END LOG BLOCK
                 */

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
                    if (ph.getRequestedProtocol() == 0) {
                        ProtostuffIOUtil.writeTo(sos, response, schemaResp,
                                buffer);
                    } else {
                        JsonIOUtil.writeTo(sos, response, schemaResp,
                                ph.getJsonNumerical(), buffer);
                    }
                } finally {
                    buffer.clear();
                    sos.close();
                }

                /**
                 * LOG BLOCK
                 */
                elapsedTime = System.currentTimeMillis() - startedTime;
                messageLog = new StringBuilder(stepCounter.toString())
                        .append(".(")
                        .append(requestUuid)
                        .append(") - Requested service (SHA1=")
                        .append(request.getMethodName()).append(")")
                        .append(" is not registered at this server. ABORTING execution")
                        .append(". Elapsed time=")
                        .append(elapsedTime)
                        .toString();
                Logger.getLogger("ProtoProxyServer").log(Level.SEVERE, messageLog);
                stepCounter++;
                /**
                 * END LOG BLOCK
                 */
                return;
            }

            /**
             * Obtain request attributes
             */
            Object[] values = request.getValues();
            Method method = methodMap.get(request.getMethodName());
            Class<?>[] args = method.getParameterTypes();

            /**
             * LOG BLOCK
             */
            elapsedTime = System.currentTimeMillis() - startedTime;
            messageLog = new StringBuilder(stepCounter.toString())
                    .append(".(")
                    .append(requestUuid)
                    .append(") - Requested service (SHA1=")
                    .append(request.getMethodName()).append(")")
                    .append(" resolved as '")
                    .append(method.getName()).append("' with [")
                    .append("local_parameter_count=").append(args.length)
                    .append(",request_parameter_count=").append(values.length)
                    .append("]. Elapsed time=")
                    .append(elapsedTime)
                    .toString();
            Logger.getLogger("ProtoProxyServer").log(Level.INFO, messageLog);
            stepCounter++;
            /**
             * END LOG BLOCK
             */


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
                    if (ph.getRequestedProtocol() == 0) {
                        ProtostuffIOUtil.writeTo(sos, response, schemaResp,
                                buffer);
                    } else {
                        JsonIOUtil.writeTo(sos, response, schemaResp,
                                ph.getJsonNumerical(), buffer);
                    }
                } finally {
                    buffer.clear();
                    sos.close();
                }

                /**
                 * LOG BLOCK
                 */
                elapsedTime = System.currentTimeMillis() - startedTime;
                messageLog = new StringBuilder(stepCounter.toString())
                        .append(".(")
                        .append(requestUuid)
                        .append(") - Requested service has a different argument count than local implementation.")
                        .append(" Maybe the remote server uses a different API version")
                        .append(". Elapsed time=")
                        .append(elapsedTime)
                        .toString();
                Logger.getLogger("ProtoProxyServer").log(Level.SEVERE, messageLog);
                stepCounter++;
                /**
                 * END LOG BLOCK
                 */

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
                    /**
                     * LOG BLOCK
                     */
                    elapsedTime = System.currentTimeMillis() - startedTime;
                    messageLog = new StringBuilder(stepCounter.toString())
                            .append(".(")
                            .append(requestUuid)
                            .append(") - Request passed all session validations.")
                            .append(". Elapsed time=")
                            .append(elapsedTime)
                            .toString();
                    Logger.getLogger("ProtoProxyServer").log(Level.INFO, messageLog);
                    stepCounter++;
                    /**
                     * END LOG BLOCK
                     */
                } catch (ProtoException ex) {
                    /**
                     * Si ocurre algun error se devuelve el motivo
                     */
                    ResponseEnvelope response = new ResponseEnvelope(1, null, ex);
                    try {
                        if (ph.getRequestedProtocol() == 0) {
                            ProtostuffIOUtil.writeTo(sos, response, schemaResp,
                                    buffer);
                        } else {
                            JsonIOUtil.writeTo(sos, response, schemaResp,
                                    ph.getJsonNumerical(), buffer);
                        }
                    } finally {
                        buffer.clear();
                        sos.close();
                    }
                    /**
                     * LOG BLOCK
                     */
                    elapsedTime = System.currentTimeMillis() - startedTime;
                    messageLog = new StringBuilder(stepCounter.toString())
                            .append(".(")
                            .append(requestUuid)
                            .append(") - Request invalid. Session validations not passed.")
                            .append(". Elapsed time=")
                            .append(elapsedTime)
                            .toString();
                    Logger.getLogger("ProtoProxyServer").log(Level.SEVERE, messageLog, ex);
                    stepCounter++;
                    /**
                     * END LOG BLOCK
                     */
                    return;
                }
            }
            /*
             * Invoke method
             */
            Object result = null;
            try {
                /**
                 * LOG BLOCK
                 */
                elapsedTime = System.currentTimeMillis() - startedTime;
                messageLog = new StringBuilder(stepCounter.toString())
                        .append(".(")
                        .append(requestUuid)
                        .append(") - Service execution begin.")
                        .append(". Elapsed time=")
                        .append(elapsedTime)
                        .toString();
                Logger.getLogger("ProtoProxyServer").log(Level.INFO, messageLog);
                stepCounter++;
                /**
                 * END LOG BLOCK
                 */

                /**
                 * Call proxy method
                 */
                result = method.invoke(srvImplementation, values);

                /**
                 * LOG BLOCK
                 */
                elapsedTime = System.currentTimeMillis() - startedTime;
                messageLog = new StringBuilder(stepCounter.toString())
                        .append(".(")
                        .append(requestUuid)
                        .append(") - Service execution has ended successfully.")
                        .append(". Elapsed time=")
                        .append(elapsedTime)
                        .toString();
                Logger.getLogger("ProtoProxyServer").log(Level.INFO, messageLog);
                stepCounter++;
                /**
                 * END LOG BLOCK
                 */
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
                    if (ph.getRequestedProtocol() == 0) {
                        ProtostuffIOUtil.writeTo(sos, response, schemaResp,
                                buffer);
                    } else {
                        JsonIOUtil.writeTo(sos, response, schemaResp,
                                ph.getJsonNumerical(), buffer);
                    }
                } finally {
                    buffer.clear();
                    sos.close();
                }

                /**
                 * LOG BLOCK
                 */
                elapsedTime = System.currentTimeMillis() - startedTime;
                messageLog = new StringBuilder(stepCounter.toString())
                        .append(".(")
                        .append(requestUuid)
                        .append(") - Service execution ended with errors.")
                        .append(". Elapsed time=")
                        .append(elapsedTime)
                        .toString();
                Logger.getLogger("ProtoProxyServer").log(Level.SEVERE, messageLog, e1);
                stepCounter++;
                /**
                 * END LOG BLOCK
                 */
                return;
            }
            /*
             * Write response to output
             */
            ResponseEnvelope response = new ResponseEnvelope(0, result, null);
            try {
                /**
                 * LOG BLOCK
                 */
                elapsedTime = System.currentTimeMillis() - startedTime;
                messageLog = new StringBuilder(stepCounter.toString())
                        .append(".(")
                        .append(requestUuid)
                        .append(") - Begin writing data to remote.")
                        .append(". Elapsed time=")
                        .append(elapsedTime)
                        .toString();
                Logger.getLogger("ProtoProxyServer").log(Level.INFO, messageLog);
                stepCounter++;
                /**
                 * END LOG BLOCK
                 */
                if (ph.getRequestedProtocol() == 0) {
                    ProtostuffIOUtil.writeTo(sos, response, schemaResp,
                            buffer);
                } else {
                    JsonIOUtil.writeTo(sos, response, schemaResp,
                            ph.getJsonNumerical(), buffer);
                }
            } finally {
                buffer.clear();
                sos.close();
                /**
                 * LOG BLOCK
                 */
                elapsedTime = System.currentTimeMillis() - startedTime;
                messageLog = new StringBuilder(stepCounter.toString())
                        .append(".(")
                        .append(requestUuid)
                        .append(") - End writing data to remote. Bye")
                        .append(". Elapsed time=")
                        .append(elapsedTime)
                        .toString();
                Logger.getLogger("ProtoProxyServer").log(Level.INFO, messageLog);
                stepCounter++;
                /**
                 * END LOG BLOCK
                 */
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
