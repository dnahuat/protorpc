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
package com.baco.protorpc.client;

import com.baco.protorpc.util.ProtoBufferPool;
import com.baco.protorpc.api.ProtoSession;
import com.baco.protorpc.exceptions.ProtoTransportException;
import com.baco.protorpc.exceptions.ServerResponseNullException;
import com.baco.protorpc.util.ProtoConfig;
import com.baco.protorpc.util.ProtoEncoders;
import com.baco.protorpc.util.ProtoProxySessionRetriever;
import com.baco.protorpc.util.ProtoSessionImpl;
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
import io.protostuff.runtime.DefaultIdStrategy;
import io.protostuff.runtime.Delegate;
import io.protostuff.runtime.RuntimeEnv;
import io.protostuff.runtime.RuntimeSchema;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.HttpsURLConnection;
import org.iq80.snappy.SnappyInputStream;
import org.iq80.snappy.SnappyOutputStream;

/**
 * CHANGELOG ---------- 2012-02-09 : First version
 */
/**
 * Client invocation proxy
 *
 * @author deiby_nahuat
 */
public class ProtoProxy
        implements InvocationHandler, Serializable {

    private static final Long serialVersionUID = 1l;

    private static final Integer PROTO_REQUEST_TIMEOUT = 500000;

    private URL url;
    private boolean isSecure = false;
    private ProtoRemoteExceptionHandler exHandler;
    private ProtoProxySessionRetriever sesRetriever;
    private final Map<Method, String> methodMap = new ConcurrentHashMap<Method, String>();
    private final Schema<RequestEnvelope> schema;
    private final Schema<ResponseEnvelope> schemaResp;

    protected ProtoProxy(final URL url, final boolean isSecure,
            final ProtoRemoteExceptionHandler exHandler,
            final ProtoProxySessionRetriever sesRetriever) {
        this.url = url;
        this.isSecure = isSecure;
        this.exHandler = exHandler;
        this.sesRetriever = sesRetriever;
        /**
         * Prepare schemas for wrappers
         */
        DefaultIdStrategy dis = (DefaultIdStrategy) RuntimeEnv.ID_STRATEGY;
        dis.registerDelegate(TIMESTAMP_DELEGATE);
        dis.registerDelegate(DATE_DELEGATE);
        dis.registerDelegate(TIME_DELEGATE);
        schema = RuntimeSchema.getSchema(
                RequestEnvelope.class);
        schemaResp = RuntimeSchema.getSchema(
                ResponseEnvelope.class);
    }

    public URL getURL() {
        return url;
    }

    public void setURL(URL url) {
        this.url = url;
    }

    public final void setExceptionHandler(
            final ProtoRemoteExceptionHandler exHandler) {
        this.exHandler = exHandler;
    }

    public final void setSessionRetriever(
            final ProtoProxySessionRetriever sesRetriever) {
        this.sesRetriever = sesRetriever;
    }

    public final void setSecure(boolean secure) {
        this.isSecure = secure;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        methodMap.put(method, ProtoEncoders.getMethodNameAsSha1(method));
        LinkedBuffer buffer = null;
        try {
            String serMode = ProtoConfig.getSerializationMode();
            Boolean compEnabled = ProtoConfig.isCompressionEnabled();
            Boolean isJsonNumeric = ProtoConfig.isJSONNumerical();
            
            
            buffer = ProtoBufferPool.takeBuffer();
            /*
             * Initialize current buffer
             */
            buffer.clear();
            String uniqueName = methodMap.get(method);
            InputStream is;
            OutputStream os;
            /*
             * Http connection
             */
            URLConnection connection;
            try {
                if (isSecure) {
                    connection = (HttpsURLConnection) url.openConnection();
                    ((HttpsURLConnection) connection).setRequestMethod("POST");
                } else {
                    connection = (HttpURLConnection) url.openConnection();
                    ((HttpURLConnection) connection).setRequestMethod("POST");
                }
            } catch (IOException ex) {
                ProtoTransportException pex = new ProtoTransportException(
                        "I/O error while open connection.", ex);
                if (exHandler != null) {
                    exHandler.processException(pex);
                } else {
                    throw pex;
                }
                return null;
            }
            /**
             * Retrieve session
             */
            ProtoSession session = null;
            if (sesRetriever != null && sesRetriever.getSession() != null) {
                session = sesRetriever.getSession();
            } else {
                session = new ProtoSessionImpl();
            }
            /*
             * Configure connection
             */
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setReadTimeout(PROTO_REQUEST_TIMEOUT);
            try {
                connection.connect();
            } catch (SocketTimeoutException ex) {
                ProtoTransportException pex = new ProtoTransportException(
                        "Network error", ex);
                if (exHandler != null) {
                    exHandler.processException(pex);
                } else {
                    throw pex;
                }
                return null;
            } catch (IOException ex) {
                ProtoTransportException pex = new ProtoTransportException(
                        "Couldn't connect to remote host, connection timed out",
                        ex);
                if (exHandler != null) {
                    exHandler.processException(pex);
                } else {
                    throw pex;
                }
                return null;
            }
            /*
             * Request prepare
             */
            RequestEnvelope request = new RequestEnvelope(uniqueName, session,
                    (args != null && args.length > 0) ? args : null);

            /*
             * Write to stream and close it
             */
            os = connection.getOutputStream();
            OutputStream sos;
            if(compEnabled) {
                sos = new SnappyOutputStream(os);
            } else {
                sos = os;
            }
            try {
                if(serMode.equals(ProtoConfig.BINARY_MODE)) {
                    ProtostuffIOUtil.writeTo(sos, request, schema, buffer);
                } else {
                    JsonIOUtil.writeTo(os, request, schema, isJsonNumeric, buffer);
                }
                sos.flush();
                sos.close();
            } catch (IOException ex) {
                ProtoTransportException pex = new ProtoTransportException(
                        "Error while writing to server.", ex);
                if (exHandler != null) {
                    exHandler.processException(pex);
                } else {
                    throw pex;
                }
                return null;
            } finally {
                buffer.clear();
            }

            /*
             * Read server response
             */
            ResponseEnvelope response = null;
            try {
                is = connection.getInputStream();
                InputStream sis;
                if(compEnabled) {
                    sis = new SnappyInputStream(is);
                } else {
                    sis = is;
                }
                response = schemaResp.newMessage();
                if(serMode.equals(ProtoConfig.JSON_MODE)) {
                    ProtostuffIOUtil.mergeFrom(sis, response, schemaResp);
                } else {
                    JsonIOUtil.mergeFrom(sis, response, schemaResp, isJsonNumeric, buffer);
                }
                sis.close();
            } catch (IOException ex) {
                ProtoTransportException pex = new ProtoTransportException(
                        "Couldn't read server response, connection failed.", ex);
                if (exHandler != null) {
                    exHandler.processException(pex);
                } else {
                    throw pex;
                }
                return null;
            }
            if (response != null) {
                if (response.getStatus() > 0) {
                    if (exHandler != null) {
                        exHandler.processException(response.getThrowable());
                    } else {
                        throw response.getThrowable();
                    }
                }
                Object value = response.getResult();
                return value;
            } else {
                ServerResponseNullException pex = new ServerResponseNullException(
                        null);
                if (exHandler != null) {
                    exHandler.processException(pex);
                } else {
                    throw pex;
                }
                return null;
            }
        } catch (InterruptedException ex) {
            ProtoTransportException pex = new ProtoTransportException(
                    "Couldn't obtain a free buffer to write to remote. Max capacity reached.",
                    ex);
            if (exHandler != null) {
                exHandler.processException(pex);
            } else {
                throw pex;
            }
            return null;
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
