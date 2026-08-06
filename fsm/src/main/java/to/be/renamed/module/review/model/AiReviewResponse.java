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

import to.be.renamed.module.review.ai.SchemaEnum;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Data transfer object representing the complete structured AI review response.
 * This is the top-level class passed to {@code JsonSchema.builder().type(AiReviewResponse.class)}
 * for structured output parsing.
 *
 * @since 1.6.0
 */
public class AiReviewResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @SchemaEnum({"Excellent", "Good", "Fair", "Poor", "Critical"})
    private String qualityScore;
    private List<AiReviewIssue> issues;
    private String overallSummary;
    private List<String> recommendations;

    /**
     * No-argument constructor required for structured output deserialization.
     */
    public AiReviewResponse() {
        this.qualityScore = "";
        this.issues = new ArrayList<>();
        this.overallSummary = "";
        this.recommendations = new ArrayList<>();
    }

    public AiReviewResponse(final String qualityScore, final List<AiReviewIssue> issues,
                            final String overallSummary, final List<String> recommendations) {
        this.qualityScore = qualityScore;
        this.issues = issues != null ? new ArrayList<>(issues) : new ArrayList<>();
        this.overallSummary = overallSummary;
        this.recommendations = recommendations != null ? new ArrayList<>(recommendations) : new ArrayList<>();
    }

    public String getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(final String qualityScore) {
        this.qualityScore = qualityScore;
    }

    public List<AiReviewIssue> getIssues() {
        return issues;
    }

    public void setIssues(final List<AiReviewIssue> issues) {
        this.issues = issues != null ? new ArrayList<>(issues) : new ArrayList<>();
    }

    public String getOverallSummary() {
        return overallSummary;
    }

    public void setOverallSummary(final String overallSummary) {
        this.overallSummary = overallSummary;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(final List<String> recommendations) {
        this.recommendations = recommendations != null ? new ArrayList<>(recommendations) : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "AiReviewResponse{" +
                "qualityScore='" + qualityScore + '\'' +
                ", issueCount=" + (issues != null ? issues.size() : 0) +
                ", overallSummary='" + (overallSummary != null ? overallSummary.substring(0, Math.min(50, overallSummary.length())) : "") + '\'' +
                '}';
    }
}
