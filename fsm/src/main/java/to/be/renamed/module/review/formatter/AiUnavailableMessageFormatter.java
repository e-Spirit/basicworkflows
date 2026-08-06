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
package to.be.renamed.module.review.formatter;

import java.util.ResourceBundle;

/**
 * Selects the "AI review unavailable" product message for the current client.
 * <p>
 * ContentCreator (WebEdit) receives an HTML variant with a clickable link;
 * SiteArchitect receives a plain-text variant with the URL spelled out, because
 * Swing info dialogs cannot render clickable links.
 * </p>
 *
 * @since 1.6.0
 */
public class AiUnavailableMessageFormatter {

    /**
     * @param isWebEdit {@code true} when running in ContentCreator (WebEdit).
     * @param bundle    the resolved message bundle.
     * @return the HTML variant for WebEdit, otherwise the plain-text variant.
     */
    public String format(final boolean isWebEdit, final ResourceBundle bundle) {
        return isWebEdit
                ? bundle.getString("aiReviewOwnAiHtml")
                : bundle.getString("aiReviewOwnAi");
    }
}
