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
package to.be.renamed.module.review.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated AI review results across all release languages.
 * Serialized as JSON via Gson and stored in the workflow session under
 * {@code WorkflowConstants.WF_AI_REVIEW_RESULTS}.
 *
 * @since 1.6.0
 */
public class MultiLanguageReviewResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<LanguageReviewResult> languageResults = new ArrayList<>();
    private boolean passed = false;
    private boolean hasBlockingIssues = false;
    /** AI value string of the worst score seen, or {@code null} when no results have been added. */
    private String worstScore = null;

    public MultiLanguageReviewResult() {
    }

    /**
     * Adds a per-language result and recomputes all aggregate fields.
     */
    public void addLanguageResult(final LanguageReviewResult result) {
        languageResults.add(result);
        if (result.getResult() != null && result.getResult().hasBlockingIssues()) {
            hasBlockingIssues = true;
        }
        worstScore = pickWorse(worstScore, scoreFor(result));
        passed = computePassed();
    }

    private boolean computePassed() {
        if (languageResults.isEmpty()) {
            return false;
        }
        for (final LanguageReviewResult r : languageResults) {
            if (r.hasError() || !r.isPassed()) {
                return false;
            }
        }
        return true;
    }

    private static String scoreFor(final LanguageReviewResult r) {
        if (r.hasError() || r.getResult() == null) {
            return QualityScore.CRITICAL.getAiValue();
        }
        final QualityScore parsed = QualityScore.fromAiValue(r.getResult().getQualityScore());
        return parsed != null ? parsed.getAiValue() : QualityScore.CRITICAL.getAiValue();
    }

    private static String pickWorse(final String current, final String candidate) {
        if (current == null) {
            return candidate;
        }
        if (candidate == null) {
            return current;
        }
        final QualityScore c = QualityScore.fromAiValue(current);
        final QualityScore d = QualityScore.fromAiValue(candidate);
        final int cOrd = c != null ? c.ordinal() : QualityScore.CRITICAL.ordinal();
        final int dOrd = d != null ? d.ordinal() : QualityScore.CRITICAL.ordinal();
        return dOrd > cOrd ? candidate : current;
    }

    public List<LanguageReviewResult> getLanguageResults() {
        return languageResults;
    }

    public void setLanguageResults(final List<LanguageReviewResult> languageResults) {
        this.languageResults = languageResults != null ? languageResults : new ArrayList<>();
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(final boolean passed) {
        this.passed = passed;
    }

    public boolean isHasBlockingIssues() {
        return hasBlockingIssues;
    }

    public void setHasBlockingIssues(final boolean hasBlockingIssues) {
        this.hasBlockingIssues = hasBlockingIssues;
    }

    public String getWorstScoreValue() {
        return worstScore;
    }

    public void setWorstScore(final String worstScore) {
        this.worstScore = worstScore;
    }

    /**
     * Returns the worst {@link QualityScore} across all reviewed languages, or {@code null} if no
     * results have been added yet.
     */
    public QualityScore getWorstScore() {
        if (worstScore == null) {
            return null;
        }
        final QualityScore parsed = QualityScore.fromAiValue(worstScore);
        return parsed != null ? parsed : QualityScore.CRITICAL;
    }

    /** Returns the total number of issues across all languages that returned a result. */
    public int getTotalIssueCount() {
        int total = 0;
        for (final LanguageReviewResult r : languageResults) {
            if (r.getResult() != null) {
                total += r.getResult().getIssueCount();
            }
        }
        return total;
    }

    @Override
    public String toString() {
        return "MultiLanguageReviewResult{" +
                "languages=" + languageResults.size() +
                ", passed=" + passed +
                ", worstScore='" + worstScore + '\'' +
                ", hasBlockingIssues=" + hasBlockingIssues +
                '}';
    }
}
