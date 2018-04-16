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

import com.espirit.moddev.basicworkflows.util.AbstractWorkflowExecutable;
import com.espirit.moddev.basicworkflows.util.WorkflowConstants;

import com.espirit.moddev.basicworkflows.util.WorkflowSessionHelper;
import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * This class is used to display the elements that prevent the deletion of the workflow object.
 *
 * @author stephan
 * @since 1.0
 */
public class WfShowRelatedObjectsExecutable extends AbstractWorkflowExecutable {

    /**
     * The logging class to use.
     */
    public static final Class<?> LOGGER = WfShowRelatedObjectsExecutable.class;


    @Override
    public Object execute(Map<String, Object> params) {
        WorkflowScriptContext workflowScriptContext = (WorkflowScriptContext) params.get(WorkflowConstants.CONTEXT);
        final ResourceBundle bundle = loadResourceBundle(workflowScriptContext);

		List<String> referencedObjects = WorkflowSessionHelper.readObjectFromSession(workflowScriptContext, "wfReferencedObjects");
        final String objectsInUse = bundle.getString("objectsInUse");
        StringBuilder notReleased = new StringBuilder(objectsInUse).append(":\n\n");
        int i = 0;
        for (String referencedObject : referencedObjects) {
            if (i != 0) {
                notReleased.append("\n");
            } else {
                i++;
            }
            notReleased.append(referencedObject);
        }

        // show dialog
        showDialog(workflowScriptContext, objectsInUse + ":", notReleased.toString());

        try {
            workflowScriptContext.doTransition("trigger_check_related_objects");
        } catch (IllegalAccessException e) {
            Logging.logError("Workflow Re-Delete Objects failed!", e, LOGGER);
            // show error message
            showDialog(workflowScriptContext, bundle.getString(WorkflowConstants.ERROR_MSG), bundle.getString("reDeleteFailed"));
        }

        return true;
    }


}
