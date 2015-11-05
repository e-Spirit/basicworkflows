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

import de.espirit.firstspirit.access.script.Executable;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;

import java.io.Writer;
import java.util.Map;


/**
 * Abstract WorkflowExecutable that contains the basic methods used in all executables.
 *
 * @author stephan
 * @since 1.0
 */
public abstract class AbstractWorkflowExecutable implements Executable {

    /**
     * The logging class to use.
     */
    public static final Class<?> LOGGER = AbstractWorkflowExecutable.class;


    @Override
    public final Object execute(Map<String, Object> args, Writer out, Writer err) {
        return execute(args);
    }

    /**
     * A convenience method to display a message pop-up in the client.
     *
     * @param title                 The message title.
     * @param message               The message to display.
     * @param workflowScriptContext The context to use.
     */
    protected void showDialog(WorkflowScriptContext workflowScriptContext, String title, String message) {
        // set in integration tests
        final String suppressDialog = (String) workflowScriptContext.getSession().get(WorkflowConstants.WF_SUPPRESS_DIALOG);
        if (!WorkflowConstants.TRUE.equals(suppressDialog)) {
            Dialog dialog = new Dialog(workflowScriptContext);
            dialog.showInfo(title, message);
        }
    }


    /**
     * A convenience method to display a question pop-up in the client.
     *
     * @param workflowScriptContext The context to use.
     * @param title                 The pop-up title.
     * @param question              The question to display.
     * @return Boolean indicating whether the question was answered with yes or no
     */
    protected boolean showQuestionDialog(WorkflowScriptContext workflowScriptContext, String title, String question) {
        // set in integration tests
        final String suppressDialog = (String) workflowScriptContext.getSession().get(WorkflowConstants.WF_SUPPRESS_DIALOG);
        if (!WorkflowConstants.TRUE.equals(suppressDialog)) {

            Dialog dialog = new Dialog(workflowScriptContext);

            return dialog.showQuestion(Dialog.QuestionType.YES_NO, title, question);

        } else {
            // Always return true for integration tests
            return true;
        }
    }


    /**
     * Convenience method to get the value of a custom workflow attribute.
     *
     * @param workflowScriptContext The context to use.
     * @param attribute             The attribute to get the value for.
     * @return the value.
     */
    protected static Object getCustomAttribute(WorkflowScriptContext workflowScriptContext, String attribute) {
        return workflowScriptContext.getTask().getCustomAttributes().get(attribute);
    }

    /**
     * Read object from session.
     *
     * @param <T>                   the type parameter
     * @param workflowScriptContext the workflow script context
     * @param key                   the key
     * @return the t
     */
    protected static <T> T readObjectFromSession(final WorkflowScriptContext workflowScriptContext, final String key) {
        return (T) workflowScriptContext.getSession().get(key); //NOSONAR
    }

    /**
     * Is not failed.
     *
     * @param workflowScriptContext the workflow script context
     * @return the boolean
     */
    protected static boolean isNotFailed(WorkflowScriptContext workflowScriptContext) {
        final Object wfDoFail = getCustomAttribute(workflowScriptContext, "wfDoFail");
        return wfDoFail == null || WorkflowConstants.FALSE.equals(wfDoFail);
    }

    protected static boolean isNotFailedTest(WorkflowScriptContext workflowScriptContext) {
        return getCustomAttribute(workflowScriptContext, "wfDoTestFail") == null || WorkflowConstants.FALSE.equals(
            getCustomAttribute(workflowScriptContext, "wfDoTestFail"));
    }
}
