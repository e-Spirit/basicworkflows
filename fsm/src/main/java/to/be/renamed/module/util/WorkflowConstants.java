/*
 * BasicWorkflows Module
 * %%
 * Copyright (C) 2012 - 2023 Crownpeak Technology GmbH - https://www.crownpeak.com
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
package to.be.renamed.module.util;

/**
 * Utility class to set constants.
 *
 * @author stephan
 * @since 1.0
 */
public interface WorkflowConstants {

    /**
     * The package name of the resource bundle.
     */
    String MESSAGES = "to.be.renamed.module.Messages";

    /**
     * String representation of true.
     */
    String TRUE = "true";

    /**
     * String representation of false.
     */
    String FALSE = "false";

    /**
     * Key for context in executable's parameters.
     */
    String CONTEXT = "context";

    /**
     * Resource bundle key for error message title.
     */
    String ERROR_MSG = "errorMsg";

    /**
     * Resource bundle key for delete failed message.
     */
    String DELETE_FAILED = "deleteFailed";

    /**
     * Resource bundle key for release failed message.
     */
    String RELEASE_FAILED = "releaseFailed";

    /**
     * Key to suppress dialog for testing.
     */
    String WF_SUPPRESS_DIALOG = "wfSuppressDialog";

    /**
     * Resource bundle key for warning message.
     */
    String WARNING = "warning";

    /**
     * Key that identifies the releasePageRefElements.
     */
    String RELEASE_PAGEREF_ELEMENTS = "releasePageRefElements";

    /**
     * Key that identifies the relatedPageRefElements.
     */
    String RELATED_PAGEREF_ELEMENTS = "relatedPageRefElements";

    String WF_NOT_RELEASED_ELEMENTS = "wfNotReleasedElements";

    String WF_BROKEN_REFERENCES = "wfBrokenReferences";

    String IMAGE_HAS_REFERENCES = "wfImageHasReferences";

    String IMAGE_FOLDER_HAS_REFERENCES = "wfImageFolderHasReferences";

    String WF_RECURSIVE_CHILDREN = "wfRecursiveChildren";

    String MEDIA_FORM_REFNAME = "wf_releasewmedia";

    String RECURSIVE_FORM_REFNAME = "wf_releaseRecursively";

    String LANGUAGES_FORM_REFNAME = "wf_languages";

    String WF_OBJECTS_IN_WORKFLOW = "wfObjectsInWorkflow";

    /**
     * Key for storing AI review blocking issues flag in workflow session.
     */
    String WF_AI_REVIEW_BLOCKING = "wfAiReviewBlocking";

    /**
     * Key for storing AI review timestamp in workflow session.
     */
    String WF_AI_REVIEW_TIMESTAMP = "wfAiReviewTimestamp";

    /**
     * Key for storing the AI review pass/fail decision in workflow session.
     */
    String WF_AI_REVIEW_PASSED = "wfAiReviewPassed";

    /** Session flag: {@code true} when the AI review was skipped because the AI agent is missing/unavailable. */
    String WF_AI_REVIEW_UNAVAILABLE = "wfAiReviewUnavailable";

    /**
     * Name of the prompt operation to use for AI content review.
     * Admins can configure this prompt in ServerManager → AI Configuration → Prompt Operations.
     * If not found, falls back to default built-in prompt.
     */
    String AI_REVIEW_PROMPT_OPERATION_NAME = "Content Review";

    // Default transition names for WfAiReviewExecutable
    String DEFAULT_TRANSITION_REVIEW_OK = "trigger_review_ok";
    String DEFAULT_TRANSITION_REVIEW_NOT_OK = "trigger_review_ok";

    // Default transition names for WfShowAiReviewResultsExecutable
    String DEFAULT_TRANSITION_RESULTS_PROCEED = "trigger_results_proceed";
    String DEFAULT_TRANSITION_RESULTS_DECLINE = "trigger_results_decline";

    // Session keys for storing transition names (layer 1 of transition resolution)
    String SESSION_TRANSITION_REVIEW_OK = "review_ok";
    String SESSION_TRANSITION_REVIEW_NOT_OK = "review_not_ok";
    String SESSION_TRANSITION_RESULTS_PROCEED = "results_proceed";
    String SESSION_TRANSITION_RESULTS_DECLINE = "results_decline";

    // Session key and default transition for the AI-not-available info dialog OK click
    String SESSION_TRANSITION_AI_NOT_AVAILABLE = "ai_not_available";
    String DEFAULT_TRANSITION_AI_NOT_AVAILABLE = "trigger_ai_not_available";

    // Session key for configurable prompt operation name
    String SESSION_PROMPT_OPERATION_NAME = "prompt_operation_name";

    // Form reference name for optional review instructions
    String AI_REVIEW_INSTRUCTIONS_FORM_REFNAME = "wf_aiReviewInstructions";

    // Session key for minimum acceptable quality score (default: Excellent)
    String SESSION_MIN_QUALITY_SCORE = "min_quality_score";

    /**
     * Resource bundle key for AI review loading indicator message.
     */
    String AI_REVIEW_LOADING_MESSAGE = "aiReviewLoadingMessage";


    /**
     * Session key for the serialized {@code MultiLanguageReviewResult} JSON (multi-language review).
     */
    String WF_AI_REVIEW_RESULTS = "wfAiReviewResults";

    /**
     * Resource bundle key for the per-language loading overlay progress message.
     * Accepts three positional arguments: {0} language display name, {1} current index, {2} total.
     */
    String AI_REVIEW_LOADING_MESSAGE_FOR_LANGUAGE = "aiReviewLoadingMessageForLanguage";
}
