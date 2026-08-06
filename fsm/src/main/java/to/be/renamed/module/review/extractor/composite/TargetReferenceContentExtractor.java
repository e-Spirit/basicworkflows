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

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.access.editor.value.TargetReference;
import de.espirit.firstspirit.access.store.IDProvider;

import java.util.Set;

public class TargetReferenceContentExtractor implements ContentExtractor {

    private static final Class<?> LOGGER = TargetReferenceContentExtractor.class;

    @Override
    public boolean canHandle(final Object value) {
        return value instanceof TargetReference;
    }

    @Override
    public void extract(final Object value, final Language language, final String displayName,
                        final StringBuilder content, final Set<Long> visitedIds) {
        final TargetReference targetRef = (TargetReference) value;
        try {
            final IDProvider target = targetRef.get();
            if (target == null) {
                content.append(displayName).append(": (empty reference)\n");
                return;
            }

            FormDataExtractionHelper.handleValue(target, language, displayName, content, visitedIds);
        } catch (Exception e) {
            Logging.logInfo("Could not resolve TargetReference: " + e.getMessage(), LOGGER);
            content.append(displayName).append(": [Unresolved Reference]\n");
        }
    }
}
