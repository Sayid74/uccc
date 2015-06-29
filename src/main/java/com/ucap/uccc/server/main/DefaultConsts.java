/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ucap.uccc.server.main;

import java.io.File;
import static java.util.logging.Level.*;
import java.util.logging.Logger;

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

	public static final Logger LOG_SEVERE, LOG_WARNING, LOG_INFO, LOG_CONFIG, LOG_FINE, LOG_FINER, LOG_FINEST;
	static
	{
		//Set up Logger
		LOG_SEVERE = Logger.getLogger("com.ucap.uccc.logger.severe");
		LOG_WARNING = Logger.getLogger("com.ucap.uccc.logger.warning");
		LOG_INFO = Logger.getLogger("com.ucap.uccc.logger.info");
		LOG_CONFIG = Logger.getLogger("com.ucap.uccc.logger.config");
		LOG_FINE = Logger.getLogger("com.ucap.uccc.logger.fine");
		LOG_FINER = Logger.getLogger("com.ucap.uccc.logger.finer");
		LOG_FINEST = Logger.getLogger("com.ucap.uccc.logger.finest");

		LOG_SEVERE.setLevel(SEVERE);
		LOG_WARNING.setLevel(WARNING);
		LOG_INFO.setLevel(INFO);
		LOG_CONFIG.setLevel(CONFIG);
		LOG_FINE.setLevel(FINE);
		LOG_FINER.setLevel(FINER);
		LOG_FINEST.setLevel(FINEST) ;
	} 

	public static final String pathOfSource(String filename) {
		return RESOURCES_PATH + filename;
	}
}
