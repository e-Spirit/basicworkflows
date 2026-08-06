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
import java.util.regex.Pattern;

/**
 * Data transfer object representing a single issue identified by AI content review.
 * Used as the element type in the structured output schema for AI responses.
 *
 * @since 1.6.0
 */
public class AiReviewIssue implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private String elementType;
    private String name;
    private String id;
    private String fieldName;
    private String fieldLabel;
    private String description;
    private String suggestion;
    @SchemaEnum({"info", "warning", "blocking"})
    private String severity;
    @SchemaEnum({"empty_field", "content_quality", "seo", "accessibility", "brand_voice"})
    private String problemType;
    private String cardId;

    /**
     * No-argument constructor required for JSON deserialization by structured output.
     */
    public AiReviewIssue() {
        this.elementType = "";
        this.name = "";
        this.id = "";
        this.fieldName = "";
        this.fieldLabel = "";
        this.description = "";
        this.suggestion = "";
        this.severity = "info";
        this.problemType = "content_quality";
        this.cardId = "";
    }

    public AiReviewIssue(final String elementType, final String name, final String id,
                         final String fieldName, final String problemType, final String description,
                         final String suggestion, final String severity) {
        this.elementType = elementType;
        this.name = name;
        this.id = id;
        this.fieldName = fieldName;
        this.problemType = problemType;
        this.description = description;
        this.suggestion = suggestion;
        this.severity = severity;
    }

    public String getElementType() {
        return elementType;
    }

    public void setElementType(final String elementType) {
        this.elementType = elementType;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(final String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldLabel() {
        return fieldLabel;
    }

    public void setFieldLabel(final String fieldLabel) {
        this.fieldLabel = fieldLabel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(final String suggestion) {
        this.suggestion = suggestion;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(final String severity) {
        this.severity = severity;
    }

    public String getProblemType() {
        return problemType;
    }

    public void setProblemType(final String problemType) {
        this.problemType = problemType;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(final String cardId) {
        this.cardId = cardId;
    }

    /**
     * Whether this issue is scoped to a specific FS_CATALOG card (as opposed to a section-level
     * field). True only when {@code cardId} is a syntactically valid UUID, so a hallucinated
     * non-UUID value never suppresses navigation to a genuine top-level field.
     */
    public boolean isCardScoped() {
        return cardId != null && UUID_PATTERN.matcher(cardId).matches();
    }

    @Override
    public String toString() {
        return "[" + elementType + ": " + name + "] [Field: " + fieldName + "] - " + description +
                (suggestion != null && !suggestion.isEmpty() ? " | Suggestion: " + suggestion : "");
    }
}
