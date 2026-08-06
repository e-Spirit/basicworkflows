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
package to.be.renamed.module.review.extractor.field;

import to.be.renamed.module.review.extractor.ContentExtractor;

import de.espirit.firstspirit.access.Language;

import java.util.Set;

public class StringContentExtractor implements ContentExtractor {

    @Override
    public boolean canHandle(final Object value) {
        return value instanceof String;
    }

    @Override
    public void extract(final Object value, final Language language, final String displayName,
                        final StringBuilder content, final Set<Long> visitedIds) {
        // Normalize embedded newlines so multi-line string values don't corrupt the
        // "label: value\n" format that the AI relies on for field parsing.
        final String text = ((String) value).replaceAll("\\r?\\n", " ").trim();
        if (!text.isEmpty()) {
            content.append(displayName).append(": ").append(text).append("\n");
        } else {
            content.append(displayName).append(": (empty)\n");
        }
    }
}
