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
import de.espirit.firstspirit.access.store.mediastore.Media;

import java.util.Set;

public class MediaExtractor implements ContentExtractor {

    private static final Class<?> LOGGER = MediaExtractor.class;

    @Override
    public boolean canHandle(final Object value) {
        return value instanceof Media;
    }

    @Override
    public void extract(final Object value, final Language language, final String displayName,
                        final StringBuilder content, final Set<Long> visitedIds) {
        final Media media = (Media) value;

        if (!visitedIds.add(media.getId())) {
            Logging.logInfo("Skipping already visited Media (ID: " + media.getId() + ")", LOGGER);
            return;
        }

        content.append(displayName).append(": [Media: ").append(media.getDisplayName(language));
        FormDataExtractionHelper.appendUid(media, content);
        content.append(" | filename=").append(media.getFilename())
                .append(" | type=").append(media.getType() == Media.PICTURE ? "picture" : "file")
                .append(" | id=").append(media.getId()).append("]\n");
    }
}
