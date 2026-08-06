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

import com.google.gson.Gson;

import to.be.renamed.module.review.model.AiReviewIssue;
import to.be.renamed.module.review.model.AiReviewResult;
import to.be.renamed.module.review.model.LanguageReviewResult;
import to.be.renamed.module.review.model.MultiLanguageReviewResult;
import to.be.renamed.module.util.WorkflowConstants;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.Task;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.Store;
import de.espirit.firstspirit.agency.QueryAgent;
import de.espirit.firstspirit.agency.QueryResult;
import de.espirit.firstspirit.agency.SpecialistsBroker;
import de.espirit.firstspirit.agency.StoreAgent;
import de.espirit.firstspirit.webedit.WebeditUiAgent;
import de.espirit.firstspirit.client.plugin.report.DataProvider;
import de.espirit.firstspirit.client.plugin.report.ParameterMap;
import de.espirit.firstspirit.client.plugin.report.ParameterSelect;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Provides AI review report entries by scanning elements with active workflow tasks
 * and reading review data from their task custom attributes (workflow session).
 *
 * @since 1.6.0
 */
public class AiReviewDataProvider implements DataProvider<AiReviewReportEntry> {

    private static final Class<?> LOGGER = AiReviewDataProvider.class;
    private static final Gson GSON = new Gson();

    private final ParameterSelect severityFilterParam;
    private final ParameterSelect languageFilterParam;

    private final List<AiReviewReportEntry> allEntries = new ArrayList<>();
    private Iterator<AiReviewReportEntry> entryIterator;
    private String severityFilter;
    private String languageFilter;
    private boolean running;

    AiReviewDataProvider(final ParameterSelect severityFilterParam,
                         final ParameterSelect languageFilterParam) {
        this.severityFilterParam = severityFilterParam;
        this.languageFilterParam = languageFilterParam;
    }

    @Override
    public void start(final SpecialistsBroker broker, final ParameterMap parameter) {
        Logging.logInfo("AiReviewDataProvider.start() called", LOGGER);
        allEntries.clear();

        severityFilter = parameter.get(severityFilterParam);
        languageFilter = parameter.get(languageFilterParam);
        Logging.logInfo("Severity filter: " + severityFilter + ", language filter: " + languageFilter, LOGGER);

        try {
            final QueryAgent queryAgent = broker.requireSpecialist(QueryAgent.TYPE);
            final StoreAgent storeAgent = broker.requireSpecialist(StoreAgent.TYPE);

            final WebeditUiAgent webeditUiAgent = broker.requestSpecialist(WebeditUiAgent.TYPE);
            final boolean inContentCreator = webeditUiAgent != null;
            final IDProvider previewElement = inContentCreator ? webeditUiAgent.getPreviewElement() : null;
            Logging.logInfo("ContentCreator=" + inContentCreator
                    + ", previewElement=" + (previewElement != null ? previewElement.getId() : "null"), LOGGER);

            int elementCount = 0;
            try (final QueryResult workflowElements = queryAgent.answer("fs.workflow = *", null)) {
                for (final IDProvider element : workflowElements) {
                    if (inContentCreator && (previewElement == null || element.getId() != previewElement.getId())) {
                        continue;
                    }
                    elementCount++;

                    final Task task = element.getTask();
                    if (task == null) {
                        continue;
                    }

                    final Map<Object, Object> attributes = task.getCustomAttributes();
                    final Object resultsJson = attributes.get(WorkflowConstants.WF_AI_REVIEW_RESULTS);
                    if (resultsJson == null) {
                        continue;
                    }

                    try {
                        final MultiLanguageReviewResult multiResult = GSON.fromJson(
                                resultsJson.toString(), MultiLanguageReviewResult.class);
                        if (multiResult == null) {
                            continue;
                        }

                        for (final LanguageReviewResult langResult : multiResult.getLanguageResults()) {
                            if (langResult.hasError() || langResult.getResult() == null) {
                                continue;
                            }
                            final String abbr = langResult.getLanguageAbbreviation();
                            if (!matchesLanguage(abbr, languageFilter)) {
                                continue;
                            }

                            final AiReviewResult result = langResult.getResult();
                            final String qualityScore = result.getQualityScore();
                            final String displayName = langResult.getLanguageDisplayName();

                            final List<AiReviewIssue> issues = result.getIssues();
                            if (issues != null) {
                                for (final AiReviewIssue issue : issues) {
                                    if (matchesSeverity(issue, severityFilter)) {
                                        final IDProvider issueElement = resolveElement(storeAgent, issue, element);
                                        allEntries.add(new AiReviewReportEntry(
                                                issueElement, issue, qualityScore, allEntries.size(), abbr, displayName));
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Logging.logWarning("Failed to parse AI review results for element "
                                + element.getId() + ": " + e.getMessage(), LOGGER);
                    }
                }
            }
            Logging.logInfo("AI review report: " + elementCount + " workflow elements scanned, "
                    + allEntries.size() + " entries collected (severity: " + severityFilter
                    + ", language: " + languageFilter + ")", LOGGER);
        } catch (Exception e) {
            Logging.logError("Failed to query workflow elements for AI review report: " + e.getMessage(), e, LOGGER);
        }

        entryIterator = allEntries.iterator();
        running = !allEntries.isEmpty();
        Logging.logInfo("AiReviewDataProvider.start() finished, running=" + running + ", total entries: " + allEntries.size(), LOGGER);
    }

    private IDProvider resolveElement(final StoreAgent storeAgent, final AiReviewIssue issue,
                                      final IDProvider fallback) {
        final String issueId = issue.getId();
        if (issueId == null || issueId.isEmpty()) {
            return fallback;
        }

        final long nodeId;
        try {
            nodeId = Long.parseLong(issueId);
        } catch (NumberFormatException e) {
            return fallback;
        }

        for (final Store.Type type : Store.Type.values()) {
            try {
                final IDProvider found = storeAgent.getStore(type, false).getStoreElement(nodeId);
                if (found != null) {
                    return found;
                }
            } catch (Exception e) {
                // Some store types may not be available, skip
            }
        }

        return fallback;
    }

    private boolean matchesSeverity(final AiReviewIssue issue, final String filter) {
        if (filter == null || "ALL".equals(filter)) {
            return true;
        }
        return filter.equalsIgnoreCase(issue.getSeverity());
    }

    private boolean matchesLanguage(final String abbreviation, final String filter) {
        if (filter == null || "ALL".equalsIgnoreCase(filter)) {
            return true;
        }
        return filter.equalsIgnoreCase(abbreviation);
    }

    @Override
    public List<AiReviewReportEntry> getNext(final int count) {
        if (entryIterator == null) {
            running = false;
            return Collections.emptyList();
        }
        final List<AiReviewReportEntry> batch = new ArrayList<>();
        while (entryIterator.hasNext() && batch.size() < count) {
            batch.add(entryIterator.next());
        }
        if (!entryIterator.hasNext()) {
            running = false;
        }
        return batch;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getTotal() {
        return allEntries.size();
    }

    @Override
    public void close() {
        allEntries.clear();
        entryIterator = null;
    }

    @Override
    public String serialize(final AiReviewReportEntry object) {
        return String.valueOf(object.getIndex());
    }

    @Override
    public AiReviewReportEntry deserialize(final @NotNull String string) {
        final int idx = Integer.parseInt(string);
        if (idx >= 0 && idx < allEntries.size()) {
            return allEntries.get(idx);
        }
        return null;
    }
}
