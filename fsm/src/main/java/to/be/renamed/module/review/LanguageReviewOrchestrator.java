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
package to.be.renamed.module.review;

import to.be.renamed.module.review.model.AiReviewResult;
import to.be.renamed.module.review.model.LanguageReviewResult;
import to.be.renamed.module.review.model.MultiLanguageReviewResult;
import to.be.renamed.module.review.model.QualityScore;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.access.store.IDProvider;

import java.util.List;

/**
 * Loops over each release language, calls {@link AiContentReviewer} once per language, and
 * aggregates the per-language results into a {@link MultiLanguageReviewResult}.
 * An exception for one language is caught and recorded as an error result; the remaining
 * languages continue to be reviewed.
 *
 * @since 1.6.0
 */
class LanguageReviewOrchestrator {

    private static final Class<?> LOGGER = LanguageReviewOrchestrator.class;

    private final AiContentReviewer reviewer;

    LanguageReviewOrchestrator(final AiContentReviewer reviewer) {
        this.reviewer = reviewer;
    }

    /**
     * Reviews {@code element} in each of the given {@code languages} and returns the aggregated
     * result.
     *
     * @param element          the workflow element whose content is reviewed
     * @param languages        ordered list of languages to review (master first)
     * @param reviewType       review type passed to the AI (e.g. {@code "quality"})
     * @param instructions     optional additional instructions; may be {@code null}
     * @param minScore         minimum quality score required for a pass; may be {@code null}
     * @param progressListener optional callback for per-language overlay updates; may be
     *                         {@code null}
     * @return aggregated result across all languages
     */
    MultiLanguageReviewResult reviewAllLanguages(final IDProvider element,
                                                 final List<Language> languages,
                                                 final String reviewType,
                                                 final String instructions,
                                                 final QualityScore minScore,
                                                 final ProgressListener progressListener) {
        final MultiLanguageReviewResult aggregate = new MultiLanguageReviewResult();
        final int total = languages.size();

        for (int i = 0; i < total; i++) {
            final Language language = languages.get(i);
            final int oneBased = i + 1;

            if (progressListener != null) {
                progressListener.onLanguageStart(language, oneBased, total);
            }

            final LanguageReviewResult langResult = reviewLanguage(
                    element, language, reviewType, instructions, minScore);
            aggregate.addLanguageResult(langResult);
        }

        return aggregate;
    }

    private LanguageReviewResult reviewLanguage(final IDProvider element,
                                                final Language language,
                                                final String reviewType,
                                                final String instructions,
                                                final QualityScore minScore) {
        final String abbr = language.getAbbreviation();
        final String displayName = language.getName();

        try {
            final String content = reviewer.extractContentForReview(element, language);
            final AiReviewResult result = reviewer.sendToAiForReview(content, reviewType, instructions, language);
            final boolean passed = WfAiReviewExecutable.determineReviewPassed(
                    QualityScore.fromAiValue(result.getQualityScore()), minScore, result.hasBlockingIssues());
            return new LanguageReviewResult(abbr, displayName, result, passed, null);
        } catch (Exception e) {
            Logging.logError("AI review failed for language " + abbr + ": " + e.getMessage(), e, LOGGER);
            final String errorMsg = e.getMessage() != null && !e.getMessage().isEmpty()
                    ? e.getMessage()
                    : e.getClass().getSimpleName();
            return new LanguageReviewResult(abbr, displayName, null, false, errorMsg);
        }
    }
}
