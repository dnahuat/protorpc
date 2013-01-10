/*
 *  Copyright 2013, Deiby Nahuat Uc
 */
package com.baco.protorpc.util;

import java.io.Serializable;
import java.util.UUID;

/**
 * Session transport
 * @author deiby.nahuat
 */
public class ProtoSession implements Serializable {
	private static final long serialVersionUID = 3L;

	private String username;

	private String session;

	public ProtoSession(String username) {
		this.username = username;
		this.session = UUID.randomUUID().toString();
	}
	
	public ProtoSession(String username, String session) {
		this.username = username;
		this.session = session;
	}

	public String getUsername() {
		return username;
	}

	public String getSession() {
		return session;
	}
	
}
