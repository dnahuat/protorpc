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

import java.io.Serializable;

/**
 * CHANGELOG
 * ----------
 * 2012-02-09 : First version
 */
/**
 * Response wrapper
 * @author deiby_nahuat
 */
public class ResponseEnvelope implements Serializable {
    /* --- OpStatus ---
     * 0 : Everything OK
     * 1 : Method doesn't exists
     * 2 : Internal Error
     */
    private static final Long serialVersionUID = 1l;
    private Object result = null;    
    private Integer opStatus = 0;
    private String detailMessage = "";
    private String opMessage = "";
	private String stacktrace = "";
    
    public ResponseEnvelope() {
    }
    
    public ResponseEnvelope(Integer opStatus, String opMessage, String detailMessage, String stacktrace, Object result) {
        this.opStatus = opStatus;
        this.result = result;
        this.opMessage = opMessage;
		this.detailMessage = detailMessage;
		this.stacktrace = stacktrace;
    }

    /**
     * Gets the operation status
     * @return The status
     */
    public Integer getStatus() {
        return opStatus;
    }
    
    /**
     * Gets the operation message
     * @return The message
     */
    public String getOpMessage() {
        return opMessage;
    }
    
    /**
     * Gets the operation detailed message
     * @return The detailed message
     */
    public String getDetailMessage() {
        return detailMessage;
    }

	/**
	 * Returns the operation stacktrace if any, or an
	 * empty string
	 * @return The operation stacktrace 
	 */
	public String getStacktrace() {
		return stacktrace;
	}
    
    /**
     * Gets the operation result
     * @return The result
     */
    public Object getResult() {
        return result;
    }
        
}
