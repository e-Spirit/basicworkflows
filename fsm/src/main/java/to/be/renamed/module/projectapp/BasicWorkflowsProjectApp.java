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

import com.espirit.moddev.components.annotations.ProjectAppComponent;

import de.espirit.firstspirit.access.project.Project;
import de.espirit.firstspirit.agency.ModuleAdminAgent;
import de.espirit.firstspirit.agency.SpecialistsBroker;
import de.espirit.firstspirit.io.FileSystem;
import de.espirit.firstspirit.module.ProjectApp;
import de.espirit.firstspirit.module.ProjectEnvironment;
import de.espirit.firstspirit.module.descriptor.ProjectAppDescriptor;

import java.util.Collection;

/**
 * General BasicWorkflows project app. Its configuration ({@link BasicWorkflowsProjectAppConfig})
 * lets an editor import the workflow features into this project and toggle whether the AI review
 * report is shown. Report visibility is derived from the persisted checkbox, not from mere
 * installation of this app.
 */
@ProjectAppComponent(
    name = BasicWorkflowsProjectApp.PROJECT_APP_NAME,
    displayName = "BasicWorkflows Configuration",
    configurable = BasicWorkflowsProjectAppConfig.class
)
public class BasicWorkflowsProjectApp implements ProjectApp {

    /** Project app identifier (see {@code module.xml}). */
    public static final String PROJECT_APP_NAME = "BasicWorkflows_ProjectApp";

    private static final String MODULE_NAME = "basicworkflows";

    @Override
    public void init(final ProjectAppDescriptor projectAppDescriptor, final ProjectEnvironment projectEnvironment) {
        // Not used
    }

    @Override
    public void installed() {
        // Not used
    }

    @Override
    public void uninstalling() {
        // Not used
    }

    @Override
    public void updated(final String s) {
        // Not used
    }

    /**
     * Returns whether the AI review report should be shown for the given project: {@code true}
     * only when this project app is installed in the project and its "show report" checkbox is
     * enabled. Returns {@code false} when the app is not installed.
     *
     * @param specialistsBroker broker to resolve the {@link ModuleAdminAgent}.
     * @param projectId         id of the project to check.
     */
    public static boolean isReportEnabled(final SpecialistsBroker specialistsBroker, final long projectId) {
        final ModuleAdminAgent moduleAdminAgent = specialistsBroker.requireSpecialist(ModuleAdminAgent.TYPE);
        final Collection<Project> usages = moduleAdminAgent.getProjectAppUsages(MODULE_NAME, PROJECT_APP_NAME);
        for (final Project project : usages) {
            if (project.getId() == projectId) {
                final FileSystem<?> config = moduleAdminAgent.getProjectAppConfig(MODULE_NAME, PROJECT_APP_NAME, project);
                return BasicWorkflowsProjectAppConfigStore.readShowReport(config);
            }
        }
        return false;
    }
}
