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

import com.baco.protorpc.exceptions.ProtoException;
import com.baco.protorpc.exceptions.RemoteServerException;
import java.io.Serializable;

/**
 * Response wrapper
 *
 * @author deiby_nahuat
 */
public class ResponseEnvelope implements Serializable {
    /* --- OpStatus ---
     * 0 : Everything OK
     * 1 : Error
     */

    private static final Long serialVersionUID = 8l;
    private final Integer opStatus;
    private final Object result;
    private final ProtoException throwable;

    public ResponseEnvelope(final Integer opStatus, 
                            final Object result, 
                            final ProtoException throwable) {
        this.opStatus = opStatus;
        this.result = result;
        this.throwable = throwable;
    }

    /**
     * Gets the operation status
     *
     * @return The status
     */
    public final Integer getStatus() {
        return opStatus;
    }

    /**
     * Gets the operation result
     *
     * @return The result
     */
    public final Object getResult() {
        return result;
    }

    /**
     * Gets the operation exception or null if no error ocurred
     *
     * @return The process exception
     */
    public final ProtoException getThrowable() {
        return throwable;
    }

}
