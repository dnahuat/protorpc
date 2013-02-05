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

import com.baco.protorpc.util.ProtoSession;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;

/**
 * CHANGELOG
 * ----------
 * 2012-02-09 : First version
 */
/**
 * Servlet context manager 
 * @author deiby_nahuat
 */
public class ProtoContext {
    /* Used to atach this context to a servlet */
    private static final ThreadLocal threadContext = new ThreadLocal();
    /* The client request attached to this context */
    private ServletRequest request;    
    /* The service name requested by the client */
    private String serviceName;
    /* The service UUID requested by the client */
    private String serviceUuid;
	/* The client session */
	private ProtoSession session;
    /* The number of clients this context is serving */
    private int contextCount = 0;
    
    private ProtoContext() {        
    }
    
    /**
     * Initializes a new context
     * @param request The client request
     * @param serviceUuid The service UUID
     * @param serviceName The service name
     * @throws ServletException 
     */
    public static void initContext(ServletRequest request,
                                   String serviceUuid,
                                   String serviceName,
								   ProtoSession session) throws ServletException {
        ProtoContext context = (ProtoContext) threadContext.get();
        if(context == null) {
            context = new ProtoContext();
            threadContext.set(context);
        }
        context.request = request;
        context.serviceName = serviceName;
        context.serviceUuid = serviceUuid;
		context.session = session;
        context.contextCount++;
    }
    
    /**
     * Gets the current context attached to the calling servlet
     * @return The context
     */
    public static ProtoContext getContext() {
        return (ProtoContext) threadContext.get();
    }
    
    /**
     * Gets the servlet request attached to the calling servlet
     * @return The client request
     */
    public static ServletRequest getContextRequest() {
        ProtoContext context = (ProtoContext) threadContext.get();
        if(context != null) {
            return context.request;
        } else {
            return null;
        }
    }
    
    /**
     * Gets the current service name
     * @return The service name
     */
    public static String getContextServiceName() {
        ProtoContext context = (ProtoContext) threadContext.get();
        if(context != null) {
            return context.serviceName;
        } else {
            return null;
        }
    }
    
    /**
     * Gets the current service UUID
     * @return The service UUID
     */
    public static String getContextServiceUuid() {
        ProtoContext context = (ProtoContext) threadContext.get();
        if(context != null) {
            return context.serviceUuid;
        } else {
            return null;
        }
    }

	/**
	 * Gets the current context session
	 * @return The session
	 */
	public static ProtoSession getContextSession() {
		ProtoContext context = (ProtoContext) threadContext.get();
		if(context != null) {
			return context.session;
		} else {
			return new ProtoSession("unknown", UUID.randomUUID().toString(), "unknown_client");
		}
	}
    
    /**
     * Terminates the current context
     */
    public static void terminateContext() {
        ProtoContext context = (ProtoContext) threadContext.get();
        if(context != null && --context.contextCount == 0) {
            context.request = null;
            context.serviceName = "";
            context.serviceUuid = "";
        }
    }
    
}
