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

import to.be.renamed.module.projectapp.BasicWorkflowsProjectApp;
import to.be.renamed.module.util.WorkflowConstants;
import com.espirit.moddev.components.annotations.PublicComponent;
import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.BaseContext;
import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.agency.LanguageAgent;
import de.espirit.firstspirit.agency.ProjectAgent;
import de.espirit.firstspirit.agency.UIAgent;
import de.espirit.firstspirit.client.plugin.ReportPlugin;
import de.espirit.firstspirit.client.plugin.dataaccess.aspects.ReportItemsProviding;
import de.espirit.firstspirit.client.plugin.dataaccess.aspects.Reporting;
import de.espirit.firstspirit.client.plugin.report.DataProvider;
import de.espirit.firstspirit.client.plugin.report.DataRenderer;
import de.espirit.firstspirit.client.plugin.report.Image;
import de.espirit.firstspirit.client.plugin.report.Parameter;
import de.espirit.firstspirit.client.plugin.report.ParameterSelect;
import de.espirit.firstspirit.client.plugin.report.ReportItem;
import de.espirit.firstspirit.client.plugin.report.TransferHandler;
import de.espirit.firstspirit.webedit.WebeditUiAgent;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Report plugin that displays AI content review issues in the SiteArchitect and ContentCreator sidebar.
 * <p>
 * Scans all elements currently in a workflow, reads review data from {@code Task.getCustomAttributes()},
 * and presents each issue as a report entry with severity badge, field info, and suggestions.
 * </p>
 *
 * @since 1.6.0
 */
@PublicComponent(name = "AiReviewReport", displayName = "AI Content Review: Report")
public class AiReviewReportPlugin implements ReportPlugin<AiReviewReportEntry> {

    private static final Class<?> LOGGER = AiReviewReportPlugin.class;

    private BaseContext context;
    private ResourceBundle bundle;
    private ParameterSelect severityFilter;
    private ParameterSelect languageFilter;
    private boolean isVisible = false;

    @Override
    public void setUp(final BaseContext context) {
        Logging.logInfo("AiReviewReportPlugin.setUp() called, env.WEBEDIT=" + context.is(BaseContext.Env.WEBEDIT), LOGGER);
        this.context = context;

        try {
            final long projectId = context.requireSpecialist(ProjectAgent.TYPE).getId();
            if (isReportEnabledFor(context, projectId)) {
                final Language displayLanguage;
                if (context.is(BaseContext.Env.WEBEDIT)) {
                    displayLanguage = context.requireSpecialist(WebeditUiAgent.TYPE).getDisplayLanguage();
                } else {
                    displayLanguage = context.requireSpecialist(UIAgent.TYPE).getDisplayLanguage();
                }
                Logging.logInfo("AiReviewReportPlugin: displayLanguage=" + displayLanguage.getLocale(), LOGGER);

                final Locale langOnly = new Locale(displayLanguage.getLocale().getLanguage());
                ResourceBundle.clearCache(getClass().getClassLoader());
                this.bundle = ResourceBundle.getBundle(WorkflowConstants.MESSAGES, langOnly,
                                                       getClass().getClassLoader());
                Logging.logInfo("AiReviewReportPlugin: bundle loaded", LOGGER);

                buildParameters();
                isVisible = true;
                Logging.logInfo("AiReviewReportPlugin: setUp() complete", LOGGER);
            } else {
                Logging.logInfo("AiReviewReportPlugin: report disabled for project (checkbox off or app not installed)", LOGGER);
            }
        } catch (Exception e) {
            Logging.logError("AiReviewReportPlugin.setUp() failed: " + e.getMessage(), e, LOGGER);
        }
    }

    @Override
    public void tearDown() {
    }

    @Override
    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public String getTitle() {
        return bundle.getString("reportTitle");
    }

    @Override
    public Image<?> getIcon(final boolean selected) {
        if (context != null && context.is(BaseContext.Env.WEBEDIT)) {
            final String dataUri = buildIconDataUri(getClass());
            if (dataUri != null) {
                return Image.Factory.fromUrl(dataUri);
            }
        }
        return null;
    }

    /**
     * Loads the AI review report SVG icon from the classpath and returns it as a base64 data URI.
     * Returns {@code null} when the resource cannot be found or read.
     * Package-private to allow direct testing without a live FirstSpirit client.
     */
    static String buildIconDataUri(final Class<?> clazz) {
        try (final InputStream is = clazz.getResourceAsStream(
                "/com/espirit/moddev/basicworkflows/reports_wf_ai_analysis.svg")) {
            if (is == null) {
                return null;
            }
            final byte[] bytes = is.readAllBytes();
            return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (final IOException e) {
            Logging.logWarning("Could not load AI review report icon: " + e.getMessage(), LOGGER);
            return null;
        }
    }

    @Override
    public @NotNull List<Parameter<?>> getParameter() {
        return Arrays.asList(severityFilter, languageFilter);
    }

    @Override
    public @NotNull DataProvider<AiReviewReportEntry> createDataProvider() {
        return new AiReviewDataProvider(severityFilter, languageFilter);
    }

    @Override
    public @NotNull DataRenderer<AiReviewReportEntry> createDataRenderer() {
        return new AiReviewDataRenderer(context, bundle);
    }

    @Override
    public TransferHandler<AiReviewReportEntry> createTransferHandler() {
        return null;
    }

    @Override
    public @NotNull Collection<? extends ReportItem<AiReviewReportEntry>> getItems() {
        return Collections.emptyList();
    }

    @Override
    public ReportItem<AiReviewReportEntry> getDefaultItem() {
        return new NavigateToElementReportItem(bundle);
    }

    /** Protected seam so tests can stub report-enabled check without static mocking. */
    protected boolean isReportEnabledFor(final BaseContext ctx, final long projectId) {
        return BasicWorkflowsProjectApp.isReportEnabled(ctx, projectId);
    }

    private void buildParameters() {
        final List<ParameterSelect.SelectItem> severities = Arrays.asList(
                Parameter.Factory.createSelectItem(bundle.getString("reportFilterAll"), "ALL"),
                Parameter.Factory.createSelectItem(bundle.getString("reportFilterBlocking"), "blocking"),
                Parameter.Factory.createSelectItem(bundle.getString("reportFilterWarning"), "warning"),
                Parameter.Factory.createSelectItem(bundle.getString("reportFilterInfo"), "info")
        );
        severityFilter = Parameter.Factory.createSelect("severity_filter", severities, "ALL");

        final List<ParameterSelect.SelectItem> languages = new ArrayList<>();
        languages.add(Parameter.Factory.createSelectItem(bundle.getString("reportLanguageFilterAll"), "ALL"));
        try {
            final Collection<Language> projectLanguages =
                    context.requireSpecialist(LanguageAgent.TYPE).getLanguages();
            for (final Language language : projectLanguages) {
                final String abbr = language.getAbbreviation();
                languages.add(Parameter.Factory.createSelectItem(abbr, abbr));
            }
        } catch (Exception e) {
            Logging.logWarning("Could not load project languages for language filter: " + e.getMessage(), LOGGER);
        }
        languageFilter = Parameter.Factory.createSelect("language_filter", languages, "ALL");
    }
}
