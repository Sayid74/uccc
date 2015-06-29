/**
 * Copyright 2012 Manning Publications Co.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ucap.uccc.main;

import java.io.File;

import javax.servlet.http.HttpServlet;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.chemistry.opencmis.commons.server.CmisServiceFactory;
import com.ucap.uccc.server.cmis.impl.atompub.CmisAtomPubServlet;
import com.ucap.uccc.server.cmis.impl.webservices.CmisWebServicesServlet;
import com.ucap.uccc.server.shared.ThresholdOutputStreamFactory;
import org.apache.chemistry.opencmis.server.support.filter.LoggingFilter;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import static com.ucap.uccc.main.DefaultConsts.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class Server {
	private static final int DEFAULT_PORT = 8081;
	private static final String SERVER_PORT = "cmis.server.port";
	private static final String SERVER_WORK_DIR = "cmis.server.workdir";
	private static final String SERVER_LOG_LEVEL = "cmis.server.loglevel";
	private static final String SERVER_RECORD = "cmis.server.record";
    private static final int THRESHOLD = 4 * 1024 * 1024;
    private static final int MAX_SIZE = -1;
	private final boolean record;
	private final File workDir;
	private final File recordDir;
	private static final int _PORT;

	private Tomcat tomcat;

	static
	{
		_PORT = Integer.parseInt(System.getProperty(SERVER_PORT, String.valueOf(DEFAULT_PORT)));
	}

	/**
	 * InMemory CMIS Server.
	 */
	public Server() throws Exception {
		System.out.println("\n:: InMemory CMIS Server ::\n");
		String workDirPath = System.getProperty(SERVER_WORK_DIR, ".");

		// record requests?
		record = Boolean.parseBoolean(System.getProperty(SERVER_RECORD, "true"));

		// create directories
		workDir = new File(workDirPath);
		recordDir = new File(workDir, "record");
	}

	/**
	 * Starts the server.
	 */
	public void start() throws Exception {
		// Set up logger
		String logLevel = System.getProperty(SERVER_LOG_LEVEL, Level.WARN.toString());
		Logger.getRootLogger().setLevel(Level.toLevel(logLevel));
		java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.WARNING);
		System.out.println("Setting log level to " + logLevel + " ...");
		
		//Create tomcat application
		tomcat = new Tomcat();
		tomcat.setPort(_PORT);
		createWebApp(tomcat);
	}

	public CmisServiceFactory getCmisFactory()
	{
		return this.factory;
	}

	public ThresholdOutputStreamFactory getStreamFactory()
	{
		return this.streamFactory;
	}


	/**
	 * Creates the web app directory and unpacks the web app content.
	 */
	private CmisServiceFactory factory;
	private ThresholdOutputStreamFactory streamFactory;
	private void createWebApp(Tomcat tomcat) throws Exception {
		System.out.println(new File(".").getAbsolutePath());
		Context ctx = tomcat.addContext("/", new File(".").getAbsolutePath());
		addRepositoryContextListener(ctx);
		//addWSServletContextListener(ctx);

		/*
		addCmis10(ctx);
		this.addCmisAtom10(ctx);
		*/
		addCmisAtom11(ctx);
		addCmisbrowser(ctx);

		factory = (CmisServiceFactory) ctx.getServletContext().getAttribute(SERVICES_FACTORY);
        streamFactory = ThresholdOutputStreamFactory.newInstance(null, THRESHOLD, MAX_SIZE, false);

		startTomcat(tomcat, ctx);
		loadInitialContent();
		finish(tomcat, ctx);
	}

	/**
	 * Starts the embedded Tomcat.
	 */
	private void startTomcat(Tomcat tomcat, Context context) throws Exception {
		System.out.println("Starting Tomcat on port " + _PORT + " ...");

		if (record) {
			FilterDef filterDef = new FilterDef();
			filterDef.setFilterName("LoggingFilter");
			filterDef.setFilterClass(LoggingFilter.class.getName());
			filterDef.addInitParameter("LogDir", recordDir.getCanonicalPath());
			filterDef.addInitParameter("PrettyPrint", "true");
			filterDef.addInitParameter("LogHeader", "true");
			filterDef.addInitParameter("Indent", "4");
			context.addFilterDef(filterDef);

			FilterMap filterMap = new FilterMap();
			filterMap.setFilterName("LoggingFilter");
			filterMap.addURLPattern("/services/*");
			filterMap.addURLPattern("/atom/*");
			filterMap.addURLPattern("/browser/*");
			context.addFilterMap(filterMap);
		}

		tomcat.start();
	}

	/**
	 * Loads the initial content into the repository.
	 */
	private void loadInitialContent() throws Exception {
		/**
		 * TO DO
		 */
	}

	/**
	 * Returns the context address.
	 */
	private final String getContextAddress(Tomcat tomcat, Context context) {
		Connector con = tomcat.getConnector();
		return con.getScheme() + "://" + tomcat.getHost().getName() + ":"
				+ con.getPort() + context.getPath();
	}

	/**
	 * Prints final information and waits for the server shutdown.
	 */
	private final void finish(Tomcat tomcat, Context context) {
		System.out.println("\nServer started.\n");

		System.out.println(getContextAddress(tomcat, context));
		// print memory
		Runtime.getRuntime().gc();

		// print binding URLs
		String ctxAdr = getContextAddress(tomcat, context);
		//System.out.println("CMIS Web Services Binding: " + ctxAdr + "/services/RepositoryService?wsdl");
		System.out.println("CMIS AtomPub Binding:      " + ctxAdr + AUTOM11_ENTRY);
		System.out.println("CMIS Browser Binding:      " + ctxAdr + BROWSER_ENTRY);

		// now wait...
		System.out.println("\nPress Ctrl+C to shutdown the server.\n");

		tomcat.getServer().await();
	}

	public static void main(String[] args) throws Exception {
		(new Server()).start();
	}
	
	private void addCmis10(Context context) {
		/**
		 *
        <servlet-name>cmisws10</servlet-name>
        <servlet-class>org.apache.chemistry.opencmis.server.impl.webservices.CmisWebServicesServlet</servlet-class>
        <init-param>
            <param-name>cmisVersion</param-name>
            <param-value>1.0</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
		 */
		HttpServlet servlet = new CmisWebServicesServlet();
		Wrapper wrapper = Tomcat.addServlet(context, "cmisws10", servlet);
		wrapper.addInitParameter("cmisVersion", "1.0");
		wrapper.setLoadOnStartup(1);
		context.addServletMapping("/cmisws10/*", "cmisws10");
	}

	private void addCmis11(Context context) {
		/**
		 *
        <servlet-name>cmisws10</servlet-name>
        <servlet-class>org.apache.chemistry.opencmis.server.impl.webservices.CmisWebServicesServlet</servlet-class>
        <init-param>
            <param-name>cmisVersion</param-name>
            <param-value>1.0</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
		 */
		
		HttpServlet servlet = new CmisWebServicesServlet();
		Wrapper wrapper = Tomcat.addServlet(context, "cmisws11", servlet);
		wrapper.addInitParameter("cmisVersion", "1.1");
		wrapper.setLoadOnStartup(1);
		context.addServletMapping("/cmisws11/*", "cmisws11");
	}

	private void addCmisAtom10(Context context) {
		/**
        <servlet-name>cmisatom10</servlet-name>
        <servlet-class>org.apache.chemistry.opencmis.server.impl.atompub.CmisAtomPubServlet</servlet-class>
        <init-param>
            <param-name>callContextHandler</param-name>
            <param-value>org.apache.chemistry.opencmis.server.shared.BasicAuthCallContextHandler</param-value>
        </init-param>
        <init-param>
            <param-name>cmisVersion</param-name>
            <param-value>1.0</param-value>
        </init-param>
        <load-on-startup>2</load-on-startup>
		 **/
		HttpServlet servlet = new CmisAtomPubServlet();
		Wrapper wrapper = Tomcat.addServlet(context, "cmisatom10", servlet);
		wrapper.addInitParameter("callContextHandler", "com.ucap.uccc.server.shared.BasicAuthcallContextHandler");
		wrapper.addInitParameter("cmisVersion", "1.0");
		wrapper.setLoadOnStartup(2);
		context.addServletMapping("/cmisatom10/*", "cmisatom10");
	}

	private void addCmisAtom11(Context context) {
		/**
        <servlet-name>cmisatom11</servlet-name>
        <servlet-class>org.apache.chemistry.opencmis.server.impl.atompub.CmisAtomPubServlet</servlet-class>
        <init-param>
            <param-name>callContextHandler</param-name>
            <param-value>org.apache.chemistry.opencmis.server.shared.BasicAuthCallContextHandler</param-value>
        </init-param>
        <init-param>
            <param-name>cmisVersion</param-name>
            <param-value>1.1</param-value>
        </init-param>
        <load-on-startup>2</load-on-startup>
		 **/
		HttpServlet servlet = new CmisAtomPubServlet();
		Wrapper wrapper = Tomcat.addServlet(context, "cmisatom11", servlet);
		wrapper.addInitParameter("callContextHandler", AUTOM11_HANDLER);
		wrapper.addInitParameter("cmisVersion", "1.1");
		wrapper.setLoadOnStartup(2);
		context.addServletMapping(AUTOM11_ENTRY, "cmisatom11");
	}
	
	private void addCmisbrowser(Context context) {
		/**
        <servlet-name>cmisbrowser</servlet-name>
        <servlet-class>org.apache.chemistry.opencmis.server.impl.browser.CmisBrowserBindingServlet</servlet-class>
        <init-param>
            <param-name>callContextHandler</param-name>
            <param-value>org.apache.chemistry.opencmis.server.impl.browser.token.TokenCallContextHandler</param-value>
        </init-param>
        <load-on-startup>2</load-on-startup>
		 **/
		HttpServlet servlet = new CmisAtomPubServlet();
		Wrapper wrapper = Tomcat.addServlet(context, "cmisbrowser", servlet);
		wrapper.addInitParameter("callContextHandler", BROWSER_HANDLER); 
		wrapper.addInitParameter("cmisVersion", "1.1");
		wrapper.setLoadOnStartup(2);
		context.addServletMapping(BROWSER_ENTRY, "cmisbrowser");
	}

	private void addRepositoryContextListener(Context context) {
		/**
		 <listener>
        	<listener-class>org.apache.chemistry.opencmis.server.impl.CmisRepositoryContextListener</listener-class>
    	</listener>
		**/
		context.addApplicationListener("com.ucap.uccc.server.cmis.impl.CmisRepositoryContextListener");
	}
	
	private void addWSServletContextListener(Context context) {
		/**
		<listener>
       		<listener-class>com.sun.xml.ws.transport.http.servlet.WSServletContextListener</listener-class>
    	</listener>
		**/
		
		context.addApplicationListener("com.sun.xml.ws.transport.http.servlet.WSServletContextListener");
	}
}
