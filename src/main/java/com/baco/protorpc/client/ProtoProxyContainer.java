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

import com.baco.protorpc.util.ProtoProxySessionRetriever;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Contenedor de protoproxy
 *
 * @author deiby.nahuat
 */
public class ProtoProxyContainer {

    private final ClassLoader loader;
    private boolean isHttps = false;
    private InvocationHandler handler;
    private String urlString;
    private URL url;
    private ProtoRemoteExceptionHandler exHandler;
    private ProtoProxySessionRetriever sesRetriever;

    public ProtoProxyContainer(final Class service,
            final String urlString,
            final ProtoRemoteExceptionHandler exHandler,
            final ProtoProxySessionRetriever sesRetriever,
            final Boolean isHttps) {
        loader = Thread.currentThread().getContextClassLoader();
        this.urlString = urlString;
        this.exHandler = exHandler;
        this.sesRetriever = sesRetriever;
    }

    public final void setUrlString(final String urlString) {
        this.urlString = urlString;
        url = null;
    }

    public final void setExceptionHandler(
            final ProtoRemoteExceptionHandler exHandler) {
        this.exHandler = exHandler;
    }

    public final void setSessionRetriever(
            final ProtoProxySessionRetriever sesRetriever) {
        this.sesRetriever = sesRetriever;
    }

    public final void setHttps(Boolean isHttps) {
        this.isHttps = isHttps;
    }

    public final Boolean isHttps() {
        return isHttps;
    }

    public <T> T getProxyInstance(final Class service) throws MalformedURLException, IllegalArgumentException {
        String protocol = isHttps ? "https://" : "http://";
        if (url == null) {
            url = new URL(protocol + urlString);
        }
        if (handler == null) {
            handler = new ProtoProxy(url, isHttps, exHandler, sesRetriever);            
        } else {
            ((ProtoProxy) handler).setURL(url);
            ((ProtoProxy) handler).setSecure(isHttps);
            ((ProtoProxy) handler).setExceptionHandler(exHandler);
            ((ProtoProxy) handler).setSessionRetriever(sesRetriever);
        }
        return (T) Proxy.newProxyInstance(loader, new Class[]{service},
                    handler);
    }

}
