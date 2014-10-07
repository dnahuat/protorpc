package com.baco.protorpc.api;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session data transport interface
 * @author deiby.nahuat
 */
public class ProtoSession implements Serializable {
    
    private final Map<String, String> valueMap = new ConcurrentHashMap<String, String>();
    
    protected String  getValue(String key) {
        return valueMap.get(key);
    }
    
    protected void setValue(String key, String value) {
        valueMap.put(key, value);
    }
    
    protected void clearValues() {
        valueMap.clear();
    }
    
}
