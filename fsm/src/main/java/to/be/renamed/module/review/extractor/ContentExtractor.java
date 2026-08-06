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

import de.espirit.firstspirit.access.Language;

import java.util.Set;

/**
 * Strategy interface for extracting a text representation from a form field value.
 * Simple extractors append "displayName: value\n". Complex extractors (Catalog, Dataset, etc.)
 * may recurse into nested FormData via FormDataExtractionHelper.
 */
public interface ContentExtractor {

    boolean canHandle(Object value);

    void extract(Object value, Language language, String displayName, StringBuilder content, Set<Long> visitedIds);
}
