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

package com.espirit.moddev.basicworkflows.release;

import com.espirit.moddev.basicworkflows.util.AbstractWorkflowExecutable;
import com.espirit.moddev.basicworkflows.util.FormEvaluator;
import com.espirit.moddev.basicworkflows.util.FsLocale;
import com.espirit.moddev.basicworkflows.util.StoreUtil;
import com.espirit.moddev.basicworkflows.util.WorkflowConstants;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.contentstore.ContentWorkflowable;
import de.espirit.firstspirit.access.store.sitestore.PageRef;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * This class is used to actually release the workflow object.
 *
 * @author stephan
 * @since 1.0
 */
public class WfReleaseExecutable extends AbstractWorkflowExecutable {

    /**
     * The logging class to use.
     */
    public static final Class<?> LOGGER = WfReleaseExecutable.class;


    @Override
    public Object execute(Map<String, Object> params) {
        WorkflowScriptContext workflowScriptContext = (WorkflowScriptContext) params.get(WorkflowConstants.CONTEXT);
        ResourceBundle.clearCache();
        final ResourceBundle bundle = ResourceBundle.getBundle(WorkflowConstants.MESSAGES, new FsLocale(workflowScriptContext).get());
        final boolean releaseWithMedia = new FormEvaluator(workflowScriptContext).getCheckboxValue("wf_releasewmedia");
        final WorkflowObject workflowObject = new WorkflowObject(workflowScriptContext);
        boolean releaseStatus = false;
        List<Object> releaseObjects = new ArrayList<Object>();

        // check test case or skip if wfDoFail is set
        if (isNotFailed(workflowScriptContext)) {
            Object releasePageRefElements = readObjectFromSession(workflowScriptContext, WorkflowConstants.RELEASE_PAGEREF_ELEMENTS);
            List<String> releasePageRefUids = null;
            if(releasePageRefElements != null) {
                releasePageRefUids = (List<String>) releasePageRefElements;
            }

            if (releasePageRefUids != null  && !releasePageRefUids.isEmpty()) {
                for (String pageRefUid : releasePageRefUids) {
                    PageRef pageRef = new StoreUtil(workflowScriptContext).loadPageRefByUid(pageRefUid);
                    workflowObject.setStoreElement(pageRef);
                    // add referenced elements from pageref
                    releaseObjects.addAll(workflowObject.getRefObjectsFromStoreElement(releaseWithMedia));
                    // add the pageref (and page)
                    if ((pageRef.getPage()).getReleaseStatus() != IDProvider.RELEASED) {
                        releaseObjects.add(pageRef.getPage());
                    }
                    releaseObjects.add(pageRef);
                }
                // do release
                releaseStatus = new ReleaseObject(workflowScriptContext, releaseObjects).release(false);
            } else if (workflowScriptContext.getWorkflowable() != null && workflowScriptContext.getWorkflowable() instanceof ContentWorkflowable) {
                // do release of referenced media if checkbox is checked
                releaseObjects.addAll(workflowObject.getRefObjectsFromEntity(releaseWithMedia));
                releaseStatus = new ReleaseObject(workflowScriptContext, releaseObjects).release(false);
                // release entity
                if (releaseStatus) {
                    ContentWorkflowable contentWorkflowable = (ContentWorkflowable) workflowScriptContext.getWorkflowable();
                    // do release
                    releaseStatus = new ReleaseObject(workflowScriptContext, contentWorkflowable.getEntity()).release(false);
                }
            } else {
                // add dependend objects
                releaseObjects.addAll(workflowObject.getRefObjectsFromStoreElement(releaseWithMedia));
                if (workflowScriptContext.getElement() instanceof PageRef
                    && (((PageRef) workflowScriptContext.getElement()).getPage()).getReleaseStatus() != IDProvider.RELEASED) {
                    releaseObjects.add(((PageRef) workflowScriptContext.getElement()).getPage());
                }
                // add the object
                releaseObjects.add(workflowScriptContext.getElement());
                // do release
                releaseStatus = new ReleaseObject(workflowScriptContext, releaseObjects).release(false);
            }
        }
        // check if release was successful (check wfDoFail for test case)
        if (releaseStatus) {
            try {
                // refresh workflow object
                if (workflowScriptContext.getWorkflowable() != null && workflowScriptContext.getWorkflowable() instanceof ContentWorkflowable) {
                    ((ContentWorkflowable) workflowScriptContext.getWorkflowable()).getEntity().refresh();
                } else {
                    workflowScriptContext.getElement().refresh();
                }
                // do final transition
                workflowScriptContext.doTransition("trigger_finish");
                Logging.logInfo("Workflow Release successful.", LOGGER);
            } catch (IllegalAccessException e) {
                Logging.logError("Workflow Release failed!", e, LOGGER);
                // show error message
                showDialog(workflowScriptContext, bundle.getString(WorkflowConstants.ERROR_MSG), bundle.getString("releaseFailed"));
            }
        } else {
            try {
                workflowScriptContext.doTransition("trigger_release_failed");
            } catch (IllegalAccessException e) {
                Logging.logError("Workflow Release failed!", e, LOGGER);
                // show error message
                showDialog(workflowScriptContext, bundle.getString(WorkflowConstants.ERROR_MSG), bundle.getString("releaseFailed"));
            }
        }
        return true;
    }

}
