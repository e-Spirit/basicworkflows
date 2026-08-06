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
package to.be.renamed.module.review.extractor.composite;

import to.be.renamed.module.review.extractor.ContentExtractor;
import to.be.renamed.module.review.extractor.FormDataExtractionHelper;

import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.client.access.editor.lists.Catalog;
import de.espirit.firstspirit.forms.FormData;

import java.util.Set;

public class CatalogContentExtractor implements ContentExtractor {

    @Override
    public boolean canHandle(final Object value) {
        return value instanceof Catalog;
    }

    @Override
    public void extract(final Object value, final Language language, final String displayName,
                        final StringBuilder content, final Set<Long> visitedIds) {
        final Catalog catalog = (Catalog) value;

        if (catalog.isEmpty()) {
            content.append(displayName).append(": (empty catalog)\n");
            return;
        }

        content.append(displayName).append(": [Catalog with ").append(catalog.size()).append(" entries]\n");

        int cardIndex = 0;
        for (final Catalog.Card card : catalog) {
            cardIndex++;
            try {
                final String templateUid = card.getTemplate().getUid();
                content.append("[Catalog Entry ").append(cardIndex)
                        .append(" | template=").append(templateUid);
            } catch (Exception e) {
                content.append("[Catalog Entry ").append(cardIndex);
            }
            try {
                content.append(" | cardId=").append(card.getId());
            } catch (Exception e) {
                // Card id unavailable (older FS or unexpected state) — omit it; navigation then
                // falls back to opening the owning section without card scoping.
            }
            content.append("]\n");

            final FormData cardData = card.getItem();
            FormDataExtractionHelper.extract(cardData, language, content, visitedIds);
        }
    }
}
