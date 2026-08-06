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
package to.be.renamed.module.review.language;

import to.be.renamed.module.util.FormEvaluator;

import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves the set of languages to review based on the {@code wf_languages} form selection.
 * An empty selection (or a missing form field) falls back to all project languages.
 * The returned list is ordered by project order (master language first) and deduplicated.
 *
 * @since 1.6.0
 */
public class ReviewLanguageResolver {

    /**
     * Resolves review languages from the workflow session.
     */
    public List<Language> resolve(final WorkflowScriptContext context) {
        return resolve(context, new FormEvaluator(context));
    }

    /**
     * Seam for unit tests — accepts an already-constructed {@link FormEvaluator}.
     */
    List<Language> resolve(final WorkflowScriptContext context, final FormEvaluator formEvaluator) {
        final Language[] formLanguages = formEvaluator.getLanguages();
        final List<Language> projectLanguages = context.getProject().getLanguages();

        if (formLanguages == null || formLanguages.length == 0) {
            return new ArrayList<>(projectLanguages);
        }

        // Build a set of selected languages for fast lookup (by identity)
        final Set<Language> selected = new HashSet<>(Arrays.asList(formLanguages));

        // Iterate project languages (already in project order, master first) and keep only selected
        final List<Language> result = new ArrayList<>();
        for (final Language lang : projectLanguages) {
            if (selected.contains(lang)) {
                result.add(lang);
            }
        }
        return result;
    }
}
