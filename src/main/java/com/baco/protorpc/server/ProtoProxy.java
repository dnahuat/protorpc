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

import com.baco.protorpc.util.ProtoEncoders;
import com.baco.protorpc.util.RequestEnvelope;
import com.baco.protorpc.util.ResponseEnvelope;
import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import com.jcraft.jzlib.DeflaterOutputStream;
import com.jcraft.jzlib.InflaterInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletRequest;

/**
 * CHANGELOG
 * ----------
 * 2012-02-09 : First version
 */
/**
 * Service execution proxy
 *
 * @author deiby_nahuat
 */
public class ProtoProxy {
    private Object srvImplementation;
    private Map<String, Method> methodMap = new HashMap();
    private static ThreadLocal threadBuffer = new ThreadLocal();

    protected ProtoProxy(Object srvImplementation, Class srvDescriptor) throws IllegalArgumentException {
        this.srvImplementation = srvImplementation;
        /*
         * Fill method map
         */
        Method[] methods = srvDescriptor.getMethods();
        for (Method method : methods) {
            methodMap.put(ProtoEncoders.getMethodNameAsSha1(method), method);
        }

        if (srvImplementation == null) {
            throw new IllegalArgumentException("Service Implementation cannot be null");
        }
        if (!srvDescriptor.isAssignableFrom(srvImplementation.getClass())) {
            throw new IllegalArgumentException("Service " + srvImplementation + " doesn't implements " + srvDescriptor.getName());
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
    public void invoke(ServletRequest servletRequest, InputStream is, OutputStream os) throws Exception {
        /*
         * Prepare buffer Reuses thread buffer or creates a new buffer
         */
        if (threadBuffer.get() == null) {
            threadBuffer.set(LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
        }
        LinkedBuffer buffer = (LinkedBuffer) threadBuffer.get();
        /**
         * Obtain decompressed input stream
         */
        InflaterInputStream gis = new InflaterInputStream(is);
        /**
         * Prepare schemas
         */
        Schema<RequestEnvelope> schema = RuntimeSchema.getSchema(RequestEnvelope.class);
        Schema<ResponseEnvelope> schemaResp = RuntimeSchema.getSchema(ResponseEnvelope.class);
        /**
         * Obtain request from decompressed input stream
         */
        RequestEnvelope request = new RequestEnvelope();
        ProtostuffIOUtil.mergeFrom(gis, request, schema);
        gis.close();
        /**
         * Obtain compressed output stream
         */
        DeflaterOutputStream gos = new DeflaterOutputStream(os);
        /*
         * Check request validity
         */
        if (request == null || request.getMethodName() == null || request.getMethodName().trim().isEmpty()) {
            ResponseEnvelope response = new ResponseEnvelope(1, "Null request", "Client request is null", null);
            try {
                ProtostuffIOUtil.writeTo(gos, response, schemaResp, buffer);
            } finally {
                buffer.clear();
                gos.close();
            }
            return;
        }
        /*
         * Check if method exists
         */
        if (methodMap.get(request.getMethodName()) == null) {
            ResponseEnvelope response = new ResponseEnvelope(1, "Nonexistent method", "Requested method doesn't exists", null);
            try {
                ProtostuffIOUtil.writeTo(gos, response, schemaResp, buffer);
            } finally {
                buffer.clear();
                gos.close();
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
            ResponseEnvelope response = new ResponseEnvelope(1, "Invalid arguments", "The requested service doesn't exists for given arguments", null);
            try {
                ProtostuffIOUtil.writeTo(gos, response, schemaResp, buffer);
            } finally {
                buffer.clear();
                gos.close();
            }
            return;
        }
        /*
         * Invoke method
         */
        Object result = null;
        try {
            /*
             * Initialize servlet context
             */
            ProtoContext.initContext(servletRequest, request.getMethodName(), method.getDeclaringClass().getCanonicalName(), request.getSession());
            result = method.invoke(srvImplementation, values);
        } catch (Exception e) {
            Throwable e1 = e;
            if (e1 instanceof InvocationTargetException) {
                e1 = ((InvocationTargetException) e).getTargetException();
            }
            ResponseEnvelope response = new ResponseEnvelope(2, "Service invocation exception: (" + e1.getClass().getName() + ":" + e1.getMessage() + ")", getStackTrace(e1), null);
            try {
                ProtostuffIOUtil.writeTo(gos, response, schemaResp, buffer);
            } finally {
                buffer.clear();
                gos.close();
            }
            return;
        } finally {
            ProtoContext.terminateContext();
        }
        /*
         * Write response to output
         */
        ResponseEnvelope response = new ResponseEnvelope(0, "", "", result);
        try {
            ProtostuffIOUtil.writeTo(gos, response, schemaResp, buffer);
        } finally {
            buffer.clear();
            gos.close();
        }
    }

    /**
     * Obtains the stacktrace of a Throwable in a printable format
     * @param
     * @return
     */
    private static String getStackTrace(Throwable throwable) {
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        throwable.printStackTrace(printWriter);
        return writer.toString();
    }
}
