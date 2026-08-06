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
import de.espirit.firstspirit.agency.SpecialistsBroker;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Imports the bundled ContentTransport workflow features into a single project (the one
 * addressed by the given project-scoped broker). Preserves the no-overwrite guarantee
 * (delegated to {@link FeatureImporter}) and the prerequisite order: a feature with a
 * prerequisite is only attempted when that prerequisite ended INSTALLED or SKIPPED_EXISTS.
 * Per-feature failures are isolated as {@link ImportResult#FAILED} and never abort the run.
 */
public class WorkflowFeatureImporter {

    public static final Class<?> LOGGER = WorkflowFeatureImporter.class;

    private final FeatureImporter featureImporter;
    private final List<FeatureResource> features;

    public WorkflowFeatureImporter() {
        this(new FeatureImporter(), Arrays.asList(FeatureResource.values()));
    }

    WorkflowFeatureImporter(final FeatureImporter featureImporter, final List<FeatureResource> features) {
        validateOrder(features);
        this.featureImporter = featureImporter;
        this.features = features;
    }

    private static void validateOrder(final List<FeatureResource> features) {
        final Set<FeatureResource> seen = new HashSet<>();
        for (final FeatureResource feature : features) {
            final FeatureResource prerequisite = feature.getPrerequisite();
            if (prerequisite != null && !seen.contains(prerequisite)) {
                throw new IllegalArgumentException(
                    "Feature '" + feature + "' requires prerequisite '" + prerequisite
                        + "' to appear earlier in the features list.");
            }
            seen.add(feature);
        }
    }

    /** Imports all configured features into the project; returns the per-feature outcome. */
    public Map<FeatureResource, ImportResult> importAll(final SpecialistsBroker projectBroker) {
        final Map<FeatureResource, ImportResult> results = new EnumMap<>(FeatureResource.class);
        for (final FeatureResource feature : features) {
            if (!prerequisiteMet(feature, results)) {
                results.put(feature, ImportResult.SKIPPED_PREREQ);
                continue;
            }
            try {
                results.put(feature, featureImporter.importIfAbsent(projectBroker, feature));
            } catch (final Exception e) {
                Logging.logError("Feature '" + feature.getFileName() + "' import failed; continuing.", e, LOGGER);
                results.put(feature, ImportResult.FAILED);
            }
        }
        return results;
    }

    private boolean prerequisiteMet(final FeatureResource feature, final Map<FeatureResource, ImportResult> results) {
        final FeatureResource prerequisite = feature.getPrerequisite();
        if (prerequisite == null) {
            return true;
        }
        final ImportResult prereqResult = results.get(prerequisite);
        if (prereqResult == ImportResult.INSTALLED || prereqResult == ImportResult.SKIPPED_EXISTS) {
            return true;
        }
        Logging.logInfo("Skipping feature '" + feature.getFileName() + "': prerequisite '"
            + prerequisite.getFileName() + "' not satisfied (" + prereqResult + ").", LOGGER);
        return false;
    }
}
