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
import de.espirit.firstspirit.access.script.Executable;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.contentstore.ContentWorkflowable;
import de.espirit.firstspirit.access.store.sitestore.PageRef;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;

import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * This class is used to actually release the workflow object.
 *
 * @author stephan
 * @since 1.0
 */
public class WfReleaseExecutable extends WorkflowExecutable implements Executable {
    /** The logging class to use. */
    public static final Class<?> LOGGER = WfReleaseExecutable.class;

    /** {@inheritDoc} */
    public Object execute(Map<String, Object> params) {
        WorkflowScriptContext workflowScriptContext = (WorkflowScriptContext) params.get("context");
        ResourceBundle.clearCache();
        final ResourceBundle bundle = ResourceBundle.getBundle(WorkflowConstants.MESSAGES, new FsLocale(workflowScriptContext).get());
        final boolean releaseWithMedia = new FormEvaluator(workflowScriptContext).getCheckboxValue("wf_releasewmedia");
        final WorkflowObject workflowObject = new WorkflowObject(workflowScriptContext);
        boolean releaseStatus = false;
        ArrayList<Object> releaseObjects = new ArrayList<Object>();

        // check test case or skip if wfDoFail is set
        if(getCustomAttribute(workflowScriptContext, "wfDoFail") == null || getCustomAttribute(workflowScriptContext, "wfDoFail").equals("false")) {
            if(workflowScriptContext.getWorkflowable() != null && workflowScriptContext.getWorkflowable() instanceof ContentWorkflowable) {
                // do release of referenced media if checkbox is checked
                releaseObjects.addAll(workflowObject.getRefObjectsFromEntity(releaseWithMedia));
                releaseStatus = new ReleaseObject(workflowScriptContext, releaseObjects).release(false);
                // release entity
                if(releaseStatus) {
                    ContentWorkflowable contentWorkflowable = (ContentWorkflowable) workflowScriptContext.getWorkflowable();
                    // do release
                    releaseStatus = new ReleaseObject(workflowScriptContext, contentWorkflowable.getEntity()).release(false);
                }
            } else {
                // add dependend objects
                releaseObjects.addAll(workflowObject.getRefObjectsFromStoreElement(releaseWithMedia));
                if(workflowScriptContext.getStoreElement() instanceof PageRef && (((PageRef) workflowScriptContext.getStoreElement()).getPage()).getReleaseStatus() != IDProvider.RELEASED) {
                    releaseObjects.add(((PageRef) workflowScriptContext.getStoreElement()).getPage());
                }
                // add the object
                releaseObjects.add(workflowScriptContext.getStoreElement());
                // do release
                releaseStatus = new ReleaseObject(workflowScriptContext, releaseObjects).release(false);
            }
        }
        // check if release was successful (check wfDoFail for test case)
        if(releaseStatus) {
            try {
                // refresh workflow object
                if(workflowScriptContext.getWorkflowable() != null && workflowScriptContext.getWorkflowable() instanceof ContentWorkflowable) {
                    ((ContentWorkflowable) workflowScriptContext.getWorkflowable()).getEntity().refresh();
                } else {
                    workflowScriptContext.getStoreElement().refresh();
                }
                // do final transition
                workflowScriptContext.doTransition("trigger_finish");
                Logging.logInfo("Workflow Release successful.", LOGGER);
            } catch (IllegalAccessException e) {
                Logging.logError("Workflow Release failed!", e, LOGGER);
                // show error message
                showDialog(workflowScriptContext, bundle.getString("errorMsg"), bundle.getString("releaseFailed"));
            }
        } else {
            try {
                workflowScriptContext.doTransition("trigger_release_failed");
            } catch (IllegalAccessException e) {
                Logging.logError("Workflow Release failed!", e, LOGGER);
                // show error message
                showDialog(workflowScriptContext, bundle.getString("errorMsg"), bundle.getString("releaseFailed"));
            }
        }
        return true;
    }

}
