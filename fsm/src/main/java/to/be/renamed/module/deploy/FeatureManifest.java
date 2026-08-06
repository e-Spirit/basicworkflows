/*
 * BasicWorkflows Module
 * %%
 * Copyright (C) 2012 - 2025 Crownpeak Technology GmbH - https://www.crownpeak.com
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package to.be.renamed.module.deploy;

import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.Store;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Parses the {@code feature.xml} manifest contained in a feature transport zip. */
public final class FeatureManifest {

    private static final String MANIFEST_ENTRY = "feature.xml";

    private final List<FeatureElement> elements;

    private FeatureManifest(final List<FeatureElement> elements) {
        this.elements = Collections.unmodifiableList(elements);
    }

    public List<FeatureElement> getElements() {
        return elements;
    }

    public static FeatureManifest parse(final byte[] zipBytes) throws IOException {
        final byte[] manifestXml = readManifestEntry(zipBytes);
        if (manifestXml == null) {
            throw new IOException("feature transport zip does not contain " + MANIFEST_ENTRY);
        }
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(false);
            final Document document = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(manifestXml));

            final NodeList nodes = document.getElementsByTagName("element");
            final List<FeatureElement> parsed = new ArrayList<>(nodes.getLength());
            for (int i = 0; i < nodes.getLength(); i++) {
                final Element node = (Element) nodes.item(i);
                final Store.Type storeType = Store.Type.valueOf(node.getAttribute("store"));
                final IDProvider.UidType uidType = IDProvider.UidType.valueOf(node.getAttribute("uidType"));
                final String uid = node.getAttribute("uid");
                parsed.add(new FeatureElement(storeType, uidType, uid));
            }
            return new FeatureManifest(parsed);
        } catch (final ParserConfigurationException | SAXException e) {
            throw new IOException("Failed to parse " + MANIFEST_ENTRY, e);
        }
    }

    private static byte[] readManifestEntry(final byte[] zipBytes) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (MANIFEST_ENTRY.equals(entry.getName())) {
                    final ByteArrayOutputStream out = new ByteArrayOutputStream();
                    final byte[] buffer = new byte[8192];
                    int read;
                    while ((read = zip.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    return out.toByteArray();
                }
            }
        }
        return null;
    }
}
