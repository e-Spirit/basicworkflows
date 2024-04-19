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

import to.be.renamed.components.annotations.PublicComponent;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.BaseContext;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.contentstore.Dataset;
import de.espirit.firstspirit.access.store.pagestore.Page;
import de.espirit.firstspirit.access.store.sitestore.DocumentGroup;
import de.espirit.firstspirit.access.store.sitestore.PageRef;
import de.espirit.firstspirit.workflow.WebeditElementStatusProviderPlugin;
import de.espirit.firstspirit.workflow.WorkflowGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

import de.espirit.firstspirit.access.store.sitestore.SiteStoreFolder;

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
    public State getReleaseState(final IDProvider element) {
        State state;

        // Check the element
        state = getElementReleaseState(element);
        if (state != State.RELEASED) {
            return state;
        }

        // Check the parent
        IDProvider parent = element.getParent();
        if (parent != null && !"root".equals(parent.getUid())) {
            state = getElementReleaseState(parent);
            if (state != State.RELEASED) {
                return state;
            }
        }

        // Check the referencing page
        if (element instanceof PageRef) {
            final PageRef pageRef = (PageRef) element;
            state = getElementReleaseState(pageRef.getPage());
            if (state != State.RELEASED) {
                return state;
            }

        }

        return state;
    }

    @Override
    public List<WorkflowGroup> getWorkflowGroups(final IDProvider element) {
        final List<WorkflowGroup> collectedWorkflowGroups = new ArrayList<>();
        ResourceBundle.clearCache();
        final ResourceBundle bundle = ResourceBundle.getBundle(WorkflowConstants.MESSAGES, new FsLocale(context).get());

        if (element instanceof PageRef && pageHasTask((PageRef) element)) {
            final WorkflowGroup pageGroup = Factory.create(bundle.getString("page"),
                Collections.<IDProvider>singletonList(((PageRef) element).getPage()));
            collectedWorkflowGroups.add(pageGroup);
        } else if (element instanceof PageRef || element instanceof Page) {
            final WorkflowGroup pageRefGroup = Factory.create(bundle.getString("pageReference"), Collections.singletonList(element));
            collectedWorkflowGroups.add(pageRefGroup);
        } else if (element instanceof DocumentGroup) {
            final WorkflowGroup documentGroup = Factory.create(bundle.getString("documentGroup"), Collections.singletonList(element));
            collectedWorkflowGroups.add(documentGroup);
        } else if (element instanceof Dataset) {
            final List<IDProvider> dataSets = new ArrayList<>();
            dataSets.add(element);
            final WorkflowGroup workflowDataset = Factory.create(bundle.getString("dataset"), dataSets);
            collectedWorkflowGroups.add(workflowDataset);
        } else {
            if (element != null) {
                final String message = "No workflow group object created for element '%s'";
                Logging.logWarning(String.format(message, element.getClass().getName()), LOGGER);
            }
        }
        return collectedWorkflowGroups;
    }

    /**
     * Returns the release state for the store element.
     *
     * @param element
     * @return
     */
    private static State getElementReleaseState(final IDProvider element) {
        if (element.hasTask()) {
            return State.IN_WORKFLOW;
        } else if (isNotReleased(element)) {
            return State.CHANGED;
        }

        return State.RELEASED;
    }

    /**
     * Checks if the given element is currently released.
     *
     * @param element the element
     * @return true if the element is released
     */
    private static boolean isNotReleased(final IDProvider element) {
        return element.isReleaseSupported() && element.getReleaseStatus() != IDProvider.RELEASED;
    }

    private static boolean pageHasTask(final PageRef element) {
        return element.getPage() != null && element.getPage().hasTask();
    }

    @Override
    public void setUp(final BaseContext baseContext) {
        this.context = baseContext;
    }

    @Override
    public void tearDown() {
        //not needed here
    }
}
