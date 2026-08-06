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
package to.be.renamed.module.projectapp;

import to.be.renamed.module.deploy.FeatureResource;
import to.be.renamed.module.deploy.ImportResult;
import to.be.renamed.module.deploy.WorkflowFeatureImporter;
import to.be.renamed.module.util.WorkflowConstants;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.agency.SpecialistsBroker;
import de.espirit.firstspirit.module.Configuration;
import de.espirit.firstspirit.module.ProjectEnvironment;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Configuration GUI for the BasicWorkflows project app. Offers a checkbox that controls whether
 * the AI review report is shown, and a button that imports the three workflow features into the
 * current project (no overwrite of existing elements). The checkbox is persisted to the
 * project-app config file system.
 */
public class BasicWorkflowsProjectAppConfig implements Configuration<ProjectEnvironment> {

    public static final Class<?> LOGGER = BasicWorkflowsProjectAppConfig.class;

    private ProjectEnvironment environment;
    private boolean showReport;
    private JCheckBox showReportCheckBox;

    boolean isShowReport() {
        return showReport;
    }

    void setShowReport(final boolean showReport) {
        this.showReport = showReport;
    }

    @Override
    public void init(final String moduleName, final String componentName, final ProjectEnvironment env) {
        this.environment = env;
    }

    @Override
    public ProjectEnvironment getEnvironment() {
        return environment;
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public JComponent getGui(final Frame applicationFrame) {
        final ResourceBundle bundle = bundle();
        final JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        showReportCheckBox = new JCheckBox(bundle.getString("projectAppShowReport"), showReport);
        panel.add(showReportCheckBox, BorderLayout.NORTH);

        final JButton importButton = new JButton(bundle.getString("projectAppImportButton"));
        importButton.addActionListener(event -> onImportClicked(applicationFrame, bundle));
        panel.add(importButton, BorderLayout.SOUTH);

        return panel;
    }

    @Override
    public void load() {
        this.showReport = BasicWorkflowsProjectAppConfigStore.readShowReport(environment.getConfDir());
    }

    @Override
    public void store() {
        if (showReportCheckBox != null) {
            this.showReport = showReportCheckBox.isSelected();
        }
        try {
            BasicWorkflowsProjectAppConfigStore.writeShowReport(environment.getConfDir(), showReport);
        } catch (final IOException e) {
            Logging.logError("Could not store BasicWorkflows project-app config.", e, LOGGER);
        }
    }

    @Override
    public Set<String> getParameterNames() {
        final Set<String> names = new HashSet<>();
        names.add(BasicWorkflowsProjectAppConfigStore.KEY_SHOW_REPORT);
        return names;
    }

    @Override
    public String getParameter(final String name) {
        if (BasicWorkflowsProjectAppConfigStore.KEY_SHOW_REPORT.equals(name)) {
            return Boolean.toString(showReport);
        }
        return null;
    }

    private void onImportClicked(final Frame applicationFrame, final ResourceBundle bundle) {
        final Map<FeatureResource, ImportResult> results = runImport(environment.getBroker());
        JOptionPane.showMessageDialog(applicationFrame, formatResult(results, bundle),
            bundle.getString("projectAppImportResultTitle"), JOptionPane.INFORMATION_MESSAGE);
    }

    /** Runs the import into the current project; package-private seam for tests. */
    Map<FeatureResource, ImportResult> runImport(final SpecialistsBroker projectBroker) {
        return new WorkflowFeatureImporter().importAll(projectBroker);
    }

    /** Renders one localized line per feature outcome. Package-private for tests. */
    static String formatResult(final Map<FeatureResource, ImportResult> results, final ResourceBundle bundle) {
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<FeatureResource, ImportResult> entry : results.entrySet()) {
            final String key;
            switch (entry.getValue()) {
                case INSTALLED: key = "importInstalled"; break;
                case SKIPPED_EXISTS: key = "importSkippedExists"; break;
                case SKIPPED_NOT_INSTALLABLE: key = "importSkippedNotInstallable"; break;
                case SKIPPED_PREREQ: key = "importSkippedPrereq"; break;
                case FAILED: key = "importFailed"; break;
                default: throw new AssertionError("Unhandled ImportResult: " + entry.getValue());
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(MessageFormat.format(bundle.getString(key), entry.getKey().getFileName()));
        }
        return sb.toString();
    }

    private ResourceBundle bundle() {
        final Locale locale = Locale.getDefault();
        return ResourceBundle.getBundle(WorkflowConstants.MESSAGES, locale, getClass().getClassLoader());
    }
}
