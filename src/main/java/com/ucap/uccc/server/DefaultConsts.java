/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ucap.uccc.server;

import java.io.File;

/**
 *
 * @author Sayid
 */
public final class DefaultConsts {
	public static final String RESOURCES_PATH="resources" + File.separator;	
    public static final String REPOSITORY_CF = "repository.properties";
    public static final String SERVICES_FACTORY = "org.apache.chemistry.opencmis.servicesfactory";
    public static final String CNPRM_CALL_CONTEXT_HANDLER = "callContextHandler";
    public static final String CNPRM_CMIS_VERSION = "cmisVersion";

	public static final String pathOfSource(String filename) {
		return RESOURCES_PATH + filename;
	}
}
