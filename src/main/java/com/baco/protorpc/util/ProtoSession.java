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
	private static final long serialVersionUID = 4L;

	private String username;

	private String session;

	private String clientApp;

	public ProtoSession(String username, String clientApp) {
		this.username = username;
		this.clientApp = clientApp;
		this.session = UUID.randomUUID().toString();
	}
	
	public ProtoSession(String username, String session, String clientApp) {
		this.username = username;
		this.session = session;
		this.clientApp = clientApp;
	}

	public String getUsername() {
		return username;
	}

	public String getSession() {
		return session;
	}

	public String getClientApp() {
		return clientApp;
	}
	
}
