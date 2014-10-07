/*
 *  Copyright 2013, Deiby Nahuat Uc
 */
package com.baco.protorpc.util;

import com.baco.protorpc.api.ProtoSession;

/**
 * Session transport default implementation
 *
 * @author deiby.nahuat
 */
public class ProtoSessionImpl
        implements ProtoSession {

    private static final long serialVersionUID = 6L;
    private final String DEFAULT = "DEFAULT_SESSION";
    
    @Override
    public String getValue(String key) {
        return DEFAULT;
    }

    @Override
    public void setValue(String key, String value) {
        throw new UnsupportedOperationException("Cannot set default value");
    }

}
