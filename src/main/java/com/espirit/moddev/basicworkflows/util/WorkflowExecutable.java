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

import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;
import de.espirit.firstspirit.agency.OperationAgent;
import de.espirit.firstspirit.ui.operations.RequestOperation;

import java.io.Writer;
import java.util.Map;

/**
 * Abstract WorkflowExecutable that contains the basic methods used in all executables.
 *
 * @author stephan
 * @since 1.0
 */
public class WorkflowExecutable {

    /** {@inheritDoc} */
    public Object execute(Map<String, Object> context, Writer out, Writer err) {
        return execute(context);
    }

    /**
     * Main executable method, replaced by actual implementations.
     *
     * @param context The current FirstSpirit context.
     * @return The context object.
     */
    protected Object execute(Map<String, Object> context) {
        return context;
    }

    /**
     * A convenience method to display a message pop-up in the client.
     *
     * @param title The message title.
     * @param message The message to display.
     * @param workflowScriptContext The context to use.
     */
    protected void showDialog(WorkflowScriptContext workflowScriptContext, String title, String message){
        try {
            OperationAgent operationAgent = workflowScriptContext.requireSpecialist(OperationAgent.TYPE);
            RequestOperation requestOperation = operationAgent.getOperation(RequestOperation.TYPE);
            requestOperation.setTitle(title);
            requestOperation.perform(message);
        } catch (IllegalStateException e) {
            // catch exception for integration test case
        }
    }

    /**
     * Convenience method to get the value of a custom workflow attribute.
     *
     * @param workflowScriptContext The context to use.
     * @param attribute The attribute to get the value for.
     * @return the value.
     */
    protected Object getCustomAttribute(WorkflowScriptContext workflowScriptContext, String attribute) {
        return workflowScriptContext.getTask().getCustomAttributes().get(attribute);
    }

}
