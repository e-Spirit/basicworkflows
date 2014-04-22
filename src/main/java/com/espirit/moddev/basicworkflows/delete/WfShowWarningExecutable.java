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

import java.util.Map;
import java.util.ResourceBundle;

/**
 * This class is used to display a warning message in case of a forced delete.
 *
 * @author stephan
 * @since 1.0
 */
public class WfShowWarningExecutable extends WorkflowExecutable implements Executable {
    /** The logging class to use. */
    public static final Class<?> LOGGER = WfShowWarningExecutable.class;

    /** {@inheritDoc} */
    public Object execute(Map<String, Object> params) {
        WorkflowScriptContext workflowScriptContext = (WorkflowScriptContext) params.get("context");
        ResourceBundle.clearCache();
        final ResourceBundle bundle = ResourceBundle.getBundle(WorkflowConstants.MESSAGES, new FsLocale(workflowScriptContext).get());

        // show dialog
        showDialog(workflowScriptContext, bundle.getString("warning")+":", bundle.getString("warnDeleteMessage"));

        try {
            workflowScriptContext.doTransition("trigger_reference_ok_2");
        } catch (IllegalAccessException e) {
            Logging.logError("Workflow force transition failed!", e, LOGGER);
            // show error message
            showDialog(workflowScriptContext, bundle.getString("errorMsg"), bundle.getString("displayWarningFailed"));
        }

        return true;
    }

}
