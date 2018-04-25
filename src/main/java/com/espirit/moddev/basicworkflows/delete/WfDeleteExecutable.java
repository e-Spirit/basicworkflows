/*-
 * ========================LICENSE_START=================================
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
 * =========================LICENSE_END==================================
 */
package com.espirit.moddev.basicworkflows.delete;

import com.espirit.moddev.basicworkflows.util.AbstractWorkflowExecutable;
import com.espirit.moddev.basicworkflows.util.WorkflowConstants;
import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;

import java.util.Map;
import java.util.ResourceBundle;

/**
 * This class is used to actually delete the workflow object.
 *
 * @author stephan
 * @since 1.0
 */
public class WfDeleteExecutable extends AbstractWorkflowExecutable {

    /**
     * The logging class to use.
     */
    public static final Class<?> LOGGER = WfDeleteExecutable.class;
    private static final String MSG_WORKFLOW_DELETE_FAILED = "Workflow Delete failed!";


    @Override
    public Object execute(Map<String, Object> params) {
        WorkflowScriptContext workflowScriptContext = (WorkflowScriptContext) params.get(WorkflowConstants.CONTEXT);
        final ResourceBundle bundle = loadResourceBundle(workflowScriptContext);
        boolean deleteStatus = false;

        // check if delete was successful (skip if wfDoFail is set by test case)
        if (isNotFailed(workflowScriptContext)) {
            deleteStatus = new DeleteObject(workflowScriptContext).delete(false);
        }
        // if delete was successful
        if (deleteStatus) {
            try {
                workflowScriptContext.doTransition("trigger_finish");
                Logging.logInfo("Workflow Delete successful.", LOGGER);
            } catch (IllegalAccessException e) {
                Logging.logError(MSG_WORKFLOW_DELETE_FAILED, e, LOGGER);
                // show error message
                showDialog(workflowScriptContext, bundle.getString(WorkflowConstants.ERROR_MSG), bundle.getString(WorkflowConstants.DELETE_FAILED));
            }
        } else {
            // delete failed
            try {
                workflowScriptContext.doTransition("trigger_delete_failed");
            } catch (IllegalAccessException e) {
                Logging.logError(MSG_WORKFLOW_DELETE_FAILED, e, LOGGER);
                // show error message
                showDialog(workflowScriptContext, bundle.getString(WorkflowConstants.ERROR_MSG), bundle.getString(WorkflowConstants.DELETE_FAILED));
            }
        }
        return true;
    }

}
