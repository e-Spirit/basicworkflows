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

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.Store;
import de.espirit.firstspirit.agency.ProjectAgent;
import de.espirit.firstspirit.agency.SpecialistsBroker;
import de.espirit.firstspirit.agency.StoreAgent;
import de.espirit.firstspirit.feature.FeatureAnalyseResult;
import de.espirit.firstspirit.feature.FeatureFile;
import de.espirit.firstspirit.feature.FeatureInstallAgent;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Imports a single ContentTransport feature into the project addressed by the given
 * project-scoped broker, but only if none of the transported elements already exist
 * (no-overwrite guarantee). The import of a feature is atomic: if any element exists
 * the whole feature is skipped.
 */
public class FeatureImporter {

    public static final Class<?> LOGGER = FeatureImporter.class;

    public ImportResult importIfAbsent(final SpecialistsBroker projectBroker, final FeatureResource featureResource)
        throws IOException {

        final ProjectAgent projectAgent = projectBroker.requestSpecialist(ProjectAgent.TYPE);
        final String projectLabel = projectAgent != null ? " [project=" + projectAgent.getId() + "]" : "";

        final byte[] zipBytes = featureResource.bytes();
        final FeatureManifest manifest = FeatureManifest.parse(zipBytes);

        final FeatureElement existing = findExistingElement(projectBroker, manifest);
        if (existing != null) {
            Logging.logInfo("Skipping feature '" + featureResource.getFileName()
                + "': element already exists and would be overwritten: " + existing + projectLabel, LOGGER);
            return ImportResult.SKIPPED_EXISTS;
        }

        final FeatureInstallAgent installAgent = projectBroker.requireSpecialist(FeatureInstallAgent.TYPE);
        try (final ByteArrayInputStream uploadStream = new ByteArrayInputStream(zipBytes)) {
            final FeatureFile featureFile = installAgent.uploadFeatureFile(uploadStream);
            final FeatureAnalyseResult analyse = installAgent.analyzeFeature(featureFile).getResult(true);
            if (analyse.getErrorsCount() > 0) {
                Logging.logWarning("Skipping feature '" + featureResource.getFileName()
                    + "': analyze reported " + analyse.getErrorsCount() + " error(s), not installable." + projectLabel, LOGGER);
                return ImportResult.SKIPPED_NOT_INSTALLABLE;
            }
            installAgent.installFeature(featureFile, null).getResult(true);
            Logging.logInfo("Imported feature '" + featureResource.getFileName() + "'." + projectLabel, LOGGER);
            return ImportResult.INSTALLED;
        } catch (final IOException e) {
            throw e;
        } catch (final Exception e) {
            throw new IOException("Feature import failed for '" + featureResource.getFileName() + "'" + projectLabel, e);
        }
    }

    private FeatureElement findExistingElement(final SpecialistsBroker projectBroker, final FeatureManifest manifest) {
        final StoreAgent storeAgent = projectBroker.requireSpecialist(StoreAgent.TYPE);
        for (final FeatureElement element : manifest.getElements()) {
            final Store store = storeAgent.getStore(element.getStoreType(), false);
            final IDProvider found = store.getStoreElement(element.getUid(), element.getUidType());
            if (found != null) {
                return element;
            }
        }
        return null;
    }
}
