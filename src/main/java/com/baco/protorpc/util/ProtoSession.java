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
	private static final long serialVersionUID = 5L;

	private final String username;

	private final String session;

	private final String clientApp;

	public ProtoSession(final String username, final String clientApp) {
		this.username = username;
		this.clientApp = clientApp;
		this.session = UUID.randomUUID().toString();
	}
	
	public ProtoSession(final String username, 
                            final String session, 
                            final String clientApp) {
		this.username = username;
		this.session = session;
		this.clientApp = clientApp;
	}

	public final String getUsername() {
		return username;
	}

	public final String getSession() {
		return session;
	}

	public final String getClientApp() {
		return clientApp;
	}
	
}
