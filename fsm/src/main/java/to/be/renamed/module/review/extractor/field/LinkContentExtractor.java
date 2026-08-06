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

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.access.link.Link;

import java.util.Set;

public class LinkContentExtractor implements ContentExtractor {

    private static final Class<?> LOGGER = LinkContentExtractor.class;

    @Override
    public boolean canHandle(final Object value) {
        return value instanceof Link;
    }

    @Override
    public void extract(final Object value, final Language language, final String displayName,
                        final StringBuilder content, final Set<Long> visitedIds) {
        final Link link = (Link) value;
        try {
            final String linkText = link.getText();
            content.append(displayName).append(": ").append(linkText != null ? linkText : "[Link]").append("\n");
        } catch (Exception e) {
            Logging.logInfo("Error getting link text: " + e.getMessage(), LOGGER);
            content.append(displayName).append(": [Link]\n");
        }
    }
}
