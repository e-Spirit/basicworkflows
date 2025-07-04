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

import com.espirit.moddev.components.annotations.PublicComponent;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.BaseContext;
import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.contentstore.Dataset;
import de.espirit.firstspirit.access.store.pagestore.Page;
import de.espirit.firstspirit.access.store.sitestore.DocumentGroup;
import de.espirit.firstspirit.access.store.sitestore.PageRef;
import de.espirit.firstspirit.workflow.WebeditElementStatusProviderPlugin;
import de.espirit.firstspirit.workflow.WorkflowGroup;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * WorkflowStatusProvider that is used for the basic workflows. Distinguishes between Page, PageReference, DocumentGroup and Dataset.
 *
 * @author stephan
 * @since 1.0
 */
@PublicComponent(name = "BasicWorkflows Status Provider")
public class BasicWorkflowStatusProvider implements WebeditElementStatusProviderPlugin {

    /**
     * The context to use.
     */
    private BaseContext context;

    public static final Class<?> LOGGER = BasicWorkflowStatusProvider.class;

    @Override
    public @NotNull State getReleaseState(final @NotNull IDProvider element) {
        State state;

        // Check the element
        state = getElementReleaseState(element, null);
        if (state != State.RELEASED) {
            return state;
        }

        // Check the parent
        IDProvider parent = element.getParent();
        if (parent != null && !"root".equals(parent.getUid())) {
            if (!parent.isInReleaseStore()) {
                return getElementReleaseState(parent, null);
            }
        }

        // Check the referencing page
        if (element instanceof final PageRef pageRef) {
            state = getElementReleaseState(Objects.requireNonNull(pageRef.getPage()), null);
        }

        return state;
    }

    @Override
    public @NotNull State getReleaseState(@NotNull final IDProvider element, @NotNull final Language language) {
        State state;

        // Check the element
        state = getElementReleaseState(element, language);
        if (state != State.RELEASED) {
            return state;
        }

        // Check the parent
        IDProvider parent = element.getParent();
        if (parent != null && !"root".equals(parent.getUid())) {
            if (!parent.isReachableInReleaseStore(language)) {
                return getElementReleaseState(parent, language);
            }
        }

        // Check the referencing page
        if (element instanceof final PageRef pageRef) {
            state = getElementReleaseState(Objects.requireNonNull(pageRef.getPage()), language);
        }

        return state;
    }

    @Override
    public @NotNull List<WorkflowGroup> getWorkflowGroups(final @NotNull IDProvider element) {
        ResourceBundle.clearCache();
        final ResourceBundle bundle = ResourceBundle.getBundle(WorkflowConstants.MESSAGES, new FsLocale(context).get());

        String headline = switch (getReleaseState(element)) {
            case IN_WORKFLOW -> bundle.getString("inWorkflow");
            case CHANGED -> bundle.getString("modified");
            case RELEASED -> bundle.getString("released");
        };

        return getWorkflowGroups(element, headline);
    }

    @Override
    public @NotNull List<WorkflowGroup> getWorkflowGroups(@NotNull final IDProvider element, @NotNull final Language language) {
        ResourceBundle.clearCache();
        final ResourceBundle bundle = ResourceBundle.getBundle(WorkflowConstants.MESSAGES, new FsLocale(context).get());

        String headline = switch (getReleaseState(element, language)) {
            case IN_WORKFLOW -> bundle.getString("inWorkflow");
            case CHANGED -> bundle.getString("modified");
            case RELEASED -> bundle.getString("released");
        };

        return getWorkflowGroups(element, headline);
    }

    @NotNull
    private List<WorkflowGroup> getWorkflowGroups(@NotNull final IDProvider element, final String headline) {
        final List<WorkflowGroup> collectedWorkflowGroups = new ArrayList<>();
        if (element instanceof PageRef && pageHasTask((PageRef) element)) {
            final WorkflowGroup pageGroup = Factory.create(headline, Collections.singletonList(((PageRef) element).getPage()));
            collectedWorkflowGroups.add(pageGroup);
        } else if (element instanceof PageRef || element instanceof Page || element instanceof DocumentGroup) {
            final WorkflowGroup pageRefGroup = Factory.create(headline, Collections.singletonList(element));
            collectedWorkflowGroups.add(pageRefGroup);
        } else if (element instanceof Dataset) {
            final List<IDProvider> dataSets = new ArrayList<>();
            dataSets.add(element);
            final WorkflowGroup workflowDataset = Factory.create(headline, dataSets);
            collectedWorkflowGroups.add(workflowDataset);
        } else {
            final String message = "No workflow group object created for element '%s'";
            Logging.logWarning(String.format(message, element.getClass().getName()), LOGGER);
        }
        return collectedWorkflowGroups;
    }

    private static State getElementReleaseState(final IDProvider element, final Language language) {
        if (element.hasTask()) {
            return State.IN_WORKFLOW;
        } else if (isNotReleased(element, language)) {
            return State.CHANGED;
        }

        return State.RELEASED;
    }

    /**
     * Checks if the given element is not currently released.
     *
     * @param element the element
     * @param language the language to check, can be null
     * @return true if the element is not released
     */
    private static boolean isNotReleased(final IDProvider element, final Language language) {
        if (!element.isReleaseSupported()) {
            return false;
        } else {
            return language == null ? element.getReleaseStatus() != IDProvider.RELEASED : element.getReleaseStatus(language) != IDProvider.RELEASED;
        }
    }

    private static boolean pageHasTask(final PageRef element) {
        return element.getPage() != null && element.getPage().hasTask();
    }

    @Override
    public void setUp(final @NotNull BaseContext baseContext) {
        this.context = baseContext;
    }

    @Override
    public void tearDown() {
        //not needed here
    }
}
