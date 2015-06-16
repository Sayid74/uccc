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

import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.server.CmisService;

public class FolderData extends EntryData {

	public FolderData(CmisService service, String repositoryId,
			ZipFile zipfile, ZipEntry entry) {
		super(service, repositoryId, zipfile, entry);
	}

	/**
	 * Creates the folder.
	 */
	public String createFolder(String parentId) {
		Properties properties = createObjectProperties("cmis:folder", getName());

		return getService().createFolder(getRepositoryId(), properties,
				parentId, null, null, null, null);
	}
}
