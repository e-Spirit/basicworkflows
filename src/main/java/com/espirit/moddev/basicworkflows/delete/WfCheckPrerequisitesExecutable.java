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
import de.espirit.firstspirit.access.BaseContext;
import de.espirit.firstspirit.access.script.Executable;
import de.espirit.firstspirit.access.store.StoreElement;
import de.espirit.firstspirit.access.store.StoreElementFilter;
import de.espirit.firstspirit.access.store.contentstore.Content2;
import de.espirit.firstspirit.access.store.contentstore.ContentFolder;
import de.espirit.firstspirit.access.store.globalstore.GCAFolder;
import de.espirit.firstspirit.access.store.globalstore.GCAPage;
import de.espirit.firstspirit.access.store.mediastore.Media;
import de.espirit.firstspirit.access.store.mediastore.MediaFolder;
import de.espirit.firstspirit.access.store.pagestore.Page;
import de.espirit.firstspirit.access.store.pagestore.PageFolder;
import de.espirit.firstspirit.access.store.sitestore.DocumentGroup;
import de.espirit.firstspirit.access.store.sitestore.PageRef;
import de.espirit.firstspirit.access.store.sitestore.PageRefFolder;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;

import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;

import static de.espirit.firstspirit.access.store.StoreElementFilter.on;

/**
 * This class is used to do prerequisite checks (e.g. folder has children).
 *
 * @author stephan
 * @since 1.0
 */
public class WfCheckPrerequisitesExecutable extends WorkflowExecutable implements Executable{
    /** The logging class to use. */
    public static final Class<?> LOGGER = WfCheckPrerequisitesExecutable.class;

    /** {@inheritDoc} */
    public Object execute(Map<String, Object> params) {
        WorkflowScriptContext workflowScriptContext = (WorkflowScriptContext) params.get("context");
        ResourceBundle.clearCache();
        final ResourceBundle bundle = ResourceBundle.getBundle(WorkflowConstants.MESSAGES, new FsLocale(workflowScriptContext).get());

        //show warning dialog if prerequisites are not met and wfFolderCheckFail is not set
        if(getCustomAttribute(workflowScriptContext, "wfCheckPrerequisitesFail") == null) {
            StoreElement storeElement = workflowScriptContext.getStoreElement();
            String message = "";

            // check if folder has children
            @SuppressWarnings({"unchecked"}) final StoreElementFilter filter = on(PageFolder.class, Page.class, PageRefFolder.class, PageRef.class, DocumentGroup.class, ContentFolder.class, Content2.class, MediaFolder.class, Media.class, GCAPage.class, GCAFolder.class);
            if(storeElement.getChildren(filter, true).getFirst() != null) {
                message += bundle.getString("hasChildren");
            }

            // check if last element in pageref-folder is to be deleted
            if((storeElement instanceof PageRefFolder || storeElement instanceof PageRef || storeElement instanceof DocumentGroup) && !workflowScriptContext.is(BaseContext.Env.WEBEDIT)) {
                @SuppressWarnings({"unchecked"}) final StoreElementFilter sitestoreFilter = on(PageRefFolder.class, PageRef.class, DocumentGroup.class);
                Iterator iter = storeElement.getParent().getChildren(sitestoreFilter, false).iterator();
                // folder has at least one element
                iter.next();
                // check if there are more
                if(!iter.hasNext()) {
                    if(message.length()==0) {
                        message += "\n\n";
                    }
                    message += bundle.getString("lastElement")+"\n";
                }
            }

            if(message.length()>0) {
                // show warning message
                showDialog(workflowScriptContext, bundle.getString("warning"), message);
                try {
                    workflowScriptContext.doTransition("trigger_folder_ok");
                } catch (IllegalAccessException e) {
                    Logging.logError("Workflow Check failed!\n" + e, LOGGER);
                }
            } else {
                // do nothing if folder has no children
                try {
                    workflowScriptContext.doTransition("trigger_folder_ok");
                } catch (IllegalAccessException e) {
                    Logging.logError("Workflow Check failed!\n" + e, LOGGER);
                }
            }
        } else if(getCustomAttribute(workflowScriptContext, "wfFolderCheckFail") != null && getCustomAttribute(workflowScriptContext, "wfFolderCheckFail").equals("true")) {
            // fail test if wfFolderCheckFail is set
            try {
                workflowScriptContext.doTransition("trigger_check_conflict");
            } catch (IllegalAccessException e) {
                Logging.logError("Workflow Check failed!\n" + e, LOGGER);
            }
        }
    return true;
    }

}
