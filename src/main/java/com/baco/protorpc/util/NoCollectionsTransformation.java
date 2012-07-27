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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Cleans output from collections
 *
 * @author deiby_nahuat
 */
public class NoCollectionsTransformation implements ResultTransformation {
    
    @Override
    public void transformObject(Object object, Object parentObject, Field parentField) {        
        if (object == null
                || Map.class.isAssignableFrom(object.getClass())
                || object.getClass().isAnnotation()
                || object.getClass().isEnum()
                || object.getClass().isPrimitive()) {
            return;
        }
        if (parentObject == null && parentField == null
                && !Collection.class.isAssignableFrom(object.getClass())) {
            Field[] fields = object.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!Modifier.isStatic(field.getModifiers())
                        && !Modifier.isFinal(field.getModifiers())
                        && !Modifier.isSynchronized(field.getModifiers())
                        && !Modifier.isTransient(field.getModifiers())
                        && !Modifier.isVolatile(field.getModifiers())
                        && !Modifier.isNative(field.getModifiers())) {
                    try {
                        field.setAccessible(true);
                        Object fieldVal = field.get(object);
                        transformObject(fieldVal, object, field);
                        field.setAccessible(false);
                    } catch (IllegalArgumentException ex) {
                        ex.printStackTrace();
                    } catch (IllegalAccessException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        } else {
            if (Collection.class.isAssignableFrom(object.getClass())) {
                try {
                    parentField.set(parentObject, new ArrayList());
                } catch (IllegalArgumentException ex) {
                    ex.printStackTrace();
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                }
            } else {
                if (object.getClass().isArray()) {
                    Object[] objs = (Object[]) object;
                    for (Object obj : objs) {
                        transformObject(obj, null, null);
                    }
                } else {
                    transformObject(object, null, null);
                }
            }
            
        }
    }
}

