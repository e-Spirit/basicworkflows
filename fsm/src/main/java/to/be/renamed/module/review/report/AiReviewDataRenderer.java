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
package to.be.renamed.module.review.report;

import to.be.renamed.module.review.formatter.ReviewTheme;
import to.be.renamed.module.review.model.AiReviewIssue;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.BaseContext;
import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.access.editor.EditorValue;
import de.espirit.firstspirit.access.store.pagestore.DataProvider;
import de.espirit.firstspirit.access.store.templatestore.gom.GomFormElement;
import de.espirit.firstspirit.agency.UIAgent;
import de.espirit.firstspirit.client.access.editor.lists.Catalog;
import de.espirit.firstspirit.client.plugin.report.DataRenderer;
import de.espirit.firstspirit.client.plugin.report.Image;
import de.espirit.firstspirit.forms.FormData;
import de.espirit.firstspirit.forms.FormField;
import de.espirit.firstspirit.webedit.WebeditUiAgent;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;

/**
 * Renders AI review report entries with severity badges, field info, and descriptions.
 * Design tokens and badge HTML delegate to {@link ReviewTheme} so all producers share one palette.
 *
 * @since 1.6.0
 */
public class AiReviewDataRenderer implements DataRenderer<AiReviewReportEntry> {

    private final BaseContext context;
    private final ResourceBundle bundle;

    AiReviewDataRenderer(final BaseContext context, final ResourceBundle bundle) {
        this.context = context;
        this.bundle = bundle;
    }

    @Override
    public Image<?> getIcon(final AiReviewReportEntry entry) {
        final String severity = entry.getIssue().getSeverity();
        if (context.is(BaseContext.Env.WEBEDIT)) {
            return Image.Factory.fromUrl(severityDataUri(severity));
        }
        return Image.Factory.fromIcon(paintSeverityIcon(severity));
    }

    @Override
    public Image<?> getThumbnail(final AiReviewReportEntry entry) {
        return null;
    }

    @Override
    public String getTitle(final AiReviewReportEntry entry) {
        final AiReviewIssue issue = entry.getIssue();
        final StringBuilder title = new StringBuilder();

        final String severity = issue.getSeverity();
        if (severity != null && !severity.isEmpty()) {
            title.append("[").append(ReviewTheme.capitalize(severity)).append("] ");
        }

        final String abbr = entry.getContentLanguageAbbreviation();
        if (abbr != null && !abbr.isBlank()) {
            title.append("[").append(ReviewTheme.esc(abbr)).append("] ");
        }

        final String displayName = entry.getElement().getDisplayName(getDisplayLanguage());
        if (!displayName.isEmpty()) {
            title.append(displayName);
        } else {
            title.append(entry.getElement().getName());
        }

        final String cardLocator = cardLocatorLabel(
                issue.isCardScoped(), resolveCardIndex(entry), bundle);
        if (!cardLocator.isEmpty()) {
            title.append(" > ").append(cardLocator);
        }

        final String fieldLabel = issue.getFieldLabel();
        final String fieldName = issue.getFieldName();
        if (fieldLabel != null && !fieldLabel.isEmpty()) {
            title.append(" > ").append(fieldLabel);
        } else if (fieldName != null && !fieldName.isEmpty()) {
            title.append(" > ").append(fieldName);
        }

        return title.toString();
    }

    @Override
    public String getDescription(final AiReviewReportEntry entry) {
        return entry.getIssue().getDescription();
    }

    @Override
    public String getDetails(final AiReviewReportEntry entry) {
        final Language lang = getDisplayLanguage();
        final String rawName = entry.getElement().getDisplayName(lang);
        final String displayName = rawName.isEmpty() ? entry.getElement().getName() : rawName;
        return buildDetails(displayName, entry.getQualityScore(), entry.getIssue(), bundle,
                entry.getContentLanguageAbbreviation());
    }

    /**
     * Builds the HTML flyout for a review entry using the unified {@link ReviewTheme} palette.
     * Package-private to allow direct testing without a live FS client.
     */
    static String buildDetails(final String elementDisplayName, final String qualityScore,
                                 final AiReviewIssue issue, final ResourceBundle bundle,
                                 final String contentLanguageAbbreviation) {
        final StringBuilder html = new StringBuilder();

        html.append("<div style='font-family: ").append(ReviewTheme.FONT_STACK)
                .append("; padding: 8px; min-width: 280px; color: ").append(ReviewTheme.TEXT_COLOR).append(";'>");

        // Header: element name + score span
        html.append("<div style='margin-bottom: 8px;'>");
        html.append("<strong>").append(ReviewTheme.esc(elementDisplayName)).append("</strong>");
        final String scoreSpan = ReviewTheme.scoreSpan(qualityScore, bundle);
        if (!scoreSpan.isEmpty()) {
            html.append(" &mdash; ").append(scoreSpan);
        }
        html.append("</div>");

        // Language badge
        if (contentLanguageAbbreviation != null && !contentLanguageAbbreviation.isBlank()) {
            html.append(ReviewTheme.languageBadge(contentLanguageAbbreviation, bundle)).append(" ");
        }

        // Severity badge
        final String severity = issue.getSeverity();
        if (severity != null && !severity.isEmpty()) {
            html.append(ReviewTheme.severityBadge(severity)).append(" ");
        }

        // Element type badge
        final String elementType = issue.getElementType();
        if (elementType != null && !elementType.isEmpty()) {
            html.append(ReviewTheme.typeBadge(elementType)).append(" ");
        }

        // Field badge — label preferred over technical name; localized prefix from bundle
        final String fieldLabel = issue.getFieldLabel();
        final String fieldName = issue.getFieldName();
        final String displayField = (fieldLabel != null && !fieldLabel.isEmpty()) ? fieldLabel : fieldName;
        if (displayField != null && !displayField.isEmpty()) {
            html.append(ReviewTheme.fieldBadge(displayField, bundle));
        }

        // Description
        html.append("<p style='margin: 8px 0; color: ").append(ReviewTheme.TEXT_COLOR).append(";'>")
                .append(ReviewTheme.esc(issue.getDescription())).append("</p>");

        // Suggestion
        final String suggestion = issue.getSuggestion();
        if (suggestion != null && !suggestion.isEmpty()) {
            html.append("<p style='margin: 4px 0; color: ").append(ReviewTheme.SUGGESTION_COLOR).append("; font-style: italic;'>")
                    .append(bundle.getString("reviewSuggestion")).append(": ")
                    .append(ReviewTheme.esc(suggestion)).append("</p>");
        }

        html.append("</div>");
        return html.toString();
    }

    /**
     * Returns an SVG circle data URI for the given severity color.
     * Using a data URI avoids module web-app URL routing: relative URLs passed to
     * {@code Image.Factory.fromUrl()} are resolved relative to the project-specific CC
     * webapp root, not the module webapp, so file-based paths cannot be resolved reliably.
     * Package-private to allow direct testing.
     */
    static String severityDataUri(final String severity) {
        final String color = ReviewTheme.severityFg(severity);
        final String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='12' height='12'>"
                + "<circle cx='6' cy='6' r='5' fill='" + color + "'/></svg>";
        final String encoded = Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        return "data:image/svg+xml;base64," + encoded;
    }

    private Language getDisplayLanguage() {
        if (context.is(BaseContext.Env.WEBEDIT)) {
            return context.requireSpecialist(WebeditUiAgent.TYPE).getDisplayLanguage();
        }
        return context.requireSpecialist(UIAgent.TYPE).getDisplayLanguage();
    }

    /**
     * Returns a localized card-position label (e.g. "Entry 2") or an empty string when the issue
     * is not card-scoped or the 1-based index could not be resolved.
     * Package-private to allow direct testing without a live FS client.
     */
    static String cardLocatorLabel(final boolean cardScoped, final int oneBasedIndex,
                                    final ResourceBundle bundle) {
        if (!cardScoped || oneBasedIndex < 1) {
            return "";
        }
        return MessageFormat.format(bundle.getString("reviewCardEntry"), oneBasedIndex);
    }

    private int resolveCardIndex(final AiReviewReportEntry entry) {
        try {
            final AiReviewIssue issue = entry.getIssue();
            final String cardId = issue.getCardId();
            if (cardId == null || cardId.isEmpty()) {
                return -1;
            }
            if (!(entry.getElement() instanceof final DataProvider dataProvider)) {
                return -1;
            }
            final Language lang = getDisplayLanguage();
            final FormData formData = dataProvider.getFormData();
            for (final Object formElement : formData.getForm()) {
                String fieldName = null;
                if (formElement instanceof GomFormElement) {
                    fieldName = ((GomFormElement) formElement).getName().toString();
                } else if (formElement instanceof FormField) {
                    fieldName = ((FormField<?>) formElement).getName();
                }
                if (fieldName == null) {
                    continue;
                }
                final Object value = unwrapValue(formData.get(lang, fieldName), lang);
                if (!(value instanceof Catalog)) {
                    continue;
                }
                int index = 0;
                for (final Catalog.Card card : (Catalog) value) {
                    index++;
                    try {
                        if (cardId.equals(String.valueOf(card.getId()))) {
                            return index;
                        }
                    } catch (Exception ignored) { }
                }
            }
        } catch (Exception e) {
            Logging.logWarning("Could not resolve card index: " + e.getMessage(), AiReviewDataRenderer.class);
        }
        return -1;
    }

    private static Object unwrapValue(final Object raw, final Language language) {
        Object v = raw;
        for (int i = 0; i < 5 && v != null; i++) {
            if (v instanceof FormField) {
                v = ((FormField<?>) v).get();
            } else if (v instanceof EditorValue) {
                v = ((EditorValue<?>) v).get(language);
            } else {
                break;
            }
        }
        return v;
    }

    private static ImageIcon paintSeverityIcon(final String severity) {
        final int size = 12;
        final BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            try {
                g2.setColor(Color.decode(ReviewTheme.severityFg(severity)));
            } catch (NumberFormatException e) {
                g2.setColor(new Color(0x1a, 0x52, 0x76));
            }
            g2.fillOval(1, 1, size - 2, size - 2);
        } finally {
            g2.dispose();
        }
        return new ImageIcon(img);
    }
}
