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

import java.util.ResourceBundle;

/**
 * Quality score tiers returned by the AI review.
 * Ordered from best to worst — ordinal is used for comparison.
 *
 * @since 1.6.0
 */
public enum QualityScore {

    EXCELLENT("Excellent", "scoreExcellent", "#28a745"),
    GOOD("Good", "scoreGood", "#28a745"),
    FAIR("Fair", "scoreFair", "#ffc107"),
    POOR("Poor", "scorePoor", "#dc3545"),
    CRITICAL("Critical", "scoreCritical", "#dc3545");

    private final String aiValue;
    private final String bundleKey;
    private final String color;

    QualityScore(final String aiValue, final String bundleKey, final String color) {
        this.aiValue = aiValue;
        this.bundleKey = bundleKey;
        this.color = color;
    }

    /** The value the AI returns (always English). */
    public String getAiValue() {
        return aiValue;
    }

    /** The color to use when displaying this score. */
    public String getColor() {
        return color;
    }

    /** Returns the localized display name from the resource bundle. */
    public String getDisplayName(final ResourceBundle bundle) {
        try {
            return bundle.getString(bundleKey);
        } catch (java.util.MissingResourceException e) {
            return aiValue;
        }
    }

    /** Returns true if this score meets or exceeds the given minimum. */
    public boolean meetsMinimum(final QualityScore minimum) {
        return this.ordinal() <= minimum.ordinal();
    }

    /** Parses an AI response string into a QualityScore, or null if unknown. */
    public static QualityScore fromAiValue(final String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (QualityScore score : values()) {
            if (score.aiValue.equalsIgnoreCase(value.trim())) {
                return score;
            }
        }
        return null;
    }

    /** Returns a comma-separated list of all valid AI values (for use in prompts). */
    public static String allAiValues() {
        final StringBuilder sb = new StringBuilder();
        for (QualityScore score : values()) {
            if (!sb.isEmpty()) {
                sb.append("\", \"");
            }
            sb.append(score.aiValue);
        }
        return "\"" + sb + "\"";
    }
}
