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

import de.espirit.firstspirit.access.editor.CheckboxEditorValue;
import de.espirit.firstspirit.access.editor.value.Option;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;

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
        boolean releaseWithMedia = false;

        Object relwMedia = workflowScriptContext.getTask().getCustomAttributes().get("wf_releasewmedia");
        // test case
        if (relwMedia != null) {
            if (isReleaseMedia(relwMedia)) {
                releaseWithMedia = true;
            }
            // standard case
        } else {
            final CheckboxEditorValue value = (CheckboxEditorValue) workflowScriptContext.getData().get(varname).getEditor();
            final Set<Option> options = value.get(workflowScriptContext.getProject().getMasterLanguage());
            for (Option option : options) {
                releaseWithMedia = Boolean.parseBoolean((String) option.getValue());
            }
        }

        return releaseWithMedia;
    }

    private static boolean isReleaseMedia(Object relwMedia) {
        return WorkflowConstants.TRUE.equals(relwMedia);
    }
}
