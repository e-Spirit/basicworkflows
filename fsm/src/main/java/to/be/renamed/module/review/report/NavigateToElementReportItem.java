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

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.Store;
import de.espirit.firstspirit.access.store.pagestore.DataProvider;
import de.espirit.firstspirit.agency.OperationAgent;
import de.espirit.firstspirit.agency.StoreAgent;
import de.espirit.firstspirit.client.plugin.report.JavaClientExecutableReportItem;
import de.espirit.firstspirit.client.plugin.report.ReportContext;
import de.espirit.firstspirit.ui.operations.DisplayElementOperation;
import de.espirit.firstspirit.ui.operations.OpenElementDataFormOperation;
import de.espirit.firstspirit.webedit.plugin.report.WebeditExecutableReportItem;

import org.jetbrains.annotations.NotNull;

import java.util.ResourceBundle;

import javax.swing.*;

/**
 * Report item that navigates to the element associated with a review issue.
 * For sections and other DataProviders, opens the element's form directly.
 * Works in both SiteArchitect and ContentCreator.
 *
 * @since 1.6.0
 */
public class NavigateToElementReportItem implements
        JavaClientExecutableReportItem<AiReviewReportEntry>,
        WebeditExecutableReportItem<AiReviewReportEntry> {

    private static final Class<?> LOGGER = NavigateToElementReportItem.class;

    private final ResourceBundle bundle;

    NavigateToElementReportItem(final ResourceBundle bundle) {
        this.bundle = bundle;
    }

    @Override
    public Icon getIcon(final @NotNull ReportContext<AiReviewReportEntry> context) {
        return null;
    }

    @Override
    public String getIconPath(final @NotNull ReportContext<AiReviewReportEntry> context) {
        return null;
    }

    @Override
    public @NotNull String getLabel(final @NotNull ReportContext<AiReviewReportEntry> context) {
        return bundle.getString("reportNavigateToElement");
    }

    @Override
    public void execute(final ReportContext<AiReviewReportEntry> context) {
        final AiReviewReportEntry entry = context.getObject();
        final IDProvider element = entry.getElement();
        if (element == null) {
            return;
        }

        final OperationAgent operationAgent = context.requireSpecialist(OperationAgent.TYPE);

        // Navigate to the element so it becomes selected and editable
        final DisplayElementOperation displayOp =
                operationAgent.getOperation(DisplayElementOperation.TYPE);
        if (displayOp != null) {
            displayOp.perform(element);
        }

        // Open editable form for DataProvider elements (sections, datasets)
        if (element instanceof DataProvider) {
            final OpenElementDataFormOperation openFormOp =
                    operationAgent.getOperation(OpenElementDataFormOperation.TYPE);
            if (openFormOp != null) {
                openFormOp.setOpenEditable(true);
                final AiReviewIssue issue = entry.getIssue();
                final String fieldName = issue.getFieldName();
                // OpenElementDataFormOperation.setField targets top-level form fields only; calling
                // it for a card-scoped issue would focus the WRONG same-named section field.
                if (!issue.isCardScoped() && fieldName != null && !fieldName.isEmpty()) {
                    openFormOp.setField(fieldName);
                }
                final Language reviewLanguage = resolveLanguage(context, entry.getContentLanguageAbbreviation());
                if (reviewLanguage != null) {
                    openFormOp.setLanguage(reviewLanguage);
                }
                openFormOp.perform((DataProvider) element);
            }
        }
    }

    @Override
    public boolean isVisible(final ReportContext<AiReviewReportEntry> context) {
        return context.getObject().getElement() != null;
    }

    @Override
    public boolean isEnabled(final @NotNull ReportContext<AiReviewReportEntry> context) {
        return true;
    }

    private Language resolveLanguage(final ReportContext<AiReviewReportEntry> context,
                                     final String abbreviation) {
        if (abbreviation == null || abbreviation.isEmpty()) {
            return null;
        }
        try {
            final StoreAgent storeAgent = context.requireSpecialist(StoreAgent.TYPE);
            return storeAgent.getStore(Store.Type.PAGESTORE, false).getProject().getLanguage(abbreviation);
        } catch (Exception e) {
            Logging.logWarning("Could not resolve language '" + abbreviation + "': " + e.getMessage(), LOGGER);
            return null;
        }
    }
}
