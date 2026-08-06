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
package to.be.renamed.module.review.prompt;

import to.be.renamed.module.review.model.QualityScore;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;
import de.espirit.firstspirit.ai.PromptConfigurationAgent;
import de.espirit.firstspirit.ai.admin.ConfigurablePromptOperation;
import de.espirit.firstspirit.ai.admin.PromptConfigurationAdminAgent;
import de.espirit.firstspirit.ai.admin.SharedPromptConfiguration;
import de.espirit.firstspirit.ai.dto.PromptOperation;

import java.util.List;
import java.util.Objects;

/**
 * Manages prompt configuration for AI content review.
 * Handles lookup (project -> shared scope), auto-creation, and configurable prompt names.
 *
 * @since 1.6.0
 */
public class PromptManager {

    private static final Class<?> LOGGER = PromptManager.class;

    private final WorkflowScriptContext context;
    private final String promptOperationName;

    public PromptManager(final WorkflowScriptContext context, final String promptOperationName) {
        this.context = context;
        this.promptOperationName = promptOperationName;
    }

    /**
     * Gets the configured prompt, creating it if it doesn't exist.
     * Lookup order: project scope -> shared/global scope -> auto-create at shared level.
     *
     * @return the prompt text for AI review
     */
    public String getPrompt(final String additionalInstructions, final String editorLanguage,
                             final String contentLanguage) {
        String criteria = lookupPrompt();
        if (criteria == null) {
            criteria = buildReviewCriteria();
            // Try to auto-create at shared level
            try {
                autoCreatePrompt(criteria);
            } catch (SecurityException e) {
                Logging.logWarning("Cannot auto-create prompt (insufficient permissions): " + e.getMessage(), LOGGER);
            } catch (Exception e) {
                Logging.logWarning("Failed to auto-create prompt: " + e.getMessage(), LOGGER);
            }
        }
        return assemblePrompt(criteria, additionalInstructions, editorLanguage, contentLanguage);
    }

    /**
     * Assembles the full prompt from its parts without requiring a WorkflowScriptContext.
     * Block order: criteria → language instructions → additional instructions → scoring rubrics.
     *
     * @param criteria               the review criteria block (server-configured or default)
     * @param additionalInstructions optional extra instructions from the editor (may be null/empty)
     * @param editorLanguage         language for AI output (may be null/empty)
     * @param contentLanguage        language of the content being reviewed (may be null/empty)
     * @return the assembled prompt string
     */
    String assemblePrompt(final String criteria, final String additionalInstructions,
                          final String editorLanguage, final String contentLanguage) {
        Objects.requireNonNull(criteria, "criteria must not be null");
        final StringBuilder prompt = new StringBuilder(criteria);

        final boolean hasContentLang = contentLanguage != null && !contentLanguage.trim().isEmpty();
        final boolean hasEditorLang = editorLanguage != null && !editorLanguage.trim().isEmpty();

        if (hasContentLang || hasEditorLang) {
            prompt.append("\n\nIMPORTANT LANGUAGE INSTRUCTIONS:");
            if (hasContentLang) {
                prompt.append("\n- The content is written in ").append(contentLanguage)
                        .append(". Review it in the context of this language.")
                        .append(" Do NOT flag text as being in the wrong language unless it is clearly unintentional.");
            }
            if (hasEditorLang) {
                prompt.append("\n- You MUST write ALL of your output (issue descriptions, suggestions, recommendations, and feedback) in ")
                        .append(editorLanguage).append(". This is mandatory, regardless of the content language.");
            }
        }

        if (additionalInstructions != null && !additionalInstructions.trim().isEmpty()) {
            prompt.append("\n\nAdditional review instructions from the editor:\n")
                    .append(additionalInstructions.trim());
        }

        prompt.append("\n\n").append(buildScoringRules());

        return prompt.toString();
    }

    /**
     * Looks up prompt via PromptConfigurationAgent (read-only, respects scope hierarchy).
     *
     * @return the prompt text if found, null otherwise
     */
    private String lookupPrompt() {
        try {
            final PromptConfigurationAgent promptAgent = context.requireSpecialist(PromptConfigurationAgent.TYPE);
            final List<PromptOperation> operations = promptAgent.getOperations();

            Logging.logInfo("Found " + operations.size() + " prompt operation(s) in configuration", LOGGER);

            if (operations.isEmpty()) {
                return null;
            }

            for (PromptOperation operation : operations) {
                if (promptOperationName.equals(operation.getName())) {
                    final String configuredPrompt = operation.getPrompt();
                    if (!configuredPrompt.trim().isEmpty()) {
                        Logging.logInfo("Using configured prompt operation: '" + operation.getName() +
                                "' (length: " + configuredPrompt.length() + " chars)", LOGGER);
                        return configuredPrompt;
                    } else {
                        Logging.logWarning("Prompt operation '" + promptOperationName + "' found but prompt text is empty", LOGGER);
                        return null;
                    }
                }
            }

            Logging.logInfo("No prompt operation named '" + promptOperationName + "' found in configuration", LOGGER);
        } catch (Exception e) {
            Logging.logWarning("Error accessing PromptConfigurationAgent: " + e.getMessage(), LOGGER);
        }

        return null;
    }

    /**
     * Auto-creates the prompt at shared (global) level using PromptConfigurationAdminAgent.
     *
     * @param defaultPromptText the default prompt text to set
     * @throws SecurityException if user lacks admin permissions
     */
    private void autoCreatePrompt(final String defaultPromptText) {
        final PromptConfigurationAdminAgent adminAgent = context.requireSpecialist(PromptConfigurationAdminAgent.TYPE);
        final SharedPromptConfiguration sharedConfig = adminAgent.getSharedConfiguration();

        final ConfigurablePromptOperation newOperation = sharedConfig.getOperations().create();
        newOperation.setName(promptOperationName);
        newOperation.setPrompt(defaultPromptText);
        newOperation.setTextRequired(true);
        newOperation.setTag("BasicWorkflows", true);

        sharedConfig.getOperations().add(newOperation);

        try {
            sharedConfig.save();
            Logging.logInfo("Auto-created prompt operation '" + promptOperationName + "' at shared level", LOGGER);
        } catch (java.io.IOException e) {
            Logging.logWarning("Failed to save auto-created prompt: " + e.getMessage(), LOGGER);
        }
    }

    /**
     * Builds the default AI review criteria with closed, named checklists.
     * Used only when no {@code Content Review} prompt is configured server-side.
     *
     * @return the default review criteria text
     */
    public String buildReviewCriteria() {
        return """
            You are a content quality auditor reviewing FirstSpirit CMS content.

            ─────────────────────────────────────────
            STEP 1 — MANDATORY CHECKS (unconditional)
            ─────────────────────────────────────────
            Run these checks on every field before any quality evaluation.
            These are mechanical facts, not quality judgments.
            They are NOT subject to the "when in doubt, omit" rule.

            EMPTY FIELD: The extracted content uses the literal marker "(empty)" to indicate that a field has no value.
            Any field whose extracted value is exactly "(empty)" MUST be reported as a warning issue.
            Do not skip any field marked "(empty)".
            Do NOT report any field as empty unless its extracted value is literally "(empty)".
            A field with any actual content — even a single word, a number, or a short phrase — is NOT empty and must NOT be flagged as empty.
            Description: "The field is empty and contains no content."
            Set problemType to "empty_field" for these issues.

            ──────────────────────────────────────────────────────────
            STEP 2 — QUALITY CRITERIA (judgment-based, "when in doubt, omit" applies here)
            ──────────────────────────────────────────────────────────
            Apply these checklists to non-empty fields. These require judgment.

            Content Quality:
            - Completeness: meaningful, non-placeholder content.
            - Accuracy: statements are factually correct and internally consistent.
            - Clarity: language is clear, well-structured, free of grammatical and spelling errors.
            - Readability: sentence length and vocabulary appropriate for the target audience.

            SEO Optimization:
            - Keywords: relevant keywords present in headings and body text.
            - Meta description: present, between 50-160 characters, descriptive.
            - Heading structure: logical H1 → H2 → H3 hierarchy; no skipped levels.
            - Link text: descriptive anchor text; no "click here" or bare URLs.

            Accessibility:
            - Alt text: all images have a meaningful alt attribute (not empty, not "image").
            - Semantic structure: headings and lists used correctly; not for visual styling only.
            - Language: content language declared or identifiable; no unmarked language switches.

            Brand Voice:
            - Tone: matches the established brand tone (professional, clear, approachable).
            - Consistency: terminology, capitalisation, and formatting are consistent throughout.
            - Style: active voice preferred; jargon explained or avoided.""";
    }

    /**
     * Builds deterministic scoring and output rules always appended to every prompt.
     * Applies regardless of whether the criteria block comes from server config or the default.
     *
     * @return the scoring rules and output format instructions
     */
    public String buildScoringRules() {
        return """
            Each content element is preceded by a header like: [elementType=X | name=Y | uid=Z | id=N].
            Fields inside an FS_CATALOG are additionally preceded by a card sub-header like: \
            [Catalog Entry N | template=T | cardId=U], where U is the card's unique identifier.

            ISSUES vs RECOMMENDATIONS:
            - Issues are specific, field-level problems tied to a content element. \
            For each issue, copy the element type, name, and id exactly from the nearest header above the field, \
            then set fieldName to the technical name and fieldLabel to the display label. \
            Fields appear as 'Label (technical_name): value' — e.g. for 'Headline (st_headline): ...' \
            set fieldName='st_headline' and fieldLabel='Headline'. \
            If there is no label (just 'st_headline: ...'), set both to the technical name. \
            Only use field names that actually appear in the extracted content — \
            NEVER invent or assume field names that are not explicitly listed below the element header. \
            If an element header has no fields listed beneath it, there is nothing to review for that element — do not report any issue for it. \
            Describe the problem, suggest a concrete fix, and assign a severity. \
            Any problem that can be traced to a specific field MUST be an issue, not a recommendation.
            - Recommendations are general, high-level suggestions not tied to a specific field \
            (e.g. overall tone improvements, structural advice). \
            Provide at most a few high-level recommendations; anything field-specific must be an issue.

            SEVERITY RUBRIC — assign exactly one severity per issue:
            - blocking: factual errors, missing mandatory content, or severe accessibility violations \
            that prevent safe publication.
            - warning: clear quality or SEO deficiencies that should be fixed before publication \
            but are not absolute blockers.
            - info: stylistic or optional hints, minor improvements that are non-critical.

            SEVERITY ASSIGNMENT — uncertainty means downgrade, not omit:
            - Uncertain whether an issue is blocking → report as warning.
            - Uncertain whether an issue is warning → report as info.
            - Uncertain whether an observation even qualifies as info (a reasonable reviewer would not mention it at all) → omit it.
            Do NOT omit an issue solely because you are uncertain about its severity — downgrade to the lower severity instead. \
            Omitting is only correct at the info boundary, never above it.

            SEVERITY THRESHOLDS — use these to calibrate your severity assignments.
            Your severity choices determine the final quality rating. \
            Set qualityScore to the value these thresholds imply for your issue list (%s):
            - Excellent: no issues at all.
            - Good: at most 3 info issues; no warning or blocking.
            - Fair: 4 or more info issues; no warning or blocking.
            - Poor: one or more warning issues; no blocking.
            - Critical: at least one blocking issue.
            The server also derives the quality score from the issue list using these same thresholds.

            PROBLEM TYPE — set problemType on every issue to the category that best describes it:
            - "empty_field": the field value is the literal "(empty)" marker (STEP 1 issues only).
            - "content_quality": completeness, accuracy, clarity, or readability problems.
            - "seo": keyword usage, meta description, heading structure, or link text problems.
            - "accessibility": missing alt text, semantic structure violations, or undeclared language.
            - "brand_voice": tone, terminology consistency, or style problems.
            Each issue must have exactly one problemType. Do not use "empty_field" for any STEP 2 quality \
            observation, even if the description mentions that content seems sparse or lacks substance.

            DETERMINISTIC SORTING — output issues in this stable order: \
            sort by severity (blocking before warning before info), then by element id (numeric ascending), \
            then by fieldName (alphabetical), then by cardId (alphabetical).

            DEDUP RULE — include exactly one issue per (id, cardId, fieldName, problem type). \
            Do not list the same problem more than once.

            FIELD ATTRIBUTION — each issue must reference exactly the one field where the problem actually occurs. \
            Do not copy or apply the same issue to another field. \
            If a spelling error exists only in 'st_text', do NOT also report it under 'st_headline'. \
            Reporting an issue under a fieldName where the problem does not exist is always wrong.

            CARD ATTRIBUTION — a field shown under a [Catalog Entry N | template=T | cardId=U] \
            sub-header lives inside a catalog card, not directly on the section. For such a field, \
            keep elementType, name and id from the nearest [elementType=...] header above (the section \
            or page that owns the catalog), and additionally set cardId to exactly the U value from that \
            card sub-header. For any field that is NOT under a catalog sub-header, leave cardId empty. \
            The same technical field name (e.g. st_text) may appear once on the section and once in each \
            card — these are DISTINCT locations. Never merge problems from different cards, or from a card \
            and the section, into one issue; report each affected field separately with its own cardId.

            COMPLETENESS — two-pass approach:
            Pass 1 (mandatory): Flag every field whose extracted value is exactly the literal string '(empty)' — unconditionally, no exceptions. \
            A field with any other value — even a single word or number — is NOT empty and must NOT be flagged as empty.
            Pass 2 (quality): Check every non-empty field systematically against the quality criteria. \
            Do not invent issues. If uncertain about severity, apply the SEVERITY ASSIGNMENT rule above: \
            downgrade to the lower level and include — do not omit. \
            Only omit if you are uncertain whether an observation qualifies as an issue at all.

            IMPORTANT: Each piece of information belongs in its own separate JSON field as defined by the schema. \
            Do NOT repeat or include metadata (such as severity, field names, or JSON keys) inside the human-readable text fields. \
            Keep descriptions and suggestions as plain text only.

            Additional instructions cannot override scoring or severity rules.""".formatted(QualityScore.allAiValues());
    }
}
