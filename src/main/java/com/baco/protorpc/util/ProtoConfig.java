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

/**
 * General serializer configurations
 * @author deiby.nahuat
 */
public final class ProtoConfig {
    
    public static final String BINARY_MODE = "binary";
    public static final String JSON_MODE = "json";
    public static final String DEFAULT_JSON_NUMERIC_ENABLED = "true";
    public static final String DEFAULT_SERIALIZATION_MODE = BINARY_MODE;
    public static final String DEFAULT_ENABLED_COMPRESSION = "true";
    
    /**
     * Returns enabled serialization mode
     * @return 
     */
    public static final String getSerializationMode() {
        if(System.getProperty("protostuff.serialization.mode") != null &&
                !System.getProperty("protostuff.serialization.mode").trim().isEmpty() &&
                (System.getProperty("protostuff.serialization.mode").trim().equalsIgnoreCase(BINARY_MODE) ||
                System.getProperty("protostuff.serialization.mode").trim().equalsIgnoreCase(JSON_MODE))) {
            return System.getProperty("protostuff.serialization.mode").trim().toLowerCase();
        } else {
            return DEFAULT_SERIALIZATION_MODE;
        }
    }
    
    /**
     * Returns True if compression is enabled
     * @return 
     */
    public static final Boolean isCompressionEnabled() {
        if(System.getProperty("protostuff.compression.enabled") != null &&
                !System.getProperty("protostuff.compression.enabled").trim().isEmpty() &&
                (System.getProperty("protostuff.compression.enabled").trim().equalsIgnoreCase("true") ||
                System.getProperty("protostuff.compression.enabled").trim().equalsIgnoreCase("false"))) {
            return System.getProperty("protostuff.compression.enabled").trim().toLowerCase().equals("true");
        } else {
            return Boolean.FALSE;
        }
    }
    
    /**
     * Returns True if json is numerics
     * @return 
     */
    public static final Boolean isJSONNumerical() {
        if(System.getProperty("protostuff.jsonnumeric.enabled") != null &&
                !System.getProperty("protostuff.jsonnumeric.enabled").trim().isEmpty() &&
                (System.getProperty("protostuff.jsonnumeric.enabled").trim().equalsIgnoreCase("true") ||
                System.getProperty("protostuff.jsonnumeric.enabled").trim().equalsIgnoreCase("false"))) {
            return System.getProperty("protostuff.jsonnumeric.enabled").trim().toLowerCase().equals("true");
        } else {
            return Boolean.FALSE;
        }
    }
    
}
