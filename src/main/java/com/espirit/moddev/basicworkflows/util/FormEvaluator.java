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
package com.espirit.moddev.basicworkflows.util;

import de.espirit.firstspirit.access.editor.value.Option;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;
import de.espirit.firstspirit.forms.FormData;
import de.espirit.firstspirit.forms.FormField;

import java.util.Set;

/**
 * Convenience class to determine the value of a form element.
 *
 * @author stephan
 * @since 1.0
 */
public class FormEvaluator {

    /**
     * The workflowScriptContext from the workflow.
     */
    private WorkflowScriptContext workflowScriptContext;

    /**
     * Constructor for FormEvaluator.
     *
     * @param workflowScriptContext The workflowScriptContext from the workflow.
     */
    public FormEvaluator(WorkflowScriptContext workflowScriptContext) {
        this.workflowScriptContext = workflowScriptContext;
    }

    /**
     * Method to determine the value of a checkbox.
     *
     * @param varname The name of the variable to check.
     * @return true if the checkbox is checked.
     */
    public boolean getCheckboxValue(String varname) {
        boolean checkboxValue = false;

        Object relwMedia = workflowScriptContext.getTask().getCustomAttributes().get(varname);
        // test case
        if (relwMedia != null) {
            if (isReleaseMedia(relwMedia)) {
                checkboxValue = true;
            }
            // standard case
        } else {
            FormData data = workflowScriptContext.getFormData();
            if (data == null) {
                return false;
            }
            FormField dataValue = data.get(workflowScriptContext.getProject().getMasterLanguage(), varname);
            if (dataValue == null) {
                return false;
            }
            final Set<Option> options = (Set<Option>) dataValue.get();
            for (Option option : options) {
                checkboxValue = Boolean.parseBoolean((String) option.getValue());
            }
        }

        return checkboxValue;
    }

    private static boolean isReleaseMedia(Object relwMedia) {
        return WorkflowConstants.TRUE.equals(relwMedia);
    }
}
