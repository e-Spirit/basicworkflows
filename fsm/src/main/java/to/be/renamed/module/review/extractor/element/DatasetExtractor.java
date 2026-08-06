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
package to.be.renamed.module.review.extractor.element;

import to.be.renamed.module.review.extractor.ContentExtractor;
import to.be.renamed.module.review.extractor.FormDataExtractionHelper;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.access.store.contentstore.Dataset;
import de.espirit.or.schema.Entity;

import java.util.Set;

public class DatasetExtractor implements ContentExtractor {

    private static final Class<?> LOGGER = DatasetExtractor.class;

    @Override
    public boolean canHandle(final Object value) {
        return value instanceof Dataset;
    }

    @Override
    public void extract(final Object value, final Language language, final String displayName,
                        final StringBuilder content, final Set<Long> visitedIds) {
        final Dataset dataset = (Dataset) value;

        if (!visitedIds.add(dataset.getId())) {
            Logging.logInfo("Skipping already visited Dataset (ID: " + dataset.getId() + ")", LOGGER);
            content.append(displayName).append(": [Already extracted Dataset: ").append(dataset.getDisplayName(language)).append("]\n");
            return;
        }

        content.append("[elementType=Dataset | name=").append(dataset.getDisplayName(language));
        FormDataExtractionHelper.appendUid(dataset, content);
        content.append(" | id=").append(dataset.getId()).append("]\n");

        try {
            final Entity entity = dataset.getEntity();
            FormDataExtractionHelper.extract(dataset.getFormData(), language, content, visitedIds);
        } catch (Exception e) {
            Logging.logError("Error extracting dataset content", e, LOGGER);
        }
    }
}
