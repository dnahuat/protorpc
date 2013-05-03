/*
 *  Copyright 2013, Deiby Nahuat Uc
 */
package com.baco.protorpc.exceptions;

/**
 * Exception class to throw when a requested method doesn't exists 
 * @author deiby.nahuat
 */
public class MethodDoesntExistsException extends Exception {
	
	private final String requestedMethod;
	
	public MethodDoesntExistsException(String requestedMethod) {
		this.requestedMethod = requestedMethod;
	}

	@Override
	public String getMessage() {
		return "Method: " + requestedMethod + " , doesn't exists";
	}
	
}
