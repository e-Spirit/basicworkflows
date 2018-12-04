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
import com.espirit.moddev.basicworkflows.util.WorkflowConstants;
import com.espirit.moddev.components.annotations.PublicComponent;

import de.espirit.firstspirit.access.BaseContext;
import de.espirit.firstspirit.access.Task;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;
import de.espirit.firstspirit.workflow.model.Transition;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This executable displays a dialog to select the next transition.
 */
@PublicComponent(name = "Delete WorkFlow Show Action Dialog Executable")
public class WfShowActionDialogExecutable extends AbstractWorkflowExecutable {

    /**
     *
     * @param params the Executable params
     * @return true if execution was successful
     */
    @Override
    public Object execute(Map<String, Object> params) {
        WorkflowScriptContext context = (WorkflowScriptContext) params.get(WorkflowConstants.CONTEXT);
        try {
            if (context.is(BaseContext.Env.HEADLESS) || context.getTask().getMode() == Task.Mode.NO_CONTEXT) {
                context.doTransition((Transition) context.getProject().getProperty("targetTransition"));
            } else {
                Transition transition = context.showActionDialog();
                if (transition != null) {
                    context.doTransition(transition);
                }
            }
        } catch (IllegalAccessException ex) {
            Logger.getLogger(WfShowActionDialogExecutable.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

}
