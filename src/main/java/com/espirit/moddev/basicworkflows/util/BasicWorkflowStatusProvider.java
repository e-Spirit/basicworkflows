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

import de.espirit.firstspirit.access.BaseContext;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.StoreElement;
import de.espirit.firstspirit.access.store.contentstore.Dataset;
import de.espirit.firstspirit.access.store.sitestore.DocumentGroup;
import de.espirit.firstspirit.access.store.sitestore.PageRef;
import de.espirit.firstspirit.workflow.WebeditElementStatusProviderPlugin;
import de.espirit.firstspirit.workflow.WorkflowGroup;

import java.util.*;

/**
 * WorkflowStatusProvider that is used for the basic workflows.
 * Distinguishes between PageReference, DocumentGroup and Dataset.
 *
 * @author stephan
 * @since 1.0
 */
public class BasicWorkflowStatusProvider implements WebeditElementStatusProviderPlugin {

    /** the context to use. */
    private BaseContext context;

    /** {@inheritDoc} */
    public State getReleaseState (final IDProvider element) {
        State releaseState = State.RELEASED;
        HashMap<State, Boolean> releaseStatus = new HashMap<State, Boolean>();

        // check workflow element status
        if (element.isReleaseSupported() && element.getReleaseStatus() != IDProvider.RELEASED) {
            releaseStatus.put(State.CHANGED, true);
        }
        if (element.hasTask()) {
            releaseStatus.put(State.IN_WORKFLOW, true);
        }

        // check status of page as well if element is a pageref
        if(element instanceof PageRef) {
            if(((PageRef) element).getPage().getReleaseStatus() != IDProvider.RELEASED) {
                releaseStatus.put(State.CHANGED, true);
            } else if(((PageRef) element).getPage().hasTask()) {
                releaseStatus.put(State.IN_WORKFLOW, true);
            }
        // check status of pagerefs and pages as well if element is a documentgroup
        } else if(element instanceof DocumentGroup) {
            DocumentGroup documentGroup = (DocumentGroup) element;
            for(StoreElement storeElement : documentGroup.getChildren()) {
                if(storeElement instanceof PageRef) {
                    if(((PageRef) storeElement).getReleaseStatus() != IDProvider.RELEASED || ((PageRef) storeElement).getPage().getReleaseStatus() != IDProvider.RELEASED) {
                        releaseStatus.put(State.CHANGED, true);
                    } else if(storeElement.hasTask() || ((PageRef) storeElement).getPage().hasTask()) {
                        releaseStatus.put(State.IN_WORKFLOW, true);
                    }
                }
            }
        }
        // override status if element is a pageref or a documentgroup
        if(releaseStatus.get(State.IN_WORKFLOW) != null && releaseStatus.get(State.IN_WORKFLOW)) {
            releaseState = State.IN_WORKFLOW;
        } else if(releaseStatus.get(State.CHANGED) != null && releaseStatus.get(State.CHANGED)) {
            releaseState = State.CHANGED;
        }
        return releaseState;
    }

    /** {@inheritDoc} */
    public List<WorkflowGroup> getWorkflowGroups (final IDProvider element) {
        final List<WorkflowGroup> collectedWorkflowGroups = new ArrayList<WorkflowGroup>();
        ResourceBundle.clearCache();
        final ResourceBundle bundle = ResourceBundle.getBundle(WorkflowConstants.MESSAGES, new FsLocale(context).get());

        if (element instanceof PageRef) {
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
        }
        return collectedWorkflowGroups;
    }

    /** {@inheritDoc} */
    @Override
    public void setUp(BaseContext baseContext) {
        this.context = baseContext;
    }

    /** {@inheritDoc} */
    @Override
    public void tearDown() {
    }
}
