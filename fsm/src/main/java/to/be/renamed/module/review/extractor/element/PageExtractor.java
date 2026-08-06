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
import de.espirit.firstspirit.access.ReferenceEntry;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.contentstore.Dataset;
import de.espirit.firstspirit.access.store.pagestore.Content2Section;
import de.espirit.firstspirit.access.store.pagestore.Page;
import de.espirit.firstspirit.access.store.pagestore.Section;
import de.espirit.firstspirit.forms.FormData;

import java.util.Set;

public class PageExtractor implements ContentExtractor {

    private static final Class<?> LOGGER = PageExtractor.class;

    @Override
    public boolean canHandle(final Object value) {
        return value instanceof Page;
    }

    @Override
    public void extract(final Object value, final Language language, final String displayName,
                        final StringBuilder content, final Set<Long> visitedIds) {
        final Page page = (Page) value;

        if (!visitedIds.add(page.getId())) {
            Logging.logInfo("Skipping already visited Page (ID: " + page.getId() + ")", LOGGER);
            return;
        }

        content.append("[elementType=Page | name=").append(page.getDisplayName(language));
        FormDataExtractionHelper.appendUid(page, content);
        content.append(" | id=").append(page.getId()).append("]\n");

        // Extract page-level form data
        FormDataExtractionHelper.extract(page.getFormData(), language, content, visitedIds);

        // Extract content from sections
        for (Section<?> section : page.getChildren(Section.class, true)) {
            if (section instanceof Content2Section) {
                // TODO: Extract projected datasets via Content2 node in ContentStore
                //       See docs/content2-extraction-future-issues.md for migration path
                Logging.logInfo("Content2Section found, extraction denied", LOGGER);
            } else {
                extractRegularSectionContent(section, language, content, visitedIds);
            }
        }
    }

    private void extractRegularSectionContent(final Section<?> section, final Language language,
                                              final StringBuilder content, final Set<Long> visitedIds) {
        try {
            // Extract into a temporary buffer first so we can skip sections that have no
            // fields — an empty section header with no fields causes AI hallucination.
            final StringBuilder sectionContent = new StringBuilder();
            boolean extracted = false;

            try {
                final FormData formData = section.getFormData();
                FormDataExtractionHelper.extract(formData, language, sectionContent, visitedIds);
                extracted = true;
            } catch (Exception e) {
                Logging.logInfo("Cannot access FormData: " + e.getClass().getSimpleName(), LOGGER);
            }

            if (!extracted) {
                // Extract from referenced datasets
                try {
                    final Set<ReferenceEntry> references = section.getReferences();

                    assert references != null;
                    for (ReferenceEntry refEntry : references) {
                        final IDProvider referencedElement = refEntry.getReferencedElement();

                        if (referencedElement instanceof final Dataset dataset) {
                            FormDataExtractionHelper.handleValue(dataset, language,
                                    dataset.getDisplayName(language), sectionContent, visitedIds);
                        }
                    }
                } catch (Exception e) {
                    Logging.logInfo("Cannot access section references: " + e.getMessage(), LOGGER);
                }

                sectionContent.append("\n");
            }

            if (sectionContent.toString().trim().isEmpty()) {
                Logging.logInfo("Section '" + section.getDisplayName(language) + "' (id=" + section.getId() +
                        ") has no extractable fields — omitting from AI review content", LOGGER);
                return;
            }

            content.append("[elementType=Section | name=").append(section.getDisplayName(language));
            FormDataExtractionHelper.appendUid(section, content);
            content.append(" | id=").append(section.getId()).append("]\n");
            content.append(sectionContent);

        } catch (Exception e) {
            Logging.logError("Error extracting regular section content", e, LOGGER);
        }
    }
}
