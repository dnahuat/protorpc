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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * CHANGELOG ---------- 2012-02-09 : First version
 */
/**
 * ProtoRpc servlet
 *
 * @author deiby_nahuat
 */
public abstract class ProtoServlet
        extends GenericServlet {
    /* Invocation proxy */

    private ProtoProxy proxy;
    /* Service implementation */
    private Object serviceImpl;
    /* Service interface */
    private Class serviceIface;

    /**
     * Finds the service interface class
     *
     * @param implClass The implementation class
     * @return The service interface class
     */
    private Class findServiceIface(Class implClass) {
        if (implClass == null) {
            return null;
        }

        Class[] interfaces = implClass.getInterfaces();

        if (interfaces.length == 1) {
            return interfaces[0];
        }

        return findServiceIface(implClass.getSuperclass());
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        if (getClass().equals(ProtoServlet.class)) {
            throw new ServletException("Server must extend ProtoServlet");
        }
        /* Init proxy */
        serviceImpl = this;
        serviceIface = findServiceIface(serviceImpl.getClass());
        proxy = new ProtoProxy(serviceImpl, serviceIface, getSessionValidators());
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public abstract String getServletInfo();

    /**
     * Devuelve una coleccion de validadores de sesion y los aplica
     * secuencialmente
     *
     * @return
     */
    public abstract SessionValidator[] getSessionValidators();

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        if (!req.getMethod().equals("POST")) {
            res.sendError(500,
                    "This server only accepts Proto-serialized requests");
            PrintWriter out = res.getWriter();
            res.setContentType("text/html");
            out.println("<h1>ProtoRpc Requires POST</h1>");
            out.print("<p>");
            out.print(getServletInfo());
            out.println("</p>");
            return;
        }
        /* Invokes proxy */
        try {
            InputStream is = request.getInputStream();
            OutputStream os = response.getOutputStream();
            invoke(req, is, os);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new ServletException(e);
        }
    }

    /**
     * Invoke proxy method
     *
     * @param request The client request
     * @param is The input stream from client
     * @param os The output stream to client
     * @throws Exception
     */
    protected void invoke(HttpServletRequest request, InputStream is,
            OutputStream os)
            throws Exception {
        proxy.invoke(request, is, os);
    }

}
