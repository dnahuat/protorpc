/*
 *  Copyright 2013, Deiby Nahuat Uc
 */
package com.baco.protorpc.exceptions;

/**
 * Exception to throw when the client makes a null request
 * @author deiby.nahuat
 */
public class ClientRequestNullException extends Exception {

	@Override
	public String getMessage() {
		return "The client request was null";
	}
	
}
