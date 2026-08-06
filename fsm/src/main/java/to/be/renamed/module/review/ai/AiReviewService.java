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
package to.be.renamed.module.review.ai;

import to.be.renamed.module.review.model.AiReviewResponse;
import to.be.renamed.module.review.model.AiReviewResult;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.ai.GenerativeAIAgent;
import de.espirit.firstspirit.ai.dto.JsonSchema;
import de.espirit.firstspirit.ai.dto.Message;
import de.espirit.firstspirit.ai.dto.Request;
import de.espirit.firstspirit.ai.dto.Response;
import de.espirit.firstspirit.ai.dto.ResponseFormatJsonSchema;
import de.espirit.firstspirit.ai.dto.StrictMode;

import java.io.IOException;

/**
 * Handles AI communication for content review.
 * Builds structured output requests, calls {@link GenerativeAIAgent}, and returns parsed results.
 *
 * @since 1.6.0
 */
public class AiReviewService {

    private static final Class<?> LOGGER = AiReviewService.class;

    private static final String AI_REVIEW_SCHEMA = OpenAiSchemaGenerator.generate(AiReviewResponse.class);

    private final GenerativeAIAgent aiAgent;
    private final AiResponseParser parser;

    public AiReviewService(final GenerativeAIAgent aiAgent, final AiResponseParser parser) {
        this.aiAgent = aiAgent;
        this.parser = parser;
    }

    public AiReviewResult review(final String systemPrompt, final String content,
                                 final String reviewType) throws IOException {
        Logging.logInfo("Sending AI review request for " + reviewType, LOGGER);
        final JsonSchema schema = JsonSchema.builder()
                .json(AI_REVIEW_SCHEMA)
                .create();

        final ResponseFormatJsonSchema responseFormat = ResponseFormatJsonSchema.builder()
                .schema(schema)
                .strictMode(StrictMode.ENABLED)
                .create();

        final Request request = Request.builder()
                .addMessage(Message.builder()
                        .role(Message.SYSTEM)
                        .addText(systemPrompt)
                        .create())
                .addMessage(Message.builder()
                        .role(Message.USER)
                        .addText(content)
                        .create())
                .responseFormat(responseFormat)
                .create();

        Logging.logInfo("Sending structured request: " + logRequest(request), LOGGER);

        final Response response = aiAgent.request(request);

        final String responseText = response.getText().orElse("");
        if (responseText.trim().isEmpty()) {
            Logging.logWarning("Structured output returned empty response text", LOGGER);
            return parser.createErrorResult("Empty response from AI");
        }

        Logging.logInfo("Structured AI response JSON: " + responseText, LOGGER);

        return parser.parseJsonResponse(responseText);
    }

    private String logRequest(final Request request) {
        final StringBuilder sb = new StringBuilder();
        sb.append("--- AI Request ---");
        for (final Message msg : request.getMessages()) {
            sb.append("\n[").append(msg.getRole()).append("]\n").append(msg.getText().orElse("(no text)"));
        }
        request.getResponseFormat().ifPresent(fmt ->
                                                  sb.append("\n[ResponseFormat] ").append(fmt));
        if (!request.getTools().isEmpty()) {
            sb.append("\n[Tools] ").append(request.getTools());
        }
        sb.append("\n--- End AI Request ---");
        return sb.toString();
    }
}
