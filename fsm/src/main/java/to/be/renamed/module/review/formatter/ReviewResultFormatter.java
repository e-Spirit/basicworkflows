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
package to.be.renamed.module.review.formatter;

import to.be.renamed.module.review.model.AiReviewIssue;
import to.be.renamed.module.review.model.AiReviewResult;
import to.be.renamed.module.review.model.LanguageReviewResult;
import to.be.renamed.module.review.model.MultiLanguageReviewResult;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Formats AI review results as HTML for display in workflow dialogs.
 * Works directly with structured {@link AiReviewIssue} objects.
 * All design tokens and badge HTML are delegated to {@link ReviewTheme}.
 *
 * @since 1.6.0
 */
public class ReviewResultFormatter {

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Multi-language dialog view: status banner + CSS-only language tabs (2+ languages)
     * or a plain single-section body (1 language).
     */
    public String format(final MultiLanguageReviewResult multiResult, final ResourceBundle bundle) {
        final StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: ").append(ReviewTheme.FONT_STACK)
                .append("; padding: 10px; color: ").append(ReviewTheme.TEXT_COLOR).append(";'>");

        html.append(ReviewTheme.statusBanner(multiResult.isPassed(), bundle));

        html.append(buildTabStrip(multiResult.getLanguageResults(), bundle));

        html.append("</body></html>");
        return html.toString();
    }

    /**
     * SiteArchitect stacked view: status banner followed by each language section with a heading.
     * No radio inputs or {@code <style>} block — safe for plain HTML dialogs.
     */
    public String formatStacked(final MultiLanguageReviewResult multiResult, final ResourceBundle bundle) {
        final StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: ").append(ReviewTheme.FONT_STACK)
                .append("; padding: 10px; color: ").append(ReviewTheme.TEXT_COLOR).append(";'>");

        html.append(ReviewTheme.statusBanner(multiResult.isPassed(), bundle));

        for (final LanguageReviewResult lang : multiResult.getLanguageResults()) {
            html.append("<div style='margin: 16px 0 8px 0; padding-bottom: 4px; border-bottom: 2px solid ")
                    .append(ReviewTheme.SEPARATOR_COLOR).append(";'>");
            html.append("<strong style='font-size: 14px; color: ").append(ReviewTheme.HEADER_COLOR).append(";'>")
                    .append(ReviewTheme.esc(lang.getLanguageDisplayName()))
                    .append("</strong> ");
            html.append(ReviewTheme.languageBadge(lang.getLanguageAbbreviation(), bundle));
            html.append("</div>");
            html.append(formatLanguageSection(lang, bundle));
        }

        html.append("</body></html>");
        return html.toString();
    }

    /**
     * Backward-compatibility method kept for {@code WfShowAiReviewResultsExecutable} until Phase 6.
     * Delegates to {@link #format(MultiLanguageReviewResult, ResourceBundle)} via a single-language wrapper.
     */
    public String format(final AiReviewResult result, final boolean passed, final ResourceBundle bundle) {
        final MultiLanguageReviewResult m = new MultiLanguageReviewResult();
        m.addLanguageResult(new LanguageReviewResult("EN", "English", result, passed, null));
        return format(m, bundle);
    }

    // =========================================================================
    // Package-private section renderer (used by format and formatStacked)
    // =========================================================================

    /**
     * Renders the content section for one {@link LanguageReviewResult}.
     * If the language has an error, renders an error message instead of score/issues/recommendations.
     */
    String formatLanguageSection(final LanguageReviewResult lang, final ResourceBundle bundle) {
        if (lang.hasError()) {
            final String errorMessage = MessageFormat.format(
                    bundle.getString("reviewLanguageFailed"), lang.getErrorMessage());
            return "<div style='background: " + ReviewTheme.DANGER_BG
                    + "; color: " + ReviewTheme.DANGER_TEXT
                    + "; padding: 10px 12px; border-left: 3px solid " + ReviewTheme.DANGER_BORDER
                    + "; margin: 10px 0; border-radius: 0 3px 3px 0; font-size: 13px;'>"
                    + ReviewTheme.esc(errorMessage) + "</div>";
        }

        final AiReviewResult result = lang.getResult();
        final String score = (result != null) ? result.getQualityScore() : null;
        final List<AiReviewIssue> issues = (result != null) ? result.getIssues() : Collections.emptyList();
        final String fullFeedback = (result != null) ? result.getFullFeedback() : null;

        final StringBuilder sb = new StringBuilder();

        // Quality score
        sb.append("<div style='margin-bottom: 14px;'>");
        sb.append("<strong style='font-size: 13px; color: ").append(ReviewTheme.HEADER_COLOR).append(";'>")
                .append(bundle.getString("reviewContentQuality")).append(":</strong> ");
        sb.append(ReviewTheme.scoreSpan(score, bundle));
        sb.append("</div>");

        sb.append("<hr style='border: none; border-top: 1px solid ").append(ReviewTheme.SEPARATOR_COLOR).append("; margin: 10px 0;'>");

        // Issues section heading
        sb.append("<div style='margin: 12px 0 8px 0;'>");
        sb.append("<strong style='font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em; color: ")
                .append(ReviewTheme.MUTED_COLOR).append(";'>")
                .append(bundle.getString("reviewIssuesFound")).append("</strong>");
        sb.append("</div>");

        if (issues != null && !issues.isEmpty()) {
            sb.append("<div style='background: ").append(ReviewTheme.CARD_BG)
                    .append("; padding: 10px 12px; border-left: 3px solid ").append(ReviewTheme.CARD_BORDER)
                    .append("; margin-bottom: 14px; border-radius: 0 3px 3px 0;'>");
            sb.append("<ol style='margin: 0; padding-left: 20px; line-height: 1.6;'>");

            for (final AiReviewIssue issue : issues) {
                sb.append("<li style='margin-bottom: 8px; color: ").append(ReviewTheme.LIST_TEXT_COLOR).append(";'>");

                final String severity = issue.getSeverity();
                if (severity != null && !severity.isEmpty()) {
                    sb.append(ReviewTheme.severityBadge(severity)).append(" ");
                }

                final String elementType = issue.getElementType();
                final String name = issue.getName();
                if (elementType != null && !elementType.isEmpty()) {
                    sb.append(ReviewTheme.typeBadge(elementType, name)).append(" ");
                }

                final String fieldLabel = issue.getFieldLabel();
                final String fieldName = issue.getFieldName();
                final String displayField = (fieldLabel != null && !fieldLabel.isEmpty()) ? fieldLabel : fieldName;
                if (displayField != null && !displayField.isEmpty()) {
                    sb.append(ReviewTheme.fieldBadge(displayField, bundle)).append(" ");
                }

                sb.append("- ").append(ReviewTheme.esc(issue.getDescription()));

                final String suggestion = issue.getSuggestion();
                if (suggestion != null && !suggestion.isEmpty()) {
                    sb.append("<br/><em style='color: ").append(ReviewTheme.SUGGESTION_COLOR).append("; font-style: italic;'>")
                            .append(bundle.getString("reviewSuggestion")).append(": ")
                            .append(ReviewTheme.esc(suggestion)).append("</em>");
                }

                sb.append("</li>");
            }

            sb.append("</ol>");
            sb.append("</div>");
        } else {
            sb.append("<div style='background: ").append(ReviewTheme.SUCCESS_BG)
                    .append("; color: ").append(ReviewTheme.SUCCESS_TEXT)
                    .append("; padding: 10px 12px; border-left: 3px solid ").append(ReviewTheme.SUCCESS_BORDER)
                    .append("; margin: 10px 0; border-radius: 0 3px 3px 0; font-size: 13px;'>");
            sb.append(bundle.getString("reviewNoIssues"));
            sb.append("</div>");
        }

        if (fullFeedback != null && fullFeedback.contains("RECOMMENDATIONS:")) {
            final int recStart = fullFeedback.indexOf("RECOMMENDATIONS:") + "RECOMMENDATIONS:".length();
            final String recommendations = fullFeedback.substring(recStart).trim();

            if (!recommendations.isEmpty()) {
                sb.append("<div style='margin: 12px 0 8px 0;'>");
                sb.append("<strong style='font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em; color: ")
                        .append(ReviewTheme.MUTED_COLOR).append(";'>")
                        .append(bundle.getString("reviewRecommendations")).append("</strong>");
                sb.append("</div>");

                sb.append("<div style='background: ").append(ReviewTheme.SUCCESS_BG)
                        .append("; padding: 10px 12px; border-left: 3px solid ").append(ReviewTheme.SUCCESS_BORDER)
                        .append("; margin-bottom: 14px; border-radius: 0 3px 3px 0;'>");
                sb.append("<div style='color: ").append(ReviewTheme.SUCCESS_TEXT).append("; line-height: 1.6; font-size: 13px;'>");

                final String[] recLines = recommendations.split("\n");
                boolean inList = false;

                for (final String line : recLines) {
                    final String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    if (trimmed.startsWith("-") || trimmed.startsWith("*")) {
                        if (!inList) {
                            sb.append("<ul style='margin: 5px 0; padding-left: 20px;'>");
                            inList = true;
                        }
                        sb.append("<li style='margin-bottom: 6px;'>")
                                .append(ReviewTheme.esc(trimmed.substring(1).trim()))
                                .append("</li>");
                    } else {
                        if (inList) {
                            sb.append("</ul>");
                            inList = false;
                        }
                        sb.append("<p style='margin: 8px 0;'>").append(ReviewTheme.esc(trimmed)).append("</p>");
                    }
                }

                if (inList) {
                    sb.append("</ul>");
                }

                sb.append("</div>");
                sb.append("</div>");
            }
        }

        return sb.toString();
    }

    // =========================================================================
    // Private tab-strip builder
    // =========================================================================

    private String buildTabStrip(final List<LanguageReviewResult> langs, final ResourceBundle bundle) {
        // D10: initially checked = first failing language, otherwise index 0
        int checkedIndex = 0;
        for (int i = 0; i < langs.size(); i++) {
            final LanguageReviewResult lang = langs.get(i);
            if (lang.hasError() || !lang.isPassed()) {
                checkedIndex = i;
                break;
            }
        }

        final StringBuilder sb = new StringBuilder();

        // CSS-only tab selectors: <input> siblings before the panels div use the ~ combinator
        sb.append("<style>");
        sb.append(".bw-ai-lang-radio{display:none}");
        sb.append(".bw-ai-lang-panel{display:none}");
        sb.append(".bw-ai-lang-panels{margin-top:12px}");
        sb.append(".bw-ai-lang-label{cursor:pointer;padding:6px 14px;margin:0 4px 0 0;font-size:12px;font-weight:600;color:")
                .append(ReviewTheme.TAB_INACTIVE_COLOR)
                .append(";border:1px solid transparent;border-bottom:none;border-radius:3px 3px 0 0;background:")
                .append(ReviewTheme.CARD_BG).append("}");
        for (int i = 0; i < langs.size(); i++) {
            sb.append("#bw-ai-lang-r").append(i)
                    .append(":checked ~ .bw-ai-lang-panels .bw-ai-lang-panel-").append(i).append("{display:block}");
            sb.append("#bw-ai-lang-r").append(i)
                    .append(":checked ~ .bw-ai-lang-tabs label[for='bw-ai-lang-r").append(i).append("']{background:")
                    .append(ReviewTheme.TAB_ACTIVE_BG).append(";color:").append(ReviewTheme.TAB_ACTIVE_COLOR)
                    .append(";border-color:").append(ReviewTheme.TAB_ACTIVE_BORDER).append("}");
        }
        sb.append("</style>");

        sb.append("<div class='bw-ai-lang-wrap'>");

        // Radio inputs must appear before the tabs and panels divs so the ~ sibling combinator works
        for (int i = 0; i < langs.size(); i++) {
            sb.append("<input type='radio' name='bw-ai-lang' id='bw-ai-lang-r").append(i)
                    .append("' class='bw-ai-lang-radio'");
            if (i == checkedIndex) {
                sb.append(" checked");
            }
            sb.append(">");
        }

        // Tab strip
        sb.append("<div class='bw-ai-lang-tabs'>");
        for (int i = 0; i < langs.size(); i++) {
            final LanguageReviewResult lang = langs.get(i);
            final int count = (lang.getResult() != null) ? lang.getResult().getIssueCount() : 0;
            sb.append("<label for='bw-ai-lang-r").append(i).append("' class='bw-ai-lang-label'>")
                    .append(ReviewTheme.esc(lang.getLanguageAbbreviation()))
                    .append(" (").append(count).append(")")
                    .append("</label>");
        }
        sb.append("</div>");

        // Panels
        sb.append("<div class='bw-ai-lang-panels'>");
        for (int i = 0; i < langs.size(); i++) {
            sb.append("<div class='bw-ai-lang-panel bw-ai-lang-panel-").append(i).append("'>");
            sb.append(formatLanguageSection(langs.get(i), bundle));
            sb.append("</div>");
        }
        sb.append("</div>");

        sb.append("</div>");
        return sb.toString();
    }
}
