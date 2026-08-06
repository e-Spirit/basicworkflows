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
package to.be.renamed.module.review.ai;

import com.google.gson.Gson;

import to.be.renamed.module.review.model.AiReviewIssue;
import to.be.renamed.module.review.model.AiReviewResponse;
import to.be.renamed.module.review.model.AiReviewResult;

import de.espirit.common.base.Logging;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses structured AI responses into {@link AiReviewResult} objects.
 *
 * @since 1.6.0
 */
public class AiResponseParser {

    private static final Class<?> LOGGER = AiResponseParser.class;

    private static final Gson GSON = new Gson();

    public AiReviewResult parseJsonResponse(final String json) {
        Logging.logInfo("Parsing structured JSON response (length: " + json.length() + " chars)", LOGGER);
        final AiReviewResponse aiResponse = GSON.fromJson(json, AiReviewResponse.class);
        return parseStructuredResponse(aiResponse);
    }

    public AiReviewResult parseStructuredResponse(final AiReviewResponse aiResponse) {
        final List<AiReviewIssue> structuredIssues = new ArrayList<>();

        if (aiResponse.getIssues() != null) {
            Logging.logInfo("Issues count: " + aiResponse.getIssues().size(), LOGGER);
            structuredIssues.addAll(aiResponse.getIssues());
        }

        sortIssues(structuredIssues);
        deduplicateExactIssues(structuredIssues);

        final AiReviewResult result = new AiReviewResult();
        result.setIssues(structuredIssues);
        result.recomputeDerivedFields();

        final StringBuilder fullFeedback = new StringBuilder();
        fullFeedback.append("QUALITY SCORE: ").append(result.getQualityScore()).append("\n\n");
        if (aiResponse.getOverallSummary() != null && !aiResponse.getOverallSummary().isEmpty()) {
            fullFeedback.append(aiResponse.getOverallSummary()).append("\n\n");
        }
        if (!structuredIssues.isEmpty()) {
            fullFeedback.append("ISSUES:\n");
            for (AiReviewIssue issue : structuredIssues) {
                fullFeedback.append("- ").append(issue).append("\n");
            }
            fullFeedback.append("\n");
        }
        if (aiResponse.getRecommendations() != null && !aiResponse.getRecommendations().isEmpty()) {
            fullFeedback.append("RECOMMENDATIONS:\n");
            for (String rec : aiResponse.getRecommendations()) {
                fullFeedback.append("- ").append(rec).append("\n");
            }
        }

        result.setFullFeedback(fullFeedback.toString());
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

    public AiReviewResult createErrorResult(final String errorMessage) {
        final AiReviewIssue errorIssue = new AiReviewIssue();
        errorIssue.setDescription("AI review failed");
        errorIssue.setSeverity("blocking");

        final List<AiReviewIssue> errorIssues = new ArrayList<>();
        errorIssues.add(errorIssue);

        return new AiReviewResult(
                errorMessage,
                errorIssues,
                "Error",
                false,
                System.currentTimeMillis()
        );
    }

    private static void deduplicateExactIssues(final List<AiReviewIssue> issues) {
        final java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        issues.removeIf(issue -> {
            final String key = normalizeDedup(issue.getId()) + "|" +
                               normalizeDedup(issue.getCardId()) + "|" +
                               normalizeDedup(issue.getFieldName()) + "|" +
                               normalizeDedup(issue.getDescription());
            return !seen.add(key);
        });
    }

    private static String normalizeDedup(final String s) {
        return s != null ? s.trim().toLowerCase() : "";
    }

    private static void sortIssues(final List<AiReviewIssue> issues) {
        issues.sort((a, b) -> {
            final int sevCmp = Integer.compare(severityRank(a.getSeverity()), severityRank(b.getSeverity()));
            if (sevCmp != 0) {
                return sevCmp;
            }
            final int idCmp = compareIds(
                    a.getId() != null ? a.getId() : "",
                    b.getId() != null ? b.getId() : "");
            if (idCmp != 0) {
                return idCmp;
            }
            final String fnA = a.getFieldName() != null ? a.getFieldName() : "";
            final String fnB = b.getFieldName() != null ? b.getFieldName() : "";
            final int fnCmp = fnA.compareToIgnoreCase(fnB);
            if (fnCmp != 0) {
                return fnCmp;
            }
            final String cidA = a.getCardId() != null ? a.getCardId() : "";
            final String cidB = b.getCardId() != null ? b.getCardId() : "";
            return cidA.compareToIgnoreCase(cidB);
        });
    }

    private static int severityRank(final String severity) {
        if (severity == null) {
            return 3;
        }
        switch (severity.toLowerCase()) {
            case "blocking": return 0;
            case "warning":  return 1;
            case "info":     return 2;
            default:         return 3;
        }
    }

    private static int compareIds(final String a, final String b) {
        try {
            return Long.compare(Long.parseLong(a.trim()), Long.parseLong(b.trim()));
        } catch (NumberFormatException e) {
            return a.compareTo(b);
        }
    }

}
