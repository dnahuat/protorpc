package com.baco.protorpc.api;

/**
 * Session data transport interface
 * @author deiby.nahuat
 */
public interface ProtoSession {
    
    String  getValue(String key);
    
    void setValue(String key, String value);
    
}
