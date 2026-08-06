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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

public class DateContentExtractor implements ContentExtractor {

    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public boolean canHandle(final Object value) {
        return value instanceof Date;
    }

    @Override
    public void extract(final Object value, final Language language, final String displayName,
                        final StringBuilder content, final Set<Long> visitedIds) {
        final String formatted;
        synchronized (FORMAT) {
            formatted = FORMAT.format((Date) value);
        }
        content.append(displayName).append(": ").append(formatted).append("\n");
    }
}
