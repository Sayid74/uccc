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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServlet;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.chemistry.opencmis.commons.server.CmisServiceFactory;
import com.ucap.uccc.server.cmis.impl.CmisRepositoryContextListener;
import com.ucap.uccc.server.cmis.impl.atompub.CmisAtomPubServlet;
import com.ucap.uccc.server.cmis.impl.webservices.CmisWebServicesServlet;
import com.ucap.uccc.server.shared.ThresholdOutputStreamFactory;
import org.apache.chemistry.opencmis.server.support.filter.LoggingFilter;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import static com.ucap.uccc.server.DefaultConsts.*;

public class Server {

	private static final int DEFAULT_PORT = 8081;

	//private static final String WEBAPP = "inmemory-cmis-server-webapp.zip";
	private static final String CONTENT = "inmemory-cmis-server-content.zip";

	private static final String SERVER_PORT = "cmis.server.port";
	private static final String SERVER_WORK_DIR = "cmis.server.workdir";
	private static final String SERVER_WEBAPP = "cmis.server.webapp";
	private static final String SERVER_CONTENT = "cmis.server.content";
	private static final String SERVER_LOG_LEVEL = "cmis.server.loglevel";
	private static final String SERVER_RECORD = "cmis.server.record";
    private static final int THRESHOLD = 4 * 1024 * 1024;
    private static final int MAX_SIZE = -1;

	private static final int _BUFFER = 4096;
	private static final int _PORT;
	static
	{
		_PORT = Integer.parseInt(System.getProperty(SERVER_PORT, String.valueOf(DEFAULT_PORT)));
	}

	private Tomcat tomcat;
	private boolean record;
	private File workDir;
	private File baseDir;
	private File webDir;
	private File recordDir;

	/**
	 * InMemory CMIS Server.
	 */
	public Server() throws Exception {
		System.out.println("\n:: InMemory CMIS Server ::\n");

		String workDirPath = System.getProperty(SERVER_WORK_DIR, ".");

		// record requests?
		record = Boolean
				.parseBoolean(System.getProperty(SERVER_RECORD, "true"));

		// create directories
		workDir = new File(workDirPath);
		baseDir = new File(workDir, "cmis");
		recordDir = new File(workDir, "record");
	}

	/**
	 * Starts the server.
	 */
	public void start() throws Exception {
		System.out.println(">>>> user dir: " + System.getProperty("user.dir"));
		File directory = new File("");
		try{
			System.out.println(">>>> content path: " + directory.getCanonicalPath());
			System.out.println(">>>> absolut path: " + directory.getAbsolutePath());
		}catch(Exception e)
		{
		}
		String logLevel = System.getProperty(SERVER_LOG_LEVEL,Level.WARN.toString());
		System.out.println("Setting log level to " + logLevel + " ...");
		Logger.getRootLogger().setLevel(Level.toLevel(logLevel));
		java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.WARNING);
		
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
		/*
		addCmis10(ctx);
		this.addCmis11(ctx);
		this.addCmisAtom10(ctx);
		this.addCmisAtom11(ctx);
		*/
		addRepositoryContextListener(ctx);
		addWSServletContextListener(ctx);
		addCmisbrowser(ctx);

		factory = (CmisServiceFactory) ctx.getServletContext().getAttribute(SERVICES_FACTORY);
        streamFactory = ThresholdOutputStreamFactory.newInstance(null, THRESHOLD, MAX_SIZE, false);

		startTomcat(tomcat, ctx);
		loadInitialContent(tomcat);
		finish(tomcat, ctx);
	}

	/**
	 * Sets up the loggers.
	 */
	private void setupLoggers(Tomcat tomcat) {
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
	private void loadInitialContent(Tomcat tomcat) throws Exception {
		File content = new File(System.getProperty(SERVER_CONTENT, CONTENT));
		/*
		if (content.exists()) {
			System.out.println("Loading content from '"
					+ content.getCanonicalPath() + "' ...");

			InitialContentLoader filler = new InitialContentLoader(factory);
			filler.add(new AddParameter(content));
		}
		*/
	}

	/**
	 * Returns the context address.
	 */
	private String getContextAddress(Tomcat tomcat, Context context) {
		Connector con = tomcat.getConnector();
		return con.getScheme() + "://" + tomcat.getHost().getName() + ":"
				+ con.getPort() + context.getPath();
	}

	/**
	 * Prints final information and waits for the server shutdown.
	 */
	private void finish(Tomcat tomcat, Context context) {
		System.out.println("\nServer started.\n");

		System.out.println(getContextAddress(tomcat, context));
		// print memory
		Runtime.getRuntime().gc();
		System.out.println("Max memory: "
				+ (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + "MB");
		System.out.println();

		// print binding URLs
		String ctxAdr = getContextAddress(tomcat, context);
		System.out.println("CMIS Web Services Binding: " + ctxAdr
				+ "/services/RepositoryService?wsdl");
		System.out.println("CMIS AtomPub Binding:      " + ctxAdr + "/atom");
		System.out.println("CMIS Browser Binding:      " + ctxAdr + "/browser");

		// now wait...
		System.out.println("\nPress Ctrl+C to shutdown the server.\n");

		tomcat.getServer().await();
	}

	/**
	 * Unpacks the web app zip file into a directory.
	 */
	private void unzipWebApp(File zip, File dir) throws ZipException,
			IOException {
		ZipInputStream zipStream = new ZipInputStream(new BufferedInputStream(
				new FileInputStream(zip)));
		try {
			ZipEntry entry;
			while ((entry = zipStream.getNextEntry()) != null) {

				// build path
				StringBuilder path = new StringBuilder();
				for (String s : entry.getName().split("/")) {
					if (path.length() > 0) {
						path.append(File.separator);
					}
					path.append(s);
				}

				File file = new File(dir, path.toString());

				// create directories and write file
				if (entry.isDirectory()) {
					file.mkdirs();
				} else {
					file.getParentFile().mkdirs();

					BufferedOutputStream out = new BufferedOutputStream(
							new FileOutputStream(file), _BUFFER);

					byte[] buffer = new byte[_BUFFER];
					int b;
					while ((b = zipStream.read(buffer)) > -1) {
						out.write(buffer, 0, b);
					}

					out.flush();
					out.close();
				}

				zipStream.closeEntry();
			}
		} finally {
			zipStream.close();
		}
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
		context.addServletMapping("/cmisws10", "cmisws10");
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
		context.addServletMapping("/cmisws11", "cmisws11");
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
		context.addServletMapping("/cmisatom10", "cmisatom10");
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
		wrapper.addInitParameter("callContextHandler", "com.ucap.uccc.server.shared.BasicAuthcallContextHandler");
		wrapper.addInitParameter("cmisVersion", "1.1");
		wrapper.setLoadOnStartup(2);
		context.addServletMapping("/cmisatom11", "cmisatom11");
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
		wrapper.addInitParameter("callContextHandler", "com.ucap.uccc.server.cmis.impl.browser.token.TokenCallContextHandler");
		wrapper.addInitParameter("cmisVersion", "1.1");
		wrapper.setLoadOnStartup(2);
		context.addServletMapping("/cmisbrowser", "cmisbrowser");
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
		context.addApplicationListener("com.ucap.uccc.server.cmis.impl.WSServletContextListener");
	}
}
