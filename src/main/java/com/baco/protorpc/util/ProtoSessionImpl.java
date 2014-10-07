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
        extends ProtoSession {

    private static final long serialVersionUID = 6L;
    private final String DEFAULT = "DEFAULT_SESSION";
    
    public ProtoSessionImpl() {
        super.setValue(DEFAULT, DEFAULT);
    }
    
}
