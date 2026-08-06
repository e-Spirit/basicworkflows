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
import de.espirit.firstspirit.access.store.pagestore.Page;
import de.espirit.firstspirit.access.store.sitestore.PageRef;

import java.util.Set;

public class PageRefExtractor implements ContentExtractor {

    private static final Class<?> LOGGER = PageRefExtractor.class;

    @Override
    public boolean canHandle(final Object value) {
        return value instanceof PageRef;
    }

    @Override
    public void extract(final Object value, final Language language, final String displayName,
                        final StringBuilder content, final Set<Long> visitedIds) {
        final PageRef pageRef = (PageRef) value;

        if (!visitedIds.add(pageRef.getId())) {
            Logging.logInfo("Skipping already visited PageRef (ID: " + pageRef.getId() + ")", LOGGER);
            return;
        }

        final Page page = pageRef.getPage();

        if (page == null) {
            Logging.logWarning("PageRef '" + pageRef.getUid() + "' (ID: " + pageRef.getId() +
                    ") has no associated page", LOGGER);
            content.append("[elementType=PageRef | name=").append(pageRef.getDisplayName(language));
            FormDataExtractionHelper.appendUid(pageRef, content);
            content.append(" | id=").append(pageRef.getId()).append("]\n");
            content.append("No page content available\n\n");
            return;
        }

        FormDataExtractionHelper.handleValue(page, language, displayName, content, visitedIds);
    }
}
