/*
 * **********************************************************************
 * basicworkflows
 * %%
 * Copyright (C) 2012 - 2013 e-Spirit AG
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
 * **********************************************************************
 */

package com.espirit.moddev.basicworkflows.util;

import de.espirit.common.Logging;
import de.espirit.firstspirit.access.BaseContext;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.StoreElement;
import de.espirit.firstspirit.access.store.contentstore.Dataset;
import de.espirit.firstspirit.access.store.pagestore.Page;
import de.espirit.firstspirit.access.store.sitestore.DocumentGroup;
import de.espirit.firstspirit.access.store.sitestore.PageRef;
import de.espirit.firstspirit.workflow.WebeditElementStatusProviderPlugin;
import de.espirit.firstspirit.workflow.WorkflowGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * WorkflowStatusProvider that is used for the basic workflows. Distinguishes between Page, PageReference, DocumentGroup and Dataset.
 *
 * @author stephan
 * @since 1.0
 */
public class BasicWorkflowStatusProvider implements WebeditElementStatusProviderPlugin {

    /**
     * the context to use.
     */
    private BaseContext context;
    public static final Class<?> LOGGER = BasicWorkflowStatusProvider.class;

    @Override
    public State getReleaseState(final IDProvider element) {
        State releaseStateResult = State.RELEASED;
        EnumMap<State, Boolean> releaseStatus = new EnumMap<State, Boolean>(State.class);

        // check workflow element status
        if (isNotReleased(element)) {
            releaseStatus.put(State.CHANGED, true);
        }
        if (element.hasTask()) {
            releaseStatus.put(State.IN_WORKFLOW, true);
        }

        // check status of page and pagereffolder as well if element is a pageref
        if (element instanceof PageRef) {
            PageRef pageRef = (PageRef) element;
            if (isNotReleased(pageRef.getPage()) || isNotReleased(element.getParent())) {
                releaseStatus.put(State.CHANGED, true);
            }
            if (pageRef.getPage().hasTask() || element.getParent().hasTask()) {
                releaseStatus.put(State.IN_WORKFLOW, true);
            }
            // check status of pagerefs and pages as well if element is a documentgroup
        } else if (element instanceof DocumentGroup) {
            DocumentGroup documentGroup = (DocumentGroup) element;
            for (StoreElement storeElement : documentGroup.getChildren()) {
                if (storeElement instanceof PageRef) {
                    PageRef pageRef = (PageRef) storeElement;
                    if (isNotReleased(pageRef) || isNotReleased(pageRef.getPage())) {
                        releaseStatus.put(State.CHANGED, true);
                    }
                    if (pageRef.hasTask() || pageRef.getPage().hasTask()) {
                        releaseStatus.put(State.IN_WORKFLOW, true);
                    }
                }
            }
        }
        // override status if element is a pageref or a documentgroup
        if (hasState(releaseStatus, State.IN_WORKFLOW)) {
            releaseStateResult = State.IN_WORKFLOW;
        } else if (hasState(releaseStatus, State.CHANGED)) {
            releaseStateResult = State.CHANGED;
        }

        return releaseStateResult;
    }

    /**
     * Checks if a given state if contained in the given State map.
     *
     * @param states the State Map
     * @param state  the State to look for in the Map
     * @return true if the State is contained in the State Map
     */
    private static boolean hasState(final Map<State, Boolean> states, final State state) {
        return states.get(state) != null && states.get(state);
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

    @Override
    public List<WorkflowGroup> getWorkflowGroups(final IDProvider element) {
        final List<WorkflowGroup> collectedWorkflowGroups = new ArrayList<WorkflowGroup>();
        ResourceBundle.clearCache();
        final ResourceBundle bundle = ResourceBundle.getBundle(WorkflowConstants.MESSAGES, new FsLocale(context).get());

        if (element instanceof PageRef || element instanceof Page) {
            final WorkflowGroup pageRefGroup = Factory.create(bundle.getString("pageReference"), Collections.singletonList(element));
            collectedWorkflowGroups.add(pageRefGroup);
        } else if (element instanceof DocumentGroup) {
            final WorkflowGroup documentGroup = Factory.create(bundle.getString("documentGroup"), Collections.singletonList(element));
            collectedWorkflowGroups.add(documentGroup);
        } else if (element instanceof Dataset) {
            final List<IDProvider> dataSets = new ArrayList<IDProvider>();
            dataSets.add(element);
            final WorkflowGroup workflowDataset = Factory.create(bundle.getString("dataset"), dataSets);
            collectedWorkflowGroups.add(workflowDataset);
        } else {
            if (element != null) {
                String message = "No workflow group object created for element '%s'";
                Logging.logWarning(String.format(message, element.getClass().getName()), LOGGER);
            }
        }
        return collectedWorkflowGroups;
    }

    @Override
    public void setUp(BaseContext baseContext) {
        this.context = baseContext;
    }

    @Override
    public void tearDown() {
        //not needed here
    }
}
