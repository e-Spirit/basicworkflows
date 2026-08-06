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
import de.espirit.firstspirit.access.editor.value.Option;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles both single Option (combobox, radiobutton) and Set&lt;Option&gt; (checkbox) values.
 */
public class OptionContentExtractor implements ContentExtractor {

    @Override
    public boolean canHandle(final Object value) {
        if (value instanceof Option) {
            return true;
        }
        if (value instanceof Set) {
            final Set<?> set = (Set<?>) value;
            return !set.isEmpty() && set.iterator().next() instanceof Option;
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void extract(final Object value, final Language language, final String displayName,
                        final StringBuilder content, final Set<Long> visitedIds) {
        final String text;
        if (value instanceof Option) {
            text = extractOption((Option) value);
        } else {
            final Set<Option> options = (Set<Option>) value;
            if (options.isEmpty()) {
                content.append(displayName).append(": (empty)\n");
                return;
            }
            text = options.stream()
                    .map(this::extractOption)
                    .collect(Collectors.joining(", "));
        }
        content.append(displayName).append(": ").append(text).append("\n");
    }

    private String extractOption(final Option option) {
        final String key = option.getKey();
        return key != null ? key : "";
    }
}
