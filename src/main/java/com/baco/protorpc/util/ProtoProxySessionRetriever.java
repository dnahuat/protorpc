/*
 *  Copyright 2013, Deiby Nahuat Uc
 */
package com.baco.protorpc.util;

import com.baco.protorpc.api.ProtoSession;

/**
 * Definicion de interface para obtencion de sesion
 * @author deiby.nahuat
 */
public interface ProtoProxySessionRetriever {

	/**
	 * Obtiene la sesion asignada al transporte
	 * @return La sesion 
	 */
	ProtoSession getSession();
		
}
