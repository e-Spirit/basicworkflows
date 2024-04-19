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
package to.be.renamed.module.delete;

import to.be.renamed.module.util.AbstractWorkflowExecutable;
import to.be.renamed.module.util.WorkflowConstants;
import to.be.renamed.components.annotations.PublicComponent;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.BaseContext;
import de.espirit.firstspirit.access.Task;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;
import de.espirit.firstspirit.workflow.model.Transition;

import java.util.Map;

/**
 * This executable displays a dialog to select the next transition. If there is no transition selected (abort is pressed) the transition to a specific
 * target will be performed. The target is determined by its uid.
  */
@PublicComponent(name = "Delete WorkFlow Show Action Dialog Executable With Cancel Transition")
public class WfShowActionDialogExecutableWithCancelTransition extends AbstractWorkflowExecutable {

    /**
     * This is our logger.
     */
    public static final Class<?> LOGGER = WfShowActionDialogExecutableWithCancelTransition.class;

    /**
     * This is the uid of the target we wish to transition to.
     */
    public static final String TARGET_UID = "abort_delete";

    /**
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
                    return true;
                }
                transition = getNextTransitionByTargetName(context.getTransitions(), TARGET_UID);
                if (transition != null) {
                    context.doTransition(transition);
                }
            }
            return true;
        } catch (IllegalAccessException ex) {
            Logging.logError("Workflow failed!", ex, LOGGER);
            return false;
        }
    }
    
    /**
     * Find the transition 
     * @param transitions
     * @param targetName
     * @return 
     */
    private Transition getNextTransitionByTargetName(final Transition[] transitions, final String targetName) {
        for (Transition transition : transitions) {
            if (targetName.equals(transition.getTarget().getUid())) {
                return transition;
            }
        }
        return null;
    }

}
