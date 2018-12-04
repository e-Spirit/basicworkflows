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

import de.espirit.firstspirit.access.store.StoreElement;
import de.espirit.firstspirit.access.store.StoreElementFolder;
import de.espirit.firstspirit.access.store.contentstore.Content2;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;

import java.util.Map;

import static de.espirit.common.base.Logging.logDebug;
import static de.espirit.common.base.Logging.logError;

/**
 * This class detects if the workflow was executed on a folder or a Page/PageRef/Media/etc. and then decides which transition to select for the next
 * workflow step. <p/> Folder   -&gt; workflow transition "{@value #TRANSITION_FOLDER}" is selected<br/> Other    -&gt; workflow transition "{@value
 * #TRANSITION_ELEMENT}" is selected<br/>
 */
@PublicComponent(name = "Delete WorkFlow Branch If Folder Executable")
public class WfBranchIfFolderExecutable extends AbstractWorkflowExecutable {

    public static final Class<?> LOGGER = WfBranchIfFolderExecutable.class;

    /**
     * This transition is used for folders.
     */
    public static final String TRANSITION_FOLDER = "folder";

    /**
     * This transition is used for other elements (Page, PageRef, Media, etc.).
     */
    public static final String TRANSITION_ELEMENT = "element";

    /**
     * Move the workflow transition based on the element type.
     *
     * @param params the Executable params
     * @return true if execution was successful
     */
    @Override
    public Object execute(Map<String, Object> params) {
        WorkflowScriptContext context = (WorkflowScriptContext) params.get(WorkflowConstants.CONTEXT);
        StoreElement element = context.getElement();
        String transition = null;
        logDebug("Checking workflowable type for element " + element, LOGGER);

        if (isFolder(element)) {
            logDebug("Element is folder", LOGGER);
            transition = TRANSITION_FOLDER;
        } else {
            logDebug("Element is no folder", LOGGER);
            transition = TRANSITION_ELEMENT;
        }

        try {
            context.doTransition(transition);
        } catch (IllegalAccessException e) {
            logError("Element type check failed!", e, LOGGER);
        }
        return true;
    }

    /**
     * Detects if the given StoreElement is a Folder or Content2.
     *
     * @param element the workflow element
     * @return true if the given element is a Folder
     */
    private static boolean isFolder(StoreElement element) {
        return element instanceof StoreElementFolder && !(element instanceof Content2);
    }
}
