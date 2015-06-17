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

import com.espirit.moddev.basicworkflows.util.FormEvaluator;
import com.espirit.moddev.basicworkflows.util.FsLocale;
import com.espirit.moddev.basicworkflows.util.WorkflowConstants;
import com.espirit.moddev.basicworkflows.util.WorkflowExecutable;

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
 * This class is used to test the release of the workflow object.
 *
 * @author stephan
 * @since 1.0
 */
public class WfReleaseTestExecutable extends WorkflowExecutable {

    /**
     * The logging class to use.
     */
    public static final Class<?> LOGGER = WfReleaseTestExecutable.class;

    /**
     * {@inheritDoc}
     */
    @Override
    public Object execute(Map<String, Object> params) {
        WorkflowScriptContext workflowScriptContext = (WorkflowScriptContext) params.get(WorkflowConstants.CONTEXT);
        ResourceBundle.clearCache();
        final ResourceBundle bundle = ResourceBundle.getBundle(WorkflowConstants.MESSAGES, new FsLocale(workflowScriptContext).get());
        final boolean releaseWithMedia = new FormEvaluator(workflowScriptContext).getCheckboxValue("wf_releasewmedia");
        final WorkflowObject workflowObject = new WorkflowObject(workflowScriptContext);
        boolean releaseStatus = false;
        List<Object> releaseObjects = new ArrayList<Object>();

        // check test case or skip if wfDoTestFail is set
        if (isNotFailedTest(workflowScriptContext)) {
            if (workflowScriptContext.getWorkflowable() != null && workflowScriptContext.getWorkflowable() instanceof ContentWorkflowable) {
                // do test release of referenced media if checkbox is checked
                releaseObjects.addAll(workflowObject.getRefObjectsFromEntity(releaseWithMedia));
                // do test release
                releaseStatus = new ReleaseObject(workflowScriptContext, releaseObjects).release(true);
                // test release entity
                if (releaseStatus) {
                    ContentWorkflowable contentWorkflowable = (ContentWorkflowable) workflowScriptContext.getWorkflowable();
                    // do release
                    releaseStatus = new ReleaseObject(workflowScriptContext, contentWorkflowable.getEntity()).release(true);
                }
            } else {
                // add dependend objects
                releaseObjects.addAll(workflowObject.getRefObjectsFromStoreElement(releaseWithMedia));
                // add the object
                releaseObjects.add(workflowScriptContext.getElement());
                if (workflowScriptContext.getElement() instanceof PageRef
                    && (((PageRef) workflowScriptContext.getElement()).getPage()).getReleaseStatus() != IDProvider.RELEASED) {
                    // if object is pageref, add page to release list if unreleased
                    releaseObjects.add(((PageRef) workflowScriptContext.getElement()).getPage());
                }
                // do test release
                releaseStatus = new ReleaseObject(workflowScriptContext, releaseObjects).release(true);
            }
        }
        // check if test release was successful (check wfDoTestFail for test case)
        if (releaseStatus) {
            try {
                workflowScriptContext.doTransition("trigger_test_finished");
                Logging.logInfo("Workflow Test Release successful.", LOGGER);
            } catch (IllegalAccessException e) {
                Logging.logError("Workflow Test Release failed!\n", e, LOGGER);
                // show error message
                showDialog(workflowScriptContext, bundle.getString(WorkflowConstants.ERROR_MSG), bundle.getString("testReleaseFailed"));
            }
        } else {
            try {
                workflowScriptContext.doTransition("trigger_test_failed");
            } catch (IllegalAccessException e) {
                Logging.logError("Workflow Test Release failed!", e, LOGGER);
                // show error message
                showDialog(workflowScriptContext, bundle.getString(WorkflowConstants.ERROR_MSG), bundle.getString("testReleaseFailed"));
            }
        }
        return true;
    }

    private boolean isNotFailedTest(WorkflowScriptContext workflowScriptContext) {
        return getCustomAttribute(workflowScriptContext, "wfDoTestFail") == null || WorkflowConstants.FALSE.equals(
            getCustomAttribute(workflowScriptContext, "wfDoTestFail"));
    }

}
