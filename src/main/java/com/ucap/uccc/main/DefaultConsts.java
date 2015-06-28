/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ucap.uccc.main;

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
	public static final String BROWSER_HANDLER = "com.ucap.uccc.server.cmis.impl.browser.token.TokenCallContextHandler";
	public static final String AUTOM11_HANDLER = "com.ucap.uccc.server.shared.BasicAuthcallContextHandler";
    public static final String AUTOM11_ENTRY = "/cmisatom11/*";
    public static final String BROWSER_ENTRY = "/cmisbrowser/*";

	public static final String pathOfSource(String filename) {
		return RESOURCES_PATH + filename;
	}
}
