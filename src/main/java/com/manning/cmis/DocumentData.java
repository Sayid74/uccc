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

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.MimeTypes;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyBooleanImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDateTimeImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDecimalImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyHtmlImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIntegerImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyUriImpl;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.commons.spi.Holder;

public class DocumentData extends EntryData {

	private java.util.Properties typesMapping;

	public DocumentData(CmisService service, String repositoryId,
			ZipFile zipfile, ZipEntry entry, java.util.Properties typesMapping) {
		super(service, repositoryId, zipfile, entry);
		this.typesMapping = typesMapping;
	}

	/**
	 * Creates the document
	 */
	public String createDocument(String parentId) throws IOException {
		if (getVersionSeriesEntry() != null) {
			return createVersionSeries(parentId);
		} else {
			return createDocumentInternal(parentId, getEntry(), null);
		}
	}

	/**
	 * Creates a document.
	 */
	public String createDocumentInternal(String parentId, ZipEntry entry,
			Boolean major) throws IOException {
		InputStream stream = getZipfile().getInputStream(entry);
		ContentStream contentStream = createContentStream(getName(), stream);

		PropertiesImpl properties = createObjectProperties(getTypeId(),
				getName());

		List<PropertyData<?>> extraProperties = getProperties(entry);
		if (extraProperties != null) {
			for (PropertyData<?> property : extraProperties) {
				properties.addProperty(property);
			}
		}

		VersioningState versioningState = null;
		if (major != null) {
			versioningState = (major ? VersioningState.MAJOR
					: VersioningState.MINOR);
		}

		return getService().createDocument(getRepositoryId(), properties,
				parentId, contentStream, versioningState, null, null, null,
				null);
	}

	/**
	 * Creates a version.
	 */
	protected String createVersionInternal(String previousVersionId,
			ZipEntry entry, boolean major) throws IOException {
		InputStream stream = getZipfile().getInputStream(entry);
		ContentStream contentStream = createContentStream(getName(), stream);

		PropertiesImpl properties = new PropertiesImpl();
		List<PropertyData<?>> extraProperties = getProperties(entry);
		if (extraProperties != null) {
			for (PropertyData<?> property : extraProperties) {
				properties.addProperty(property);
			}
		}

		properties.removeProperty(PropertyIds.OBJECT_TYPE_ID);

		if (properties.getPropertyList().isEmpty()) {
			properties = null;
		}

		Holder<String> idHolder = new Holder<String>(previousVersionId);
		getService().checkOut(getRepositoryId(), idHolder, null, null);

		getService().checkIn(getRepositoryId(), idHolder, major, properties,
				contentStream, null, null, null, null, null);

		return idHolder.getValue();
	}

	protected String createVersionSeries(String parentId)
			throws UnsupportedEncodingException, IOException {
		ZipEntry entry = getVersionSeriesEntry();

		List<String> seriesFileLines = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				getZipfile().getInputStream(entry), "UTF-8"));

		String s;
		while ((s = reader.readLine()) != null) {
			s = s.trim();
			if (s.length() > 0) {
				seriesFileLines.add(s);
			}
		}
		reader = null;

		String lastId = null;
		VersioningState versioningState = null;
		boolean first = true;
		for (String line : seriesFileLines) {
			int x = line.indexOf(' ');
			if (x == -1) {
				continue;
			}

			versioningState = VersioningState.fromValue(line.substring(0, x)
					.trim().toLowerCase(Locale.ENGLISH));
			boolean major = (versioningState == VersioningState.MAJOR || versioningState == VersioningState.CHECKEDOUT);

			String filename = line.substring(x + 1);
			String path = getEntry().getName().substring(0,
					getEntry().getName().lastIndexOf('/') + 1)
					+ filename;

			ZipEntry versionEntry = getZipfile().getEntry(path);

			if (first) {
				lastId = createDocumentInternal(parentId, versionEntry, major);
				first = false;
			} else {
				lastId = createVersionInternal(lastId, versionEntry, major);
			}
		}

		if (versioningState == VersioningState.CHECKEDOUT) {
			Holder<String> idHolder = new Holder<String>(lastId);
			getService().checkOut(getRepositoryId(), idHolder, null, null);
			lastId = idHolder.getValue();
		}

		return lastId;
	}

	protected ZipEntry getVersionSeriesEntry() {
		return getZipfile().getEntry(getEntry().getName() + ".series");
	}

	/**
	 * Gets the type id.
	 */
	protected String getTypeId() {
		String typeId = null;
		int x = getName().lastIndexOf('.');
		if (x > -1) {
			String ext = getName().substring(x + 1);
			typeId = typesMapping.getProperty(ext);
		}
		if (typeId == null) {
			typeId = "cmis:document";
		}

		return typeId;
	}

	/**
	 * Gets additional properties.
	 */
	protected List<PropertyData<?>> getProperties(ZipEntry parentEntry) {
		ZipEntry entry = getZipfile().getEntry(
				parentEntry.getName() + ".properties");
		if (entry == null) {
			return null;
		}

		List<PropertyData<?>> result = new ArrayList<PropertyData<?>>();

		try {
			java.util.Properties props = new java.util.Properties();
			props.load(getZipfile().getInputStream(entry));

			String typeId = props.getProperty(PropertyIds.OBJECT_TYPE_ID);
			if (typeId == null) {
				typeId = BaseTypeId.CMIS_DOCUMENT.value();
			}

			TypeDefinition typeDef = getService().getTypeDefinition(
					getRepositoryId(), typeId, null);
			if (typeDef == null) {
				return null;
			}

			SimpleDateFormat sdf = new SimpleDateFormat(
					"yyyy-MM-dd'T'hh:mm:ssZ");

			for (Object key : props.keySet()) {
				String propId = key.toString();

				PropertyDefinition<?> propDef = typeDef
						.getPropertyDefinitions().get(propId);
				if (propDef == null
						|| propDef.getUpdatability() == Updatability.READONLY) {
					continue;
				}

				String[] values = props.getProperty(propId).split("\n");

				switch (propDef.getPropertyType()) {
				case INTEGER:
					List<BigInteger> propValueInt = new ArrayList<BigInteger>();
					for (String value : values) {
						propValueInt.add(new BigInteger(value));
					}
					result.add(new PropertyIntegerImpl(key.toString(),
							propValueInt));
					break;
				case DECIMAL:
					List<BigDecimal> propValueDec = new ArrayList<BigDecimal>();
					for (String value : values) {
						propValueDec.add(new BigDecimal(value));
					}
					result.add(new PropertyDecimalImpl(key.toString(),
							propValueDec));
					break;
				case BOOLEAN:
					List<Boolean> propValueBool = new ArrayList<Boolean>();
					for (String value : values) {
						propValueBool.add(Boolean.valueOf(value));
					}
					result.add(new PropertyBooleanImpl(key.toString(),
							propValueBool));
					break;
				case DATETIME:
					List<GregorianCalendar> propValueDateTime = new ArrayList<GregorianCalendar>();
					for (String value : values) {
						GregorianCalendar cal = new GregorianCalendar();
						cal.setTime(sdf.parse(value));
						propValueDateTime.add(cal);
					}
					result.add(new PropertyDateTimeImpl(key.toString(),
							propValueDateTime));
					break;
				case STRING:
					result.add(new PropertyStringImpl(key.toString(), Arrays
							.asList(values)));
					break;
				case ID:
					result.add(new PropertyIdImpl(key.toString(), Arrays
							.asList(values)));
					break;
				case HTML:
					result.add(new PropertyHtmlImpl(key.toString(), Arrays
							.asList(values)));
					break;
				case URI:
					result.add(new PropertyUriImpl(key.toString(), Arrays
							.asList(values)));
					break;
				}
			}
		} catch (Exception e) {
			System.err.println("File '" + getName() + ": " + e);
			e.printStackTrace();
			return null;
		}

		return result;
	}

	/**
	 * Creates a content stream object.
	 */
	protected ContentStream createContentStream(String name, InputStream stream) {
		String mimeType = MimeTypes.getMIMEType(name);

		ContentStreamImpl result = new ContentStreamImpl(name, null, mimeType,
				new FilterInputStream(stream) {
					@Override
					public void close() throws IOException {
					}
				});

		return result;
	}
}
