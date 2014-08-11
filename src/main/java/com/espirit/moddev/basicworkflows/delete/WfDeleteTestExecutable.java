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

package com.espirit.moddev.basicworkflows.delete;

import com.espirit.moddev.basicworkflows.util.FsLocale;
import com.espirit.moddev.basicworkflows.util.WorkflowConstants;
import com.espirit.moddev.basicworkflows.util.WorkflowExecutable;
import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.script.Executable;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;
import de.espirit.firstspirit.agency.OperationAgent;
import de.espirit.firstspirit.ui.operations.RequestOperation;

import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * This class is used to test the delete of the workflow object.
 *
 * @author stephan
 * @since 1.0
 */
public class WfDeleteTestExecutable extends WorkflowExecutable implements Executable {
    /** The logging class to use. */
    public static final Class<?> LOGGER = WfDeleteTestExecutable.class;
    /** Name for variable that determines if the test should fail. */
    public static final String TESTFAIL = "wfDoTestFail";

    /** {@inheritDoc} */
    public Object execute(Map<String, Object> params) {
        WorkflowScriptContext workflowScriptContext = (WorkflowScriptContext) params.get("context");
        ResourceBundle.clearCache();
        final ResourceBundle bundle = ResourceBundle.getBundle(WorkflowConstants.MESSAGES, new FsLocale(workflowScriptContext).get());
        boolean deleteStatus = false;

        // check if test delete was successful (skip if wfDoTestFail is set by test case)
        if(getCustomAttribute(workflowScriptContext, TESTFAIL) == null || getCustomAttribute(workflowScriptContext, TESTFAIL).equals("false")) {
            // do test delete
            deleteStatus = new DeleteObject(workflowScriptContext).delete(true);
        }

        // if test delete was successful
        if(deleteStatus) {
            Logging.logInfo("Workflow Test Delete successful.", LOGGER);
            try {
                workflowScriptContext.doTransition("trigger_test_finished");
            } catch (IllegalAccessException e) {
                Logging.logError("Workflow Test Delete failed!", e, LOGGER);
                // show error message
                showDialog(workflowScriptContext, bundle.getString("errorMsg"), bundle.getString("testDeleteFailed"));
            }
        } else {
            //if test delete failed, display locked elements
            try {
                // check test case or skip if wfDoTestFail is set
                if(getCustomAttribute(workflowScriptContext, TESTFAIL) == null || getCustomAttribute(workflowScriptContext, TESTFAIL).equals("false")) {
                    @SuppressWarnings("unchecked")
                    ArrayList<ArrayList> lockedObjects = (ArrayList<ArrayList>) workflowScriptContext.getSession().get("wfLockedObjects");
                    StringBuilder notReleased = new StringBuilder(bundle.getString("objectsLocked")).append(":\n\n");
                    for(ArrayList lockedObject : lockedObjects) {
                        String elementType = (String) lockedObject.get(0);
                        String uid = (String) lockedObject.get(1);
                        notReleased.append(uid).append(" (").append(elementType).append((")\n"));
                    }
                    // show dialog
                    showDialog(workflowScriptContext, bundle.getString(bundle.getString("objectsLocked")+":\n\n"), notReleased.toString());
                }
                workflowScriptContext.doTransition("trigger_test_failed");
            } catch (IllegalAccessException e) {
                Logging.logError("Workflow Test Delete failed!", e, LOGGER);
				final String suppressDialog = (String) workflowScriptContext.getSession().get("wfSuppressDialog"); // set in integration tests
				if (!"true".equals(suppressDialog)) {
					// show error message
					OperationAgent operationAgent = workflowScriptContext.requireSpecialist(OperationAgent.TYPE);
					RequestOperation requestOperation = operationAgent.getOperation(RequestOperation.TYPE);
					requestOperation.setTitle(bundle.getString("errorMsg"));
					requestOperation.perform(bundle.getString("testDeleteFailed"));
				}
            }
        }
        return true;
    }

}
