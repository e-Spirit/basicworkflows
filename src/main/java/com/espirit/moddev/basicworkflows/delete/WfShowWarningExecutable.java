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
package com.espirit.moddev.basicworkflows.delete;

import com.espirit.moddev.basicworkflows.util.AbstractWorkflowExecutable;
import com.espirit.moddev.basicworkflows.util.WorkflowConstants;
import com.espirit.moddev.components.annotations.PublicComponent;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;

import java.util.Map;
import java.util.ResourceBundle;

/**
 * This class is used to display a warning message in case of a forced delete.
 *
 * @author stephan
 * @since 1.0
 */
@PublicComponent(name = "Delete WorkFlow Show Warning Executable")
public class WfShowWarningExecutable extends AbstractWorkflowExecutable {

    /**
     * The logging class to use.
     */
    public static final Class<?> LOGGER = WfShowWarningExecutable.class;

    @Override
    public Object execute(Map<String, Object> params) {
        WorkflowScriptContext workflowScriptContext = (WorkflowScriptContext) params.get(WorkflowConstants.CONTEXT);
        final ResourceBundle bundle = loadResourceBundle(workflowScriptContext);

        // show dialog
        showDialog(workflowScriptContext, bundle.getString(WorkflowConstants.WARNING) + ":", bundle.getString("warnDeleteMessage"));

        try {
            workflowScriptContext.doTransition("trigger_reference_ok_2");
        } catch (IllegalAccessException e) {
            Logging.logError("Workflow force transition failed!", e, LOGGER);
            // show error message
            showDialog(workflowScriptContext, bundle.getString(WorkflowConstants.ERROR_MSG), bundle.getString("displayWarningFailed"));
        }

        return true;
    }

}
