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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import to.be.renamed.module.review.formatter.AiUnavailableMessageFormatter;
import to.be.renamed.module.review.formatter.ReviewResultFormatter;
import to.be.renamed.module.review.model.MultiLanguageReviewResult;
import to.be.renamed.module.util.AbstractWorkflowExecutable;
import to.be.renamed.module.util.TransitionResolver;
import to.be.renamed.module.util.WorkflowConstants;
import to.be.renamed.module.util.WorkflowSessionHelper;
import com.espirit.moddev.components.annotations.PublicComponent;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.BaseContext;
import de.espirit.firstspirit.access.User;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;

import java.text.MessageFormat;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Workflow executable that displays AI review results to the user.
 * <p>
 * Reads the aggregated {@link MultiLanguageReviewResult} from the workflow session
 * (key {@link WorkflowConstants#WF_AI_REVIEW_RESULTS}) and presents it to the user.
 * A null/blank/unparsable result is treated as "review skipped" (D6 — no backward-compat
 * re-derivation for in-flight sessions started before the multi-language format was introduced).
 * </p>
 *
 * @author Thomas Ritter
 * @since 1.6.0
 */
@PublicComponent(name = "WorkFlow Show AI Review Results")
public class WfShowAiReviewResultsExecutable extends AbstractWorkflowExecutable {

    /**
     * The logging class to use.
     */
    public static final Class<?> LOGGER = WfShowAiReviewResultsExecutable.class;

    /**
     * Returns {@code true} when the audit-log warning must be written.
     * Only a user who overrides a <em>failed</em> review warrants an audit entry;
     * proceeding after a passed review — even with minor issues — does not.
     */
    static boolean shouldAuditLog(final boolean reviewPassed) {
        return !reviewPassed;
    }

    @Override
    public Object execute(final Map<String, Object> params) {
        final WorkflowScriptContext context = (WorkflowScriptContext) params.get(WorkflowConstants.CONTEXT);
        final ResourceBundle bundle = loadResourceBundle(context);
        final ResourceBundle logBundle = loadEnglishResourceBundle();

        final boolean suppressDialog = WorkflowConstants.TRUE.equals(
                context.getSession().get(WorkflowConstants.WF_SUPPRESS_DIALOG));
        final boolean isWebEdit = context.is(BaseContext.Env.WEBEDIT);

        // Review could not run because the AI agent is missing/unavailable: show the
        // AI Assistant Suite product pop-up instead of a misleading "looks good" message,
        // then proceed (an unavailable optional gate must not block the release).
        final Boolean aiUnavailable = WorkflowSessionHelper.readObjectFromSession(
                context, WorkflowConstants.WF_AI_REVIEW_UNAVAILABLE);
        if (Boolean.TRUE.equals(aiUnavailable)) {
            Logging.logInfo("AI agent unavailable - showing AI Assistant Suite info pop-up", LOGGER);
            hideAiReviewLoadingIndicator(context);
            if (!suppressDialog) {
                final String title = bundle.getString("aiReviewOwnAiTitle");
                final String message = new AiUnavailableMessageFormatter().format(isWebEdit, bundle);
                if (isWebEdit) {
                    showHtmlDialog(context, title, message);
                } else {
                    showDialog(context, title, message);
                }
            }
            doAiNotAvailableTransition(context);
            return null;
        }

        // Read aggregated multi-language review results (D6: absent = "skipped", no backward-compat fallback)
        final MultiLanguageReviewResult results = readResults(context);
        if (results == null) {
            Logging.logInfo("No AI review result found in session - review may have been skipped", LOGGER);
            hideAiReviewLoadingIndicator(context);
            if (!suppressDialog) {
                showDialog(context, bundle.getString("aiReviewTitle"),
                        bundle.getString("reviewSkipped"));
            }
            doProceedTransition(context);
            return null;
        }

        final boolean reviewPassed = results.isPassed();

        // Format results for display — tabs in WebEdit (ContentCreator), stacked in SiteArchitect
        final ReviewResultFormatter formatter = new ReviewResultFormatter();
        final String formattedResults = isWebEdit
                ? formatter.format(results, bundle)
                : formatter.formatStacked(results, bundle);

        Logging.logInfo("Displaying AI review results: " + results.getTotalIssueCount() + " issues", LOGGER);

        if (!suppressDialog) {
            if (!reviewPassed) {
                final String question = bundle.getString("proceedWithIssues");

                final boolean proceed;
                if (isWebEdit) {
                    hideAiReviewLoadingIndicator(context);
                    final String htmlWithQuestion = formattedResults
                            + "<p style='margin-top:15px;font-weight:600;color:#333;'>" + question + "</p>";
                    proceed = showHtmlQuestionDialog(context,
                            bundle.getString("reviewResultsTitle"),
                            htmlWithQuestion,
                            bundle.getString("proceedButton"),
                            bundle.getString("declineButton"));
                } else {
                    hideAiReviewLoadingIndicator(context);
                    proceed = showCustomQuestionDialog(context,
                            bundle.getString("reviewResultsTitle"),
                            formattedResults + "\n\n" + question,
                            bundle.getString("proceedButton"),
                            bundle.getString("declineButton"));
                }

                if (!proceed) {
                    Logging.logInfo("User declined release due to AI review issues", LOGGER);

                    try {
                        TransitionResolver.doTransition(context,
                                                        WorkflowConstants.SESSION_TRANSITION_RESULTS_DECLINE,
                                                        WorkflowConstants.DEFAULT_TRANSITION_RESULTS_DECLINE);
                    } catch (IllegalAccessException e) {
                        Logging.logError("Failed to execute results_decline transition", e, LOGGER);
                    } catch (IllegalStateException e) {
                        Logging.logError(e.getMessage(), LOGGER);
                    }

                    return null;
                }

                if (shouldAuditLog(reviewPassed)) {
                    logUserProceeded(context, results, logBundle);
                }

            } else {
                final String question = bundle.getString("proceedWithoutIssues");

                final boolean proceed;
                if (isWebEdit) {
                    hideAiReviewLoadingIndicator(context);
                    final String htmlWithQuestion = formattedResults
                            + "<p style='margin-top:15px;font-weight:600;color:#333;'>" + question + "</p>";
                    proceed = showHtmlQuestionDialog(context,
                            bundle.getString("reviewResultsTitle"),
                            htmlWithQuestion,
                            bundle.getString("proceedButton"),
                            bundle.getString("declineButton"));
                } else {
                    hideAiReviewLoadingIndicator(context);
                    proceed = showCustomQuestionDialog(context,
                            bundle.getString("reviewResultsTitle"),
                            formattedResults + "\n\n" + question,
                            bundle.getString("proceedButton"),
                            bundle.getString("declineButton"));
                }

                if (!proceed) {
                    Logging.logInfo("User declined release after passed AI review", LOGGER);

                    try {
                        TransitionResolver.doTransition(context,
                                                        WorkflowConstants.SESSION_TRANSITION_RESULTS_DECLINE,
                                                        WorkflowConstants.DEFAULT_TRANSITION_RESULTS_DECLINE);
                    } catch (IllegalAccessException e) {
                        Logging.logError("Failed to execute results_decline transition", e, LOGGER);
                    } catch (IllegalStateException e) {
                        Logging.logError(e.getMessage(), LOGGER);
                    }

                    return null;
                }
            }
        }

        doProceedTransition(context);

        return null;
    }

    /**
     * Reads and deserialises the {@link MultiLanguageReviewResult} from the workflow session.
     * Returns {@code null} when the key is absent, blank, or contains invalid JSON (D6: all
     * treated as "review was skipped or ran before the multi-language format was introduced").
     */
    MultiLanguageReviewResult readResults(final WorkflowScriptContext context) {
        final String json = WorkflowSessionHelper.readObjectFromSession(
                context, WorkflowConstants.WF_AI_REVIEW_RESULTS);
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return new Gson().fromJson(json, MultiLanguageReviewResult.class);
        } catch (JsonSyntaxException e) {
            Logging.logWarning("Could not parse wfAiReviewResults JSON: " + e.getMessage(), LOGGER);
            return null;
        }
    }

    void doAiNotAvailableTransition(final WorkflowScriptContext context) {
        try {
            TransitionResolver.doTransition(context,
                    WorkflowConstants.SESSION_TRANSITION_AI_NOT_AVAILABLE,
                    WorkflowConstants.DEFAULT_TRANSITION_AI_NOT_AVAILABLE);
        } catch (IllegalStateException | IllegalAccessException e) {
            Logging.logError("Failed to execute ai_not_available transition", e, LOGGER);
        }
    }

    private void doProceedTransition(final WorkflowScriptContext context) {
        try {
            TransitionResolver.doTransition(context,
                    WorkflowConstants.SESSION_TRANSITION_RESULTS_PROCEED,
                    WorkflowConstants.DEFAULT_TRANSITION_RESULTS_PROCEED);
        } catch (IllegalAccessException e) {
            Logging.logError("Failed to execute results_proceed transition", e, LOGGER);
        } catch (IllegalStateException e) {
            Logging.logError(e.getMessage(), LOGGER);
        }
    }

    /**
     * Logs username and issue count for audit trail when user proceeds despite AI review issues.
     * Uses {@link MultiLanguageReviewResult#getTotalIssueCount()} so the count reflects all languages.
     */
    private void logUserProceeded(final WorkflowScriptContext context,
                                   final MultiLanguageReviewResult results,
                                   final ResourceBundle bundle) {
        final User user = context.getUser();
        final String userName = (user != null) ? user.getName() : "unknown";
        final IDProvider element = context.getElement();
        final String elementUid = (element != null) ? element.getName() : "unknown";
        final long elementId = (element != null) ? element.getId() : -1;

        Logging.logWarning(
                MessageFormat.format(bundle.getString("aiReviewUserProceeded"),
                        userName, results.getTotalIssueCount(), elementUid, elementId),
                LOGGER);
    }

}
