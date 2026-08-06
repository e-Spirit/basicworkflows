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

import to.be.renamed.module.review.ai.AiResponseParser;
import to.be.renamed.module.review.ai.AiReviewService;
import to.be.renamed.module.review.extractor.FormDataExtractionHelper;
import to.be.renamed.module.review.model.AiReviewIssue;
import to.be.renamed.module.review.model.AiReviewResult;
import to.be.renamed.module.review.prompt.PromptManager;
import to.be.renamed.module.util.FsLocale;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;
import de.espirit.firstspirit.ai.GenerativeAIAgent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orchestrates AI-powered content review by coordinating content extraction,
 * AI communication, and response parsing.
 *
 * @since 1.6.0
 */
class AiContentReviewer {

    private static final Class<?> LOGGER = AiContentReviewer.class;

    private static final int MAX_CONTENT_LENGTH = 16000;

    // Matches element-level headers: [elementType=... | ... | id=N ...]
    private static final Pattern ELEMENT_HEADER_ID = Pattern.compile("\\bid=(\\d+)");
    // Matches field lines: "Label (fieldName): (empty)" or "fieldName: (empty)"
    private static final Pattern EMPTY_FIELD_LINE = Pattern.compile("^(.+): \\(empty\\)$");

    private final WorkflowScriptContext workflowScriptContext;
    private final PromptManager promptManager;
    private final AiReviewService reviewService;

    AiContentReviewer(final WorkflowScriptContext workflowScriptContext,
                      final String promptOperationName) {
        this.workflowScriptContext = workflowScriptContext;
        this.promptManager = new PromptManager(workflowScriptContext, promptOperationName);

        // AI availability is already checked by WfAiReviewExecutable before this constructor is called
        final GenerativeAIAgent aiAgent = workflowScriptContext.requireSpecialist(GenerativeAIAgent.TYPE);
        this.reviewService = new AiReviewService(aiAgent, new AiResponseParser());
    }

    String extractContentForReview(final IDProvider element, final Language language) {
        final StringBuilder content = new StringBuilder();
        final Set<Long> visitedIds = new HashSet<>();

        Logging.logInfo("Content extraction start - Element: " + element.getName() +
                " (ID: " + element.getId() + ", type: " + element.getClass().getSimpleName() + ")", LOGGER);

        try {
            FormDataExtractionHelper.handleValue(element, language, element.getName(), content, visitedIds);
        } catch (Exception e) {
            Logging.logError("Error extracting content from element " + element.getId(), e, LOGGER);
            content.append("Error extracting content: ").append(e.getMessage());
        }

        String result = content.toString();
        Logging.logInfo("Extracted content length: " + result.length() + " characters", LOGGER);
        Logging.logInfo("Extracted content:\n" + result, LOGGER);

        if (result.isEmpty()) {
            Logging.logWarning("No content extracted - result is empty", LOGGER);
        }

        if (result.length() > MAX_CONTENT_LENGTH) {
            Logging.logInfo("Content truncated from " + result.length() + " to " + MAX_CONTENT_LENGTH + " characters", LOGGER);
            result = result.substring(0, MAX_CONTENT_LENGTH) + "\n\n[Content truncated...]";
        }

        return result;
    }

    AiReviewResult sendToAiForReview(final String content, final String reviewType,
                                     final String additionalInstructions,
                                     final Language contentLang) throws Exception {
        final FsLocale fsLocale = new FsLocale(workflowScriptContext);

        // Editor's application UI language (e.g. "German") — determines response language
        final java.util.Locale appLocale = fsLocale.get();
        final String editorLanguage = appLocale != null
                ? appLocale.getDisplayLanguage(java.util.Locale.ENGLISH)
                : null;

        // Content editing language resolved by caller; converted to display name for prompt
        final String contentLanguage = contentLang != null
                ? contentLang.getLocale().getDisplayLanguage(java.util.Locale.ENGLISH)
                : null;

        Logging.logInfo("Editor UI language: " + editorLanguage + ", Content language: " + contentLanguage, LOGGER);

        final String systemPrompt = promptManager.getPrompt(additionalInstructions, editorLanguage, contentLanguage);
        final AiReviewResult result = reviewService.review(systemPrompt, content, reviewType);
        filterHallucinatedEmptyIssues(result, content);
        return result;
    }

    /**
     * Removes AI issues that claim a field is empty when the extracted content shows it is not.
     * The AI occasionally hallucinates "empty" complaints for fields that have actual content.
     * We detect this by parsing the content string we sent to the AI and checking which
     * fields were actually marked "(empty)".
     */
    private void filterHallucinatedEmptyIssues(final AiReviewResult result, final String content) {
        if (result.getIssues().isEmpty()) {
            return;
        }
        final Set<String> emptyFieldKeys = parseEmptyFieldKeys(content);
        final List<AiReviewIssue> filtered = new ArrayList<>(result.getIssues());
        final int before = filtered.size();
        filtered.removeIf(issue -> isHallucinatedEmptyIssue(issue, emptyFieldKeys));
        final int removed = before - filtered.size();
        if (removed > 0) {
            Logging.logInfo("Removed " + removed + " hallucinated empty-field issue(s) from AI response", LOGGER);
            result.setIssues(filtered);
            result.recomputeDerivedFields();
            result.setFullFeedback(rebuildFeedback(result.getFullFeedback(), filtered, result.getQualityScore()));
        }
    }

    /**
     * Returns true when an issue claims a field is empty but the field was not marked "(empty)"
     * in the content we sent to the AI — i.e. the AI hallucinated the empty status.
     * Only issues with problemType "empty_field" are candidates for filtering.
     */
    static boolean isHallucinatedEmptyIssue(final AiReviewIssue issue,
                                             final Set<String> emptyFieldKeys) {
        if (!"empty_field".equals(issue.getProblemType())) {
            return false;
        }
        final String id = issue.getId();
        final String fieldName = issue.getFieldName();
        if (id == null || fieldName == null) {
            return false;
        }
        return !emptyFieldKeys.contains(id.trim() + ":" + fieldName.trim().toLowerCase());
    }

    /**
     * Parses the extracted content string to collect all "id:fieldName" pairs whose value
     * is the literal marker "(empty)". Only considers top-level element headers
     * (lines starting with "[elementType=") as ID boundaries.
     */
    static Set<String> parseEmptyFieldKeys(final String content) {
        final Set<String> keys = new HashSet<>();
        String currentElementId = null;
        for (final String line : content.split("\n")) {
            final String trimmed = line.trim();
            if (trimmed.startsWith("[elementType=")) {
                final Matcher m = ELEMENT_HEADER_ID.matcher(trimmed);
                if (m.find()) {
                    currentElementId = m.group(1);
                }
                continue;
            }
            if (currentElementId != null) {
                final Matcher m = EMPTY_FIELD_LINE.matcher(trimmed);
                if (m.matches()) {
                    final String fieldName = extractFieldName(m.group(1));
                    keys.add(currentElementId + ":" + fieldName.toLowerCase());
                }
            }
        }
        return keys;
    }

    /**
     * Extracts the field identifier from a display-name label.
     * For "Label (fieldName)" format returns "fieldName"; for bare "fieldName" returns as-is.
     */
    static String extractFieldName(final String label) {
        final int parenOpen = label.lastIndexOf('(');
        if (parenOpen >= 0 && label.endsWith(")")) {
            return label.substring(parenOpen + 1, label.length() - 1).trim();
        }
        return label.trim();
    }

    private static String rebuildFeedback(final String existingFeedback,
                                          final List<AiReviewIssue> issues,
                                          final String qualityScore) {
        final int recIdx = (existingFeedback != null ? existingFeedback : "").indexOf("RECOMMENDATIONS:");
        final String recommendations = recIdx >= 0 ? existingFeedback.substring(recIdx) : null;

        final StringBuilder feedback = new StringBuilder();
        feedback.append("QUALITY SCORE: ").append(qualityScore).append("\n\n");
        if (!issues.isEmpty()) {
            feedback.append("ISSUES:\n");
            for (final AiReviewIssue issue : issues) {
                feedback.append("- ").append(issue).append("\n");
            }
            feedback.append("\n");
        }
        if (recommendations != null) {
            feedback.append(recommendations);
        }
        return feedback.toString();
    }
}
