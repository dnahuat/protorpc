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

import com.baco.protorpc.util.ProtoEncoders;
import com.baco.protorpc.util.ProtoProxySessionRetriever;
import com.baco.protorpc.util.ProtoSession;
import com.baco.protorpc.util.RequestEnvelope;
import com.baco.protorpc.util.ResponseEnvelope;
import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import com.jcraft.jzlib.DeflaterOutputStream;
import com.jcraft.jzlib.InflaterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.net.ssl.HttpsURLConnection;

/**
 * CHANGELOG
 * ----------
 * 2012-02-09 : First version
 */
/**
 * Client invocation proxy
 * @author deiby_nahuat
 */
public class ProtoProxy implements InvocationHandler, Serializable {
    private static final Long serialVersionUID = 1l;
    
    protected ProtoProxyFactory factory;
    private URL url;
    private boolean isSecure = false;
    private ProtoProxyCommFailedHandler exHandler;
	private ProtoProxySessionRetriever sesRetriever;
    private final Map<Method, String> methodMap = new HashMap();
    private static ThreadLocal threadBuffer = new ThreadLocal();

    protected ProtoProxy(URL url, ProtoProxyFactory factory, Class<?> type, 
			boolean isSecure, ProtoProxyCommFailedHandler exHandler,
			ProtoProxySessionRetriever sesRetriever) {
        this.factory = factory;
        this.url = url;
        this.isSecure = isSecure;
        this.exHandler = exHandler;
		this.sesRetriever = sesRetriever;
    }

    public URL getURL() {
        return url;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {        
        synchronized (methodMap) {
            if (methodMap.get(method) == null) {
                methodMap.put(method, ProtoEncoders.getMethodNameAsSha1(method));
            }
        }
        /*
         * Get or initialize current buffer
         */
        if (threadBuffer.get() == null) {
            threadBuffer.set(LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
        }
        LinkedBuffer buffer = (LinkedBuffer) threadBuffer.get();
        
        /**
         * Prepare schemas for wrappers
         */
        Schema<RequestEnvelope> schema = RuntimeSchema.getSchema(RequestEnvelope.class);
        Schema<ResponseEnvelope> schemaResp = RuntimeSchema.getSchema(ResponseEnvelope.class);
                
        String uniqueName = methodMap.get(method);
        InputStream is;
        OutputStream os;
        /*
         * Http connection
         */
        URLConnection connection;
        if (isSecure) {
            connection = (HttpsURLConnection) url.openConnection();
            ((HttpsURLConnection) connection).setRequestMethod("POST");
        } else {
            connection = (HttpURLConnection) url.openConnection();
            ((HttpURLConnection) connection).setRequestMethod("POST");
        }
		/**
		 * Retrieve session
		 */
		ProtoSession session = new ProtoSession("unknown", UUID.randomUUID().toString());
		if(sesRetriever != null && sesRetriever.getSession() != null) {
			session = sesRetriever.getSession();
		}
        /*
         * Configure connection
         */
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setReadTimeout(500000);
        try {
            connection.connect();
        } catch (IOException ex) {
            ProtoProxyException pex = new ProtoProxyException("Error al conectar",ex.getMessage());
            exHandler.exceptionReceived(pex);
            return null;
		}		
        /*
         * Request prepare
         */
        RequestEnvelope request = new RequestEnvelope(uniqueName, session, (args != null && args.length > 0) ? args : null);
        
        /*
         * Write to stream and close it
         */
        os = connection.getOutputStream();
        DeflaterOutputStream gos = new DeflaterOutputStream(os);
        try {
            ProtostuffIOUtil.writeTo(gos, request, schema, buffer);
        } finally {
            buffer.clear();
        }        
        gos.flush();
        gos.close();
        
        /*
         * Read server response
         */
        is = connection.getInputStream();
        InflaterInputStream gis = new InflaterInputStream(is);
        ResponseEnvelope response = new ResponseEnvelope();
        ProtostuffIOUtil.mergeFrom(gis, response, schemaResp);
        gis.close();
        if (response != null) {
            if (response.getStatus() > 0) {
                ProtoProxyException pex = new  ProtoProxyException(response.getOpMessage(), response.getDetailMessage());
                exHandler.exceptionReceived(pex);
                return null;
            }
            Object value = response.getResult();
            return value;
        } else {
            ProtoProxyException pex = new  ProtoProxyException("Data transport failed", "Null response received from server");
            exHandler.exceptionReceived(pex);
        }
        return null;
    }
    
}
