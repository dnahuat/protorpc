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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * CHANGELOG
 * ----------
 * 2012-02-09 : First version
 */
/**
 * A simple factory for invocation proxies
 * @author deiby_nahuat
 */
public class ProtoProxyFactory {

    private final ClassLoader loader;
    private boolean isHttps = false;

    public ProtoProxyFactory() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public ProtoProxyFactory(ClassLoader loader) {
        this.loader = loader;
    }
    
    public void setHttps(boolean isHttps) {
        this.isHttps = isHttps;
    }

    /**
     * Creates a new proxy
     * @param <T> The service interface class
     * @param iface The service interface
     * @param urlString The service url
     * @return A proxy using the service interface as a facade
     * @throws MalformedURLException 
     */
    public <T> T create(Class<T> iface, String urlString)
            throws MalformedURLException {
        URL url = new URL(urlString);
        if(iface == null) {
            throw new NullPointerException("Service interface class must not be null");
        }
        InvocationHandler handler = null;
        handler = new ProtoProxy(url, this, iface, isHttps);
        return (T)Proxy.newProxyInstance(loader, new Class[]{iface} , handler);
    }
}