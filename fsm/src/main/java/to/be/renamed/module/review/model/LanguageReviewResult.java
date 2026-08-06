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

/**
 * Holds the AI review result for a single language, or an error message if the review failed.
 * {@link #getResult()} is {@code null} when {@link #hasError()} is {@code true}.
 *
 * @since 1.6.0
 */
public class LanguageReviewResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String languageAbbreviation;
    private String languageDisplayName;
    private AiReviewResult result;
    private boolean passed;
    private String errorMessage;

    public LanguageReviewResult() {
    }

    public LanguageReviewResult(final String languageAbbreviation,
                                final String languageDisplayName,
                                final AiReviewResult result,
                                final boolean passed,
                                final String errorMessage) {
        this.languageAbbreviation = languageAbbreviation;
        this.languageDisplayName = languageDisplayName;
        this.result = result;
        this.passed = passed;
        this.errorMessage = errorMessage;
    }

    public String getLanguageAbbreviation() {
        return languageAbbreviation;
    }

    public void setLanguageAbbreviation(final String languageAbbreviation) {
        this.languageAbbreviation = languageAbbreviation;
    }

    public String getLanguageDisplayName() {
        return languageDisplayName;
    }

    public void setLanguageDisplayName(final String languageDisplayName) {
        this.languageDisplayName = languageDisplayName;
    }

    public AiReviewResult getResult() {
        return result;
    }

    public void setResult(final AiReviewResult result) {
        this.result = result;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(final boolean passed) {
        this.passed = passed;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /** Returns {@code true} if this language's review failed with an error (no AI result available). */
    public boolean hasError() {
        return errorMessage != null && !errorMessage.isEmpty();
    }

    @Override
    public String toString() {
        return "LanguageReviewResult{" +
                "language='" + languageAbbreviation + '\'' +
                ", passed=" + passed +
                ", hasError=" + hasError() +
                '}';
    }
}
