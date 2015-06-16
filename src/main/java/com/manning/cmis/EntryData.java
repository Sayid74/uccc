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

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringImpl;
import org.apache.chemistry.opencmis.commons.server.CmisService;

public abstract class EntryData {

	private CmisService service;
	private String repositoryId;
	private ZipFile zipfile;
	private ZipEntry entry;
	private String name;

	public EntryData(CmisService service, String repositoryId, ZipFile zipfile,
			ZipEntry entry) {
		this.service = service;
		this.repositoryId = repositoryId;
		this.zipfile = zipfile;
		this.entry = entry;

		String[] path = entry.getName().split("/");
		name = path[path.length - 1];
	}

	public CmisService getService() {
		return service;
	}

	public String getRepositoryId() {
		return repositoryId;
	}

	public ZipFile getZipfile() {
		return zipfile;
	}

	public ZipEntry getEntry() {
		return entry;
	}

	public String getName() {
		return name;
	}

	/**
	 * Creates the base properties.
	 */
	protected PropertiesImpl createObjectProperties(String typeId, String name) {
		PropertiesImpl result = new PropertiesImpl();

		result.addProperty(new PropertyIdImpl(PropertyIds.OBJECT_TYPE_ID,
				typeId));
		result.addProperty(new PropertyStringImpl(PropertyIds.NAME, name));

		return result;
	}
}
