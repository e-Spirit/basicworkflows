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

import to.be.renamed.module.review.model.AiReviewIssue;

import de.espirit.firstspirit.access.store.IDProvider;

/**
 * A single entry in the AI Review report, representing one issue found on one element.
 *
 * @since 1.6.0
 */
public class AiReviewReportEntry {

    private final IDProvider element;
    private final AiReviewIssue issue;
    private final String qualityScore;
    private final int index;
    private final String contentLanguageAbbreviation;
    private final String languageDisplayName;

    public AiReviewReportEntry(final IDProvider element, final AiReviewIssue issue,
                                final String qualityScore, final int index,
                                final String contentLanguageAbbreviation,
                                final String languageDisplayName) {
        this.element = element;
        this.issue = issue;
        this.qualityScore = qualityScore;
        this.index = index;
        this.contentLanguageAbbreviation = contentLanguageAbbreviation;
        this.languageDisplayName = languageDisplayName;
    }

    public IDProvider getElement() {
        return element;
    }

    public AiReviewIssue getIssue() {
        return issue;
    }

    public String getQualityScore() {
        return qualityScore;
    }

    public int getIndex() {
        return index;
    }

    public String getContentLanguageAbbreviation() {
        return contentLanguageAbbreviation;
    }

    public String getLanguageDisplayName() {
        return languageDisplayName;
    }
}
