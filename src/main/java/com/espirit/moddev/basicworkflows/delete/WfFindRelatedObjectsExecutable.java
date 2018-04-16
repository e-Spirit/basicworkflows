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
import com.espirit.moddev.basicworkflows.util.FsLocale;
import com.espirit.moddev.basicworkflows.util.WorkflowConstants;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * This class is used to find the related objects of the workflow object and to determine if it can be deleted.
 *
 * @author stephan
 * @since 1.0
 */
public class WfFindRelatedObjectsExecutable extends AbstractWorkflowExecutable {

    /**
     * The logging class to use.
     */
    public static final Class<?> LOGGER = WfFindRelatedObjectsExecutable.class;

    @Override
    public Object execute(final Map<String, Object> params) {
        final WorkflowScriptContext workflowScriptContext = (WorkflowScriptContext) params.get(WorkflowConstants.CONTEXT);
        final ResourceBundle bundle = loadResourceBundle(workflowScriptContext);
        final WorkflowObject workflowObject = new WorkflowObject(workflowScriptContext);
        final ArrayList<Object> referencedObjects = new ArrayList<Object>();

        if (isStartedOnDatasource(workflowScriptContext)) {
            referencedObjects.addAll(workflowObject.getRefObjectsFromEntity());
        } else {
            referencedObjects.addAll(workflowObject.getRefObjectsFromStoreElement());
        }

        if (referencedObjects.isEmpty()) {
            Logging.logInfo("Can be deleted " + "(" + workflowObject.getId() + ")", LOGGER);
            try {
                workflowScriptContext.doTransition("trigger_reference_ok");
            } catch (final IllegalAccessException e) {
                Logging.logError("Workflow Delete failed!", e, LOGGER);
                showDialog(workflowScriptContext, bundle.getString(WorkflowConstants.ERROR_MSG), bundle.getString("deleteFailed"));
            }
        } else {
            Logging.logWarning("Cannot be deleted! " + "(" + workflowObject.getId() + ")", LOGGER);
            try {
                workflowScriptContext.doTransition("trigger_reference_conflict");
            } catch (final IllegalAccessException e) {
                Logging.logError("Workflow Delete failed!", e, LOGGER);
                // show error message
                showDialog(workflowScriptContext, bundle.getString(WorkflowConstants.ERROR_MSG), bundle.getString("deleteFailed"));
            }
        }
        return true;
    }

}
