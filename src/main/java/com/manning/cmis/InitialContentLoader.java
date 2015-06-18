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
package com.manning.cmis;

import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.commons.server.CmisServiceFactory;
import com.ucap.uccc.server.cmis.impl.CallContextImpl;
import com.ucap.uccc.server.cmis.impl.ServerVersion;
import com.ucap.uccc.server.shared.ThresholdOutputStreamFactory;

/**
 * Loads the content of Zip file into the InMemory repository using the OpenCMIS
 * local binding.
 */
public class InitialContentLoader {

	private final static String TYPES_MAPPING = "types.properties";

	private final CmisServiceFactory factory;
	private final ServletContext servletContext;
	private final HttpServletRequest request;
	private final HttpServletResponse response;
	private final ThresholdOutputStreamFactory streamFactory;
	private String repositoryId;
	private String rootFolderId;

	public InitialContentLoader(CmisServiceFactory factory
			, ServletContext servletContext
			, HttpServletRequest request
			, HttpServletResponse response
			, ThresholdOutputStreamFactory streamFactory) {
		this.factory = factory;
		this.servletContext = servletContext;
		this.request = request;
		this.response = response;
		this.streamFactory = streamFactory;

		repositoryId = null;
		CmisService service = getService();
		try {
			RepositoryInfo repInfo = service.getRepositoryInfos(null).get(0);
			repositoryId = repInfo.getId();
			rootFolderId = repInfo.getRootFolderId();
		} finally {
			service.close();
		}
	}

	/**
	 * Adds the content of a Zip file.
	 * @param parameterObject TODO
	 */
	public void add(AddParameter parameterObject) throws IOException {
		ZipFile zipfile = new ZipFile(parameterObject.zip);
		CmisService service = getService();

		java.util.Properties typesMapping = new java.util.Properties();
		ZipEntry typesMappingEntry = zipfile.getEntry(TYPES_MAPPING);
		if (typesMappingEntry != null) {
			typesMapping.load(zipfile.getInputStream(typesMappingEntry));
		}

		// iterate over the Zip file
		@SuppressWarnings("unchecked")
		Enumeration<ZipEntry> entryEnumeration = (Enumeration<ZipEntry>) zipfile
				.entries();

		try {
			while (entryEnumeration.hasMoreElements()) {
				ZipEntry entry = entryEnumeration.nextElement();

				if (entry.getName().endsWith(".properties")
						|| entry.getName().endsWith(".version")
						|| entry.getName().endsWith(".series")) {
					continue;
				}

				String[] path = entry.getName().split("/");

				String parentId = getParentFolderIdByPath(service, path);
				if (parentId == null) {
					continue;
				}

				// create directories and write file
				if (entry.isDirectory()) {
					FolderData data = new FolderData(service, repositoryId,
							zipfile, entry);
					data.createFolder(parentId);
				} else {
					DocumentData data = new DocumentData(service, repositoryId,
							zipfile, entry, typesMapping);
					data.createDocument(parentId);
				}
			}
		} finally {
			service.close();
			zipfile.close();
		}
	}

	/**
	 * Returns a service object.
	 */
	public CmisService getService() {
		CallContextImpl context = new CallContextImpl(
				ServerVersion.OPENCMIS_SERVER,
				CmisVersion.CMIS_1_1,
				repositoryId,
				servletContext,
				request,
				response,
				factory,
				streamFactory);

		context.put(CallContext.USERNAME, "system");

		return factory.getService(context);
	}

	public String getParentFolderIdByPath(CmisService service, String[] path) {
		if (path.length < 2) {
			return rootFolderId;
		}

		StringBuilder pathStr = new StringBuilder();

		for (int i = 0; i < path.length - 1; i++) {
			pathStr.append("/");
			pathStr.append(path[i]);
		}

		try {
			ObjectData data = service.getObjectByPath(repositoryId,
					pathStr.toString(), null, null, null, null, null, null,
					null);
			return data.getId();
		} catch (CmisObjectNotFoundException e) {
			return null;
		}
	}
}
