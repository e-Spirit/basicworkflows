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

package to.be.renamed.module.review.extractor;

import to.be.renamed.module.review.extractor.composite.CatalogContentExtractor;
import to.be.renamed.module.review.extractor.composite.DatasetContainerContentExtractor;
import to.be.renamed.module.review.extractor.composite.TargetReferenceContentExtractor;
import to.be.renamed.module.review.extractor.element.DatasetExtractor;
import to.be.renamed.module.review.extractor.element.MediaExtractor;
import to.be.renamed.module.review.extractor.element.PageExtractor;
import to.be.renamed.module.review.extractor.element.PageRefExtractor;
import to.be.renamed.module.review.extractor.field.BooleanContentExtractor;
import to.be.renamed.module.review.extractor.field.DateContentExtractor;
import to.be.renamed.module.review.extractor.field.DomContentExtractor;
import to.be.renamed.module.review.extractor.field.LinkContentExtractor;
import to.be.renamed.module.review.extractor.field.NumberContentExtractor;
import to.be.renamed.module.review.extractor.field.OptionContentExtractor;
import to.be.renamed.module.review.extractor.field.StringContentExtractor;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.access.editor.EditorValue;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.templatestore.gom.GomFormElement;
import de.espirit.firstspirit.forms.FormData;
import de.espirit.firstspirit.forms.FormField;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FormDataExtractionHelper {

    private static final Class<?> LOGGER = FormDataExtractionHelper.class;

    private static final List<ContentExtractor> VALUE_EXTRACTORS = Arrays.asList(
            // Leaf extractors (simple values)
            new StringContentExtractor(),
            new NumberContentExtractor(),
            new BooleanContentExtractor(),
            new DateContentExtractor(),
            new DomContentExtractor(),
            new LinkContentExtractor(),
            new OptionContentExtractor(),
            // Recursive extractors (unwrap and recurse via handleValue)
            new CatalogContentExtractor(),
            new DatasetContainerContentExtractor(),
            new TargetReferenceContentExtractor(),
            // Store element extractors (top-level and referenced elements)
            new PageRefExtractor(),
            new PageExtractor(),
            new DatasetExtractor(),
            new MediaExtractor()
    );

    private FormDataExtractionHelper() {
    }

    public static void extract(final FormData formData, final Language language,
                               final StringBuilder content, final Set<Long> visitedIds) {
        if (formData == null) {
            Logging.logWarning("FormData is null, skipping extraction", LOGGER);
            return;
        }

        try {
            int fieldCount = 0;

            for (Object element : formData.getForm()) {
                String name = null;
                String displayName = null;

                if (element instanceof final GomFormElement gomElement) {
                    name = gomElement.getName().toString();
                    final String label = gomElement.label(language.getAbbreviation());
                    displayName = (label != null && !label.equals(name)) ? label + " (" + name + ")" : name;
                } else if (element instanceof final FormField<?> field) {
                    name = field.getName();
                    displayName = name;
                } else {
                    Logging.logInfo("Skipping non-field element: " + element.getClass().getSimpleName(), LOGGER);
                }

                if (name != null) {
                    fieldCount++;
                    final Object unwrapped = unwrap(formData.get(language, name), language);
                    Logging.logInfo("Field " + fieldCount + ": " + displayName +
                            " -> " + (unwrapped != null ? unwrapped.getClass().getSimpleName() : "null"), LOGGER);

                    if (unwrapped == null) {
                        content.append(displayName).append(": (empty)\n");
                    } else {
                        handleValue(unwrapped, language, displayName, content, visitedIds);
                    }
                }
            }

            content.append("\n");

        } catch (Exception e) {
            Logging.logError("Error extracting form data", e, LOGGER);
        }
    }

    /**
     * Tries all registered ContentExtractors on the given value.
     * Called by FormDataExtractionHelper and recursively by composite extractors
     * (Catalog, DatasetContainer, TargetReference) when they unwrap nested values.
     */
    public static void handleValue(final Object value, final Language language,
                                   final String displayName, final StringBuilder content,
                                   final Set<Long> visitedIds) {
        if (value == null) {
            content.append(displayName).append(": (empty)\n");
            return;
        }

        for (final ContentExtractor extractor : VALUE_EXTRACTORS) {
            if (extractor.canHandle(value)) {
                extractor.extract(value, language, displayName, content, visitedIds);
                return;
            }
        }

        // Map/Properties fields (e.g. FS section drop zones) are structural containers
        // with no reviewable text content — skip them entirely so they never appear in the AI prompt.
        if (value instanceof Map) {
            return;
        }

        // Fallback: use toString() if it doesn't look like an object reference
        final String str = value.toString();
        if (str.contains("@") && str.matches(".*@[0-9a-f]+$")) {
            content.append(displayName).append(": [").append(value.getClass().getSimpleName()).append("]\n");
        } else {
            content.append(displayName).append(": ").append(str).append("\n");
        }
    }

    /**
     * Appends {@code " | uid=<uid>"} to the given StringBuilder if the element has a UID.
     */
    public static void appendUid(final IDProvider element, final StringBuilder content) {
        if (element.hasUid()) {
            try {
                content.append(" | uid=").append(element.getUid());
            } catch (UnsupportedOperationException ignored) { }
        }
    }

    static Object unwrap(final Object value, final Language language) {
        if (value == null) {
            return null;
        }

        if (value instanceof FormField) {
            return unwrap(((FormField<?>) value).get(), language);
        }

        if (value instanceof EditorValue) {
            return unwrap(((EditorValue<?>) value).get(language), language);
        }

        return value;
    }
}
