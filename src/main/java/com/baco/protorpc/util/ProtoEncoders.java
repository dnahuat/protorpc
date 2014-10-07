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
package com.baco.protorpc.util;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * CHANGELOG ---------- 2012-02-09 : First version
 */
/**
 * Static methods for service name encoding
 *
 * @author deiby_nahuat
 */
public class ProtoEncoders {

    static final String HEXNUMS = "0123456789ABCDEF";

    /**
     * Converts a byte array to an hex representation
     *
     * @param data The bytearray
     * @return The hex representation as a String
     */
    public static String byteArrayToHex(byte[] data) {
        if (data == null) {
            return null;
        }
        final StringBuilder hex = new StringBuilder(2 * data.length);
        for (final byte b : data) {
            hex.append(HEXNUMS.charAt((b & 0xF0) >> 4)).append(HEXNUMS.charAt(
                    (b & 0x0F)));
        }
        return hex.toString();
    }

    /**
     * Gets a SHA1 hash from a String
     *
     * @param text The text to be hashed
     * @param encoding The text encoding
     * @return The SHA1 hash
     * @throws UnsupportedEncodingException
     */
    public static String getSha1FromString(String text, String encoding) throws UnsupportedEncodingException {
        MessageDigest md;
        if (text == null && encoding == null) {
            throw new IllegalArgumentException("text or encoding cannot be null");
        }
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
        return byteArrayToHex(md.digest(text.getBytes(encoding)));
    }

    /**
     * Gets a SHA1 hash from a String using an UTF-8 encoding
     *
     * @param text The text to be hashed
     * @return The SHA1 hash
     */
    public static String getSha1FromString(String text) {
        MessageDigest md;
        if (text == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
        try {
            return byteArrayToHex(md.digest(text.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }

    /**
     * Gets a method name (+args) unique name
     *
     * @param method The method
     * @return The method unique name
     */
    public static String getMethodUniqueName(Method method) {
        if (method == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        Class[] methodParams = method.getParameterTypes();
        sb.append(method.getName());
        for (Class klass : methodParams) {
            sb.append(klass.getName());
        }
        return sb.toString();
    }

    /**
     * Hashes a method unique name with SHA1
     *
     * @param method The method
     * @return The method unique name hashed with SHA1
     */
    public static String getMethodNameAsSha1(Method method) {
        if (method == null) {
            return null;
        }
        return getSha1FromString(getMethodUniqueName(method));
    }

}
