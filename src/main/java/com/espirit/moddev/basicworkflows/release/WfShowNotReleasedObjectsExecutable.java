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

package com.espirit.moddev.basicworkflows.release;

import com.espirit.moddev.basicworkflows.util.AbstractWorkflowExecutable;
import com.espirit.moddev.basicworkflows.util.FsLocale;
import com.espirit.moddev.basicworkflows.util.WorkflowConstants;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;

import java.util.Collections;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * This class is used to display the elements that prevent the release of the workflow object.
 *
 * @author stephan
 * @since 1.0
 */
public class WfShowNotReleasedObjectsExecutable extends AbstractWorkflowExecutable {

    /**
     * The logging class to use.
     */
    public static final Class<?> LOGGER = WfShowNotReleasedObjectsExecutable.class;

    @Override
    public Object execute(Map<String, Object> params) {
        final WorkflowScriptContext workflowScriptContext = (WorkflowScriptContext) params.get(WorkflowConstants.CONTEXT);
        ResourceBundle.clearCache();
        final ResourceBundle bundle = ResourceBundle.getBundle(WorkflowConstants.MESSAGES, new FsLocale(workflowScriptContext).get());

        final StringBuilder messageBuilder = new StringBuilder();

        final Map<String, IDProvider.UidType>
            notReleasedElements =
            readMapFromSession(workflowScriptContext, WorkflowConstants.WF_NOT_RELEASED_ELEMENTS);
        final boolean brokenReferences = readBooleanFromSession(workflowScriptContext, WorkflowConstants.WF_BROKEN_REFERENCES);

        final String releaseObjectsLabel = bundle.getString("releaseObjects");
        renderMessage(messageBuilder, notReleasedElements, releaseObjectsLabel);

        if (mapContainsItems(notReleasedElements) && brokenReferences) {
            messageBuilder.append("\n\n");
        }

        if (brokenReferences) {
            final String brokenReferencesLabel = bundle.getString("brokenReferences");
            messageBuilder.append(brokenReferencesLabel);
        }

        showDialog(workflowScriptContext, bundle.getString("conflicts") + ":\n\n", messageBuilder.toString());

        try {
            workflowScriptContext.doTransition("trigger_check_not_released_objects");
        } catch (IllegalAccessException e) {
            Logging.logError("Workflow Re-Release Objects failed!", e, LOGGER);
            showDialog(workflowScriptContext, bundle.getString(WorkflowConstants.ERROR_MSG), bundle.getString("reReleaseFailed"));
        }

        return true;
    }

    private static boolean readBooleanFromSession(final WorkflowScriptContext workflowScriptContext, final String key) {
        final Boolean value = readObjectFromSession(workflowScriptContext, key);
        return value != null && value;
    }

    private static boolean mapContainsItems(final Map<String, IDProvider.UidType> map) {
        return map != null && !map.isEmpty();
    }

    private static Map<String, IDProvider.UidType> readMapFromSession(final WorkflowScriptContext workflowScriptContext, final String key) {
        final Map<String, IDProvider.UidType> map = readObjectFromSession(workflowScriptContext, key);
        return map == null ? Collections.<String, IDProvider.UidType>emptyMap() : map;
    }

    private static void renderMessage(StringBuilder message, Map<String, IDProvider.UidType> elements, String label) {
        if (elements != null && !elements.isEmpty()) {
            message.append(label).append(":\n\n");
            for (Map.Entry<String, IDProvider.UidType> entry : elements.entrySet()) {
                message.append(entry.getKey()).append("\n");
            }
        }
    }

}
