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

import to.be.renamed.module.review.language.ReviewLanguageResolver;
import to.be.renamed.module.review.model.AiReviewIssue;
import to.be.renamed.module.review.model.AiReviewResult;
import to.be.renamed.module.review.model.MultiLanguageReviewResult;
import to.be.renamed.module.review.model.QualityScore;
import to.be.renamed.module.util.AbstractWorkflowExecutable;
import to.be.renamed.module.util.FsLocale;
import to.be.renamed.module.util.TransitionResolver;
import to.be.renamed.module.util.WorkflowConstants;
import com.espirit.moddev.components.annotations.PublicComponent;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;
import de.espirit.firstspirit.ai.GenerativeAIAgent;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Workflow executable that performs AI-powered content review.
 * <p>
 * This executable extracts content from the workflow element, sends it to the AI for review,
 * and stores the results in the workflow session for display by subsequent executables.
 * </p>
 *
 * @author Thomas Ritter
 * @since 1.6.0
 */
@PublicComponent(name = "WorkFlow AI Review Executable")
public class WfAiReviewExecutable extends AbstractWorkflowExecutable {

    /**
     * The logging class to use.
     */
    public static final Class<?> LOGGER = WfAiReviewExecutable.class;

    @Override
    public Object execute(final Map<String, Object> params) {

        final WorkflowScriptContext context = (WorkflowScriptContext) params.get(WorkflowConstants.CONTEXT);
        final ResourceBundle bundle = loadResourceBundle(context);

        final boolean agentAvailable = isAiAgentAvailable(context);

        if (!agentAvailable) {
            clearSessionData(context);
            writeObjectToSession(context, WorkflowConstants.WF_AI_REVIEW_UNAVAILABLE, Boolean.TRUE);
            doReviewNotOkTransition(context);
            return null;
        }

        // Get workflow element
        final IDProvider element = context.getElement();
        if (element == null) {
            Logging.logError("No workflow element found", LOGGER);
            return null;
        }

        doReview(context, bundle, element);

        return null;
    }

    private boolean isAiAgentAvailable(final WorkflowScriptContext context) {
        try {
            final GenerativeAIAgent aiAgent = context.requireSpecialist(GenerativeAIAgent.TYPE);
            if (!aiAgent.isAvailable()) {
                Logging.logInfo("GenerativeAIAgent is not available/configured - skipping AI review", LOGGER);
                return false;
            }
            return true;
        } catch (Exception e) {
            Logging.logInfo("GenerativeAIAgent is not available - skipping AI review: " + e.getMessage(), LOGGER);
            return false;
        }
    }

    private void doReview(final WorkflowScriptContext context, final ResourceBundle bundle,
                          final IDProvider element) {
        try {
            // Read configurable prompt operation name: session key → default
            final Object sessionPromptName = context.getSession().get(WorkflowConstants.SESSION_PROMPT_OPERATION_NAME);
            final String promptOperationName = (sessionPromptName instanceof String && !((String) sessionPromptName).isEmpty())
                    ? (String) sessionPromptName
                    : WorkflowConstants.AI_REVIEW_PROMPT_OPERATION_NAME;

            final AiContentReviewer contentReviewer = new AiContentReviewer(context, promptOperationName);

            // Read optional instructions from workflow form
            final String instructions = readFormTextField(context, WorkflowConstants.AI_REVIEW_INSTRUCTIONS_FORM_REFNAME);

            // Resolve languages from wf_languages form selection (empty → all project languages)
            final List<Language> languages = new ReviewLanguageResolver().resolve(context);
            if (languages.isEmpty()) {
                Logging.logWarning("No review languages resolved — skipping AI review", LOGGER);
                clearSessionData(context);
                doReviewNotOkTransition(context);
                return;
            }

            // Progress overlay: show once before first language, update in-place for subsequent ones
            final String overlayTitle = bundle.getString("aiReviewTitle");
            final ProgressListener progressListener = (lang, index, total) -> {
                final String msg = MessageFormat.format(
                        bundle.getString(WorkflowConstants.AI_REVIEW_LOADING_MESSAGE_FOR_LANGUAGE),
                        lang.getName(), index, total);
                showAiReviewLoadingIndicator(context, overlayTitle, msg);
            };

            final QualityScore minScore = resolveMinimumScore(context);

            Logging.logInfo("Reviewing element " + element.getName() + " (ID: " + element.getId()
                    + ") in " + languages.size() + " language(s)", LOGGER);
            final MultiLanguageReviewResult aggregate = new LanguageReviewOrchestrator(contentReviewer)
                    .reviewAllLanguages(element, languages, "quality", instructions, minScore, progressListener);

            final boolean passed = determineReviewPassed(
                    aggregate.getWorstScore(), resolveMinimumScore(context), aggregate.isHasBlockingIssues());

            writeResultsToSession(context, aggregate, passed);

            if (passed) {
                doReviewOkTransition(context);
            } else {
                doReviewNotOkTransition(context);
            }

        } catch (Exception e) {
            Logging.logError("AI review failed with error", e, LOGGER);
            writeResultsToSession(context, new MultiLanguageReviewResult(), false);
            doReviewNotOkTransition(context);
        }
    }

    private void doReviewOkTransition(final WorkflowScriptContext context) {
        try {
            TransitionResolver.doTransition(context,
                    WorkflowConstants.SESSION_TRANSITION_REVIEW_OK,
                    WorkflowConstants.DEFAULT_TRANSITION_REVIEW_OK);
        } catch (IllegalAccessException e) {
            Logging.logError("Failed to execute review_ok transition", e, LOGGER);
        } catch (IllegalStateException e) {
            Logging.logError(e.getMessage(), LOGGER);
        }
    }

    private void doReviewNotOkTransition(final WorkflowScriptContext context) {
        try {
            TransitionResolver.doTransition(context,
                                            WorkflowConstants.SESSION_TRANSITION_REVIEW_NOT_OK,
                                            WorkflowConstants.DEFAULT_TRANSITION_REVIEW_NOT_OK);
        } catch (IllegalAccessException e) {
            Logging.logError("Failed to execute review_not_okay transition", e, LOGGER);
        } catch (IllegalStateException e) {
            Logging.logError(e.getMessage(), LOGGER);
        }
    }

    /**
     * Pure pass/fail decision. A blocking issue is a hard gate that fails the review
     * regardless of the configured minimum score.
     */
    static boolean determineReviewPassed(final QualityScore actual, final QualityScore minimum,
                                         final boolean hasBlockingIssues) {
        if (hasBlockingIssues) {
            return false;
        }
        if (actual == null) {
            return false;
        }
        if (minimum == null) {
            return actual == QualityScore.EXCELLENT;
        }
        return actual.meetsMinimum(minimum);
    }

    private boolean isReviewPassed(final WorkflowScriptContext context, final String actualScore,
                                   final boolean hasBlockingIssues) {
        final QualityScore actual = QualityScore.fromAiValue(actualScore);
        final QualityScore minimum = resolveMinimumScore(context);
        if (actual == null) {
            Logging.logWarning("Unknown quality score: " + actualScore, LOGGER);
        }
        Logging.logInfo("Review decision: actual=" + actualScore
                + ", minimum=" + (minimum != null ? minimum.getAiValue() : "null")
                + ", hasBlockingIssues=" + hasBlockingIssues, LOGGER);
        return determineReviewPassed(actual, minimum, hasBlockingIssues);
    }

    static QualityScore resolveMinimumScore(final WorkflowScriptContext context) {
        final Object sessionValue = context.getSession().get(WorkflowConstants.SESSION_MIN_QUALITY_SCORE);
        final String minScoreValue = (sessionValue instanceof String && !((String) sessionValue).isEmpty())
                ? (String) sessionValue
                : QualityScore.EXCELLENT.getAiValue();
        final QualityScore minimum = QualityScore.fromAiValue(minScoreValue);
        if (minimum == null) {
            Logging.logWarning("Unknown minimum quality score from session: " + minScoreValue
                    + ", defaulting to Excellent-only pass", LOGGER);
        }
        return minimum;
    }

    private String readFormTextField(final WorkflowScriptContext context, final String fieldName) {
        try {
            final de.espirit.firstspirit.forms.FormData formData = context.getFormData();
            if (formData == null) {
                return null;
            }
            if (formData.getForm().findEditor(fieldName) == null) {
                return null;
            }
            final de.espirit.firstspirit.forms.FormField<?> field =
                    formData.get(context.getProject().getMasterLanguage(), fieldName);
            if (field.get() != null) {
                return Objects.requireNonNull(field.get()).toString();
            }
        } catch (Exception e) {
            Logging.logDebug("Could not read form field '" + fieldName + "': " + e.getMessage(), LOGGER);
        }
        return null;
    }

    /**
     * Writes the aggregated multi-language review result into the workflow session.
     * This is a package-private seam so that tests can verify the exact set of session keys
     * written without executing the full workflow.
     */
    void writeResultsToSession(final WorkflowScriptContext workflowScriptContext,
                               final MultiLanguageReviewResult aggregate,
                               final boolean passed) {
        writeObjectToSession(workflowScriptContext, WorkflowConstants.WF_AI_REVIEW_RESULTS,
                new Gson().toJson(aggregate));
        writeObjectToSession(workflowScriptContext, WorkflowConstants.WF_AI_REVIEW_PASSED, passed);
        writeObjectToSession(workflowScriptContext, WorkflowConstants.WF_AI_REVIEW_BLOCKING,
                aggregate.isHasBlockingIssues());
        writeObjectToSession(workflowScriptContext, WorkflowConstants.WF_AI_REVIEW_TIMESTAMP,
                System.currentTimeMillis());
        writeObjectToSession(workflowScriptContext, WorkflowConstants.WF_AI_REVIEW_UNAVAILABLE,
                Boolean.FALSE);
    }

    private void clearSessionData(final WorkflowScriptContext context) {
        writeObjectToSession(context, WorkflowConstants.WF_AI_REVIEW_RESULTS, null);
        writeObjectToSession(context, WorkflowConstants.WF_AI_REVIEW_BLOCKING, null);
        writeObjectToSession(context, WorkflowConstants.WF_AI_REVIEW_TIMESTAMP, null);
        writeObjectToSession(context, WorkflowConstants.WF_AI_REVIEW_PASSED, null);
        writeObjectToSession(context, WorkflowConstants.WF_AI_REVIEW_UNAVAILABLE, null);
    }
}
