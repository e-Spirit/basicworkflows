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
 * Data transfer object for AI content review results.
 * <p>
 * Holds structured issues ({@link AiReviewIssue}), quality score, and full feedback.
 * Implements {@link Serializable} to enable storage in FirstSpirit workflow session data.
 * </p>
 *
 * @since 1.6.0
 */
public class AiReviewResult implements Serializable {

    private static final long serialVersionUID = 2L;

    private String fullFeedback;
    private List<AiReviewIssue> issues;
    private String qualityScore;
    private boolean hasBlockingIssues;
    private long timestamp;

    public AiReviewResult() {
        this.fullFeedback = "";
        this.issues = new ArrayList<>();
        this.qualityScore = "";
        this.hasBlockingIssues = false;
        this.timestamp = 0;
    }

    public AiReviewResult(final String fullFeedback, final List<AiReviewIssue> issues,
                          final String qualityScore, final boolean hasBlockingIssues,
                          final long timestamp) {
        this.fullFeedback = fullFeedback;
        this.issues = issues != null ? new ArrayList<>(issues) : new ArrayList<>();
        this.qualityScore = qualityScore;
        this.hasBlockingIssues = hasBlockingIssues;
        this.timestamp = timestamp;
    }

    public String getFullFeedback() {
        return fullFeedback;
    }

    public void setFullFeedback(final String fullFeedback) {
        this.fullFeedback = fullFeedback;
    }

    public List<AiReviewIssue> getIssues() {
        return issues;
    }

    public void setIssues(final List<AiReviewIssue> issues) {
        this.issues = issues != null ? new ArrayList<>(issues) : new ArrayList<>();
    }

    public String getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(final String qualityScore) {
        this.qualityScore = qualityScore;
    }

    public boolean hasBlockingIssues() {
        return hasBlockingIssues;
    }

    public void setHasBlockingIssues(final boolean hasBlockingIssues) {
        this.hasBlockingIssues = hasBlockingIssues;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final long timestamp) {
        this.timestamp = timestamp;
    }

    public int getIssueCount() {
        return issues.size();
    }

    public void recomputeDerivedFields() {
        this.hasBlockingIssues = issues.stream()
                .anyMatch(i -> "blocking".equalsIgnoreCase(i.getSeverity()));
        if (issues.isEmpty()) {
            this.qualityScore = QualityScore.EXCELLENT.getAiValue();
            return;
        }
        if (hasBlockingIssues) {
            this.qualityScore = QualityScore.CRITICAL.getAiValue();
            return;
        }
        final long warning = issues.stream()
                .filter(i -> "warning".equalsIgnoreCase(i.getSeverity())).count();
        if (warning > 0) {
            this.qualityScore = QualityScore.POOR.getAiValue();
            return;
        }
        final long info = issues.stream()
                .filter(i -> "info".equalsIgnoreCase(i.getSeverity())).count();
        this.qualityScore = info >= 4 ? QualityScore.FAIR.getAiValue() : QualityScore.GOOD.getAiValue();
    }

    @Override
    public String toString() {
        return "AiReviewResult{" +
                "qualityScore='" + qualityScore + '\'' +
                ", issueCount=" + getIssueCount() +
                ", hasBlockingIssues=" + hasBlockingIssues +
                ", timestamp=" + timestamp +
                '}';
    }
}
