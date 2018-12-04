/*
 * BasicWorkflows Module
 * %%
 * Copyright (C) 2012 - 2018 e-Spirit AG
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
package com.espirit.moddev.basicworkflows.delete;

import com.espirit.moddev.basicworkflows.util.AbstractWorkflowExecutable;
import com.espirit.moddev.basicworkflows.util.Dialog;
import com.espirit.moddev.basicworkflows.util.WorkflowConstants;
import com.espirit.moddev.basicworkflows.util.WorkflowSessionHelper;
import com.espirit.moddev.components.annotations.PublicComponent;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * This class is used to test the delete of the workflow object.
 *
 * @author stephan
 * @since 1.0
 */
@PublicComponent(name = "Delete WorkFlow Test Delete Executable")
public class WfDeleteTestExecutable extends AbstractWorkflowExecutable {

    /**
     * The logging class to use.
     */
    public static final Class<?> LOGGER = WfDeleteTestExecutable.class;


    @Override
    public Object execute(Map<String, Object> params) {
        WorkflowScriptContext workflowScriptContext = (WorkflowScriptContext) params.get(WorkflowConstants.CONTEXT);
        final ResourceBundle bundle = loadResourceBundle(workflowScriptContext);
        boolean deleteStatus = false;

        // check if test delete was successful (skip if wfDoTestFail is set by test case)
        if (isNotFailedTest(workflowScriptContext)) {
            // do test delete
            deleteStatus = new DeleteObject(workflowScriptContext).delete(true);
        }

        // if test delete was successful
        if (deleteStatus) {
            Logging.logInfo("Workflow Test Delete successful.", LOGGER);
            try {
                workflowScriptContext.doTransition("trigger_test_finished");
            } catch (IllegalAccessException e) {
                Logging.logError("Workflow Test Delete failed!", e, LOGGER);
                // show error message
                showDialog(workflowScriptContext, bundle.getString(WorkflowConstants.ERROR_MSG), bundle.getString("testDeleteFailed"));
            }
        } else {
            //if test delete failed, display locked elements
            try {
                // check test case or skip if wfDoTestFail is set
                if (isNotFailedTest(workflowScriptContext)) {
					List<List> lockedObjects = WorkflowSessionHelper.readObjectFromSession(workflowScriptContext, "wfLockedObjects");
                    StringBuilder notReleased = new StringBuilder(bundle.getString("objectsLocked")).append(":\n\n");
                    for (List lockedObject : lockedObjects) {
                        String elementType = (String) lockedObject.get(0);
                        String uid = (String) lockedObject.get(1);
                        notReleased.append(uid).append(" (").append(elementType).append(")\n");
                    }
                    // show dialog
                    showDialog(workflowScriptContext, bundle.getString(bundle.getString("objectsLocked") + ":\n\n"), notReleased.toString());
                }
                workflowScriptContext.doTransition("trigger_test_failed");
            } catch (IllegalAccessException e) {
                Logging.logError("Workflow Test Delete failed!", e, LOGGER);
                // set in integration tests
				final String suppressDialog = WorkflowSessionHelper.readObjectFromSession(workflowScriptContext, "wfSuppressDialog");
                if (!WorkflowConstants.TRUE.equals(suppressDialog)) {
                    // show error message
                    Dialog dialog = new Dialog(workflowScriptContext);
                    dialog.showError(bundle.getString(WorkflowConstants.ERROR_MSG), bundle.getString("testDeleteFailed"));
                }
            }
        }
        return true;
    }

}
