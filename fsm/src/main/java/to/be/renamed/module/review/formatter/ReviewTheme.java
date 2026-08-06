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

import to.be.renamed.module.review.model.QualityScore;

import java.util.ResourceBundle;

/**
 * Single source of truth for the unified AI-review design tokens and HTML badge fragments.
 * Consumed by {@link ReviewResultFormatter} and
 * {@code AiReviewDataRenderer} so both HTML producers share one palette.
 *
 * @since 1.6.0
 */
public final class ReviewTheme {

    // --- Token constants ---

    public static final String FONT_STACK = "'Open Sans', Arial, Helvetica, sans-serif";
    public static final String HEADER_COLOR = "#21213a";
    public static final String TEXT_COLOR = "#333";
    public static final String MUTED_COLOR = "#5a6472";
    public static final String LIST_TEXT_COLOR = "#3d4550";
    public static final String SUGGESTION_COLOR = "#1e8449";

    public static final String CARD_BG = "#f6f7f9";
    public static final String CARD_BORDER = "#e8a84a";
    public static final String SEPARATOR_COLOR = "#dde1e5";
    public static final String SUCCESS_BG = "#eaf6ee";
    public static final String SUCCESS_BORDER = "#27ae60";
    public static final String SUCCESS_TEXT = "#1e6b32";

    public static final String DANGER_BG = "#fde8e8";
    public static final String DANGER_BORDER = "#c0392b";
    public static final String DANGER_TEXT = "#c0392b";

    // Tab-strip tokens for CSS-only multi-language tab navigation
    public static final String TAB_ACTIVE_BG = "#fff";
    public static final String TAB_ACTIVE_BORDER = SEPARATOR_COLOR;
    public static final String TAB_ACTIVE_COLOR = HEADER_COLOR;
    public static final String TAB_INACTIVE_COLOR = MUTED_COLOR;
    public static final String TAB_FAILING_DOT_COLOR = DANGER_BORDER;

    // ContentCreator native button palette (see reference screenshot).
    public static final String BUTTON_PRIMARY_BG = "#3e89be";
    public static final String BUTTON_CANCEL_BG = "#e6f1f7";
    public static final String BUTTON_CANCEL_BORDER = "#b8d5e8";
    public static final String BUTTON_CANCEL_TEXT = "#3e89be";

    private static final String BADGE_SHAPE =
            "padding: 1px 6px; border-radius: 2px; font-size: 11px; font-weight: 600; letter-spacing: 0.03em;";

    private ReviewTheme() {
    }

    // --- Severity color lookups (also used by the SA icon painter) ---

    public static String severityFg(final String severity) {
        if (severity == null) {
            return "#1a5276";
        }
        return switch (severity.toLowerCase()) {
            case "blocking" -> "#c0392b";
            case "warning"  -> "#8a6200";
            default         -> "#1a5276";
        };
    }

    public static String severityBg(final String severity) {
        if (severity == null) {
            return "#ddeef6";
        }
        return switch (severity.toLowerCase()) {
            case "blocking" -> "#fde8e8";
            case "warning"  -> "#fef3dc";
            default         -> "#ddeef6";
        };
    }

    // --- Badge HTML fragments ---

    /**
     * Returns a severity badge HTML span using the unified palette and badge shape.
     * Unknown severities fall back to info styling.
     */
    public static String severityBadge(final String severity) {
        final String bg = severityBg(severity);
        final String fg = severityFg(severity);
        final String label = esc(capitalize(severity));
        return "<span style='background: " + bg + "; color: " + fg + "; " + BADGE_SHAPE + "'>"
                + label + "</span>";
    }

    /**
     * Returns an element-type badge HTML span (type label only).
     */
    public static String typeBadge(final String elementType) {
        return "<span style='background: #e8f0fb; color: #2471a3; " + BADGE_SHAPE + "'>"
                + esc(capitalize(elementType)) + "</span>";
    }

    /**
     * Returns an element-type badge HTML span including the element name after a colon.
     * Used by producers that show {@code "Type: Name"} inside one badge.
     */
    public static String typeBadge(final String elementType, final String elementName) {
        final String content = esc(capitalize(elementType)) + ": " + esc(elementName != null ? elementName : "");
        return "<span style='background: #e8f0fb; color: #2471a3; " + BADGE_SHAPE + "'>" + content + "</span>";
    }

    /**
     * Returns a field badge HTML span with the localized "Field:" prefix from the bundle.
     */
    public static String fieldBadge(final String displayField, final ResourceBundle bundle) {
        String prefix;
        try {
            prefix = bundle.getString("reviewFieldLabel");
        } catch (java.util.MissingResourceException e) {
            prefix = "Field";
        }
        return "<span style='background: #f2edf8; color: #6c3483; " + BADGE_SHAPE + "'>"
                + prefix + ": " + esc(displayField) + "</span>";
    }

    /**
     * Returns a language badge HTML span with the localized "Language:" prefix from the bundle.
     */
    public static String languageBadge(final String abbreviation, final ResourceBundle bundle) {
        String prefix;
        try {
            prefix = bundle.getString("reviewLanguageLabel");
        } catch (java.util.MissingResourceException e) {
            prefix = "Language";
        }
        return "<span style='background: #e8f0fb; color: #2471a3; " + BADGE_SHAPE + "'>"
                + prefix + ": " + esc(abbreviation) + "</span>";
    }

    /**
     * Returns the pass/fail status banner HTML. Uses the success palette when passed,
     * the danger palette when failed. Label comes from the bundle with an English fallback.
     */
    public static String statusBanner(final boolean passed, final ResourceBundle bundle) {
        final String bg = passed ? SUCCESS_BG : DANGER_BG;
        final String border = passed ? SUCCESS_BORDER : DANGER_BORDER;
        final String fg = passed ? SUCCESS_TEXT : DANGER_TEXT;
        final String key = passed ? "reviewStatusPassed" : "reviewStatusFailed";
        String label;
        try {
            label = bundle.getString(key);
        } catch (java.util.MissingResourceException e) {
            label = passed ? "Review passed" : "Review failed";
        }
        return "<div style='background: " + bg + "; color: " + fg
                + "; padding: 10px 12px; border-left: 4px solid " + border
                + "; margin-bottom: 14px; border-radius: 0 3px 3px 0; font-size: 14px; font-weight: 700;'>"
                + esc(label) + "</div>";
    }

    /**
     * Returns a score HTML span, resolving via {@link QualityScore}.
     * Falls back to the raw value with {@code #c0392b} for unknown scores.
     * Returns an empty string for null/blank input.
     */
    public static String scoreSpan(final String aiScore, final ResourceBundle bundle) {
        if (aiScore == null || aiScore.isEmpty()) {
            return "";
        }
        final QualityScore score = QualityScore.fromAiValue(aiScore);
        final String color = score != null ? score.getColor() : "#c0392b";
        final String label = score != null ? score.getDisplayName(bundle) : esc(aiScore);
        return "<span style='color: " + color + "; font-weight: 700; font-size: 13px;'>"
                + label + "</span>";
    }

    // --- Internal helpers ---

    public static String capitalize(final String text) {
        if (text == null || text.isEmpty()) {
            return text != null ? text : "";
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    /**
     * Escapes HTML special characters to prevent injection into badge and detail HTML fragments.
     * Applied to all AI-generated or user-controlled string values before concatenation.
     */
    public static String esc(final String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
