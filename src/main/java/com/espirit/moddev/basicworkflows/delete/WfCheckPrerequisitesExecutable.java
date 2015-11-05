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
import de.espirit.firstspirit.access.BaseContext;
import de.espirit.firstspirit.access.Task;
import de.espirit.firstspirit.access.store.ElementDeletedException;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.LockException;
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
import de.espirit.firstspirit.access.store.templatestore.Template;
import de.espirit.firstspirit.access.store.templatestore.TemplateFolder;
import de.espirit.firstspirit.access.store.templatestore.Workflow;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;
import de.espirit.firstspirit.agency.QueryAgent;

import java.util.HashMap;
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
public class WfCheckPrerequisitesExecutable extends AbstractWorkflowExecutable {

    /**
     * The logging class to use.
     */
    public static final Class<?> LOGGER = WfCheckPrerequisitesExecutable.class;
    private static final String WF_FOLDER_CHECK_FAIL = "wfFolderCheckFail";
    private static final String MSG_WORKFLOW_CHECK_FAILED = "Workflow Check failed!";

    @Override
    public Object execute(Map<String, Object> params) {
        WorkflowScriptContext workflowScriptContext = (WorkflowScriptContext) params.get(WorkflowConstants.CONTEXT);

        ResourceBundle.clearCache();
        final ResourceBundle bundle = ResourceBundle.getBundle(WorkflowConstants.MESSAGES, new FsLocale(workflowScriptContext).get());

        //show warning dialog if prerequisites are not met and wfFolderCheckFail is not set
        if (getCustomAttribute(workflowScriptContext, "wfCheckPrerequisitesFail") == null) {
            StoreElement storeElement = workflowScriptContext.getElement();
            String message = "";

            // workflow handling
            boolean abortWorkflow = false;
            IDProvider element = (IDProvider) storeElement;
            Workflow executedWorkflow = workflowScriptContext.getTask().getWorkflow();

            // check if workflow is executed on a workflow and on itself
            if (element instanceof Workflow) {
                boolean isExecutedWorkflow = isExecutedWorkflow(element, executedWorkflow);
                if (isExecutedWorkflow) {
                    Logging.logDebug("The workflow can't delete itself", LOGGER);
                    showDialog(workflowScriptContext, bundle.getString("deleteWorkflow"), bundle.getString("deleteDeleteItselfNotPossible"));
                    abortWorkflow = true;
                } else {
                    // query and close (if confirmed) all open workflow instances
                    QueryAgent queryAgent = workflowScriptContext.requireSpecialist(QueryAgent.TYPE);
                    Iterable<IDProvider> searchResults = queryAgent.answer("fs.workflow = *");
                    Map<Task, IDProvider> openInstances = getOpenInstances(element, searchResults);

                    if (!openInstances.isEmpty()) {
                        StringBuilder dialogMessage = new StringBuilder(bundle.getString("closeWorkflowInstances"));
                        for (Map.Entry<Task, IDProvider> entry : openInstances.entrySet()) {
                            dialogMessage.append("\n- ").append(entry.getValue().getUid());
                        }
                        boolean
                            confirmCloseTask =
                            showQuestionDialog(workflowScriptContext, bundle.getString("workflowInstances"), dialogMessage.toString());

                        if (confirmCloseTask) {
                            closeOpenInstances(openInstances);
                        } else {
                            abortWorkflow = true;
                        }
                    } else {
                        Logging.logDebug("No open workflow instances found", LOGGER);
                    }

                }
            }

            // check if folder has children
            final StoreElementFilter filter = on(TemplateFolder.class, Template.class, PageFolder.class, Page.class, PageRefFolder.class,
                                                 PageRef.class, DocumentGroup.class, ContentFolder.class, Content2.class, MediaFolder.class,
                                                 Media.class, GCAPage.class,
                                                 GCAFolder.class);
            if (!abortWorkflow &&
                (storeElement.getChildren(filter, true).getFirst() == null
                 || showQuestionDialog(workflowScriptContext, bundle.getString(WorkflowConstants.WARNING), bundle.getString("hasChildren")))) {

                // check if last element in pageref-folder is to be deleted
                if ((storeElement instanceof PageRefFolder || storeElement instanceof PageRef
                     || storeElement instanceof DocumentGroup)
                    && !workflowScriptContext.is(BaseContext.Env.WEBEDIT)) {

                    final StoreElementFilter sitestoreFilter = on(PageRefFolder.class, PageRef.class, DocumentGroup.class);
                    Iterator iter = storeElement.getParent().getChildren(sitestoreFilter, false).iterator();
                    iter.next();
                    if (!iter.hasNext()) {
                        if (message.length() == 0) {
                            message += "\n\n";
                        }
                        message += bundle.getString("lastElement") + "\n";
                    }
                }

                if (message.length() > 0) {
                    showDialog(workflowScriptContext, bundle.getString(WorkflowConstants.WARNING), message);
                }

                try {
                    workflowScriptContext.doTransition("trigger_folder_ok");
                } catch (IllegalAccessException e) {
                    Logging.logError(MSG_WORKFLOW_CHECK_FAILED, e, LOGGER);
                }
            } else {
                try {
                    workflowScriptContext.doTransition("trigger_abort_deletion");
                } catch (IllegalAccessException e) {
                    Logging.logError(MSG_WORKFLOW_CHECK_FAILED, e, LOGGER);
                }
            }
        } else if (isFolderCheckFailTrue(workflowScriptContext)) {
            // fail test if wfFolderCheckFail is set
            try {
                workflowScriptContext.doTransition("trigger_check_conflict");
            } catch (IllegalAccessException e) {
                Logging.logError(MSG_WORKFLOW_CHECK_FAILED, e, LOGGER);
            }
        }
        return true;
    }

    private static boolean isFolderCheckFailTrue(WorkflowScriptContext workflowScriptContext) {
        final Object customAttribute = getCustomAttribute(workflowScriptContext, WF_FOLDER_CHECK_FAIL);
        return WorkflowConstants.TRUE.equals(customAttribute);
    }

    /**
     * Checks whether the element on which the workflow is executed, is the workflow itself.
     */
    private static boolean isExecutedWorkflow(IDProvider element, Workflow workflow) {
        String elementUid = element.getUid();
        String workflowUid = workflow.getUid();
        return elementUid.equals(workflowUid);
    }

    /**
     * Returns all open instances from the given workflow element.
     */
    private static Map<Task, IDProvider> getOpenInstances(IDProvider element, Iterable<IDProvider> searchResults) {
        Map<Task, IDProvider> openInstances = new HashMap<Task, IDProvider>();
        for (IDProvider result : searchResults) {
            String workflowUid = result.getTask().getWorkflow().getUid();
            if (workflowUid.equals(element.getUid())) {
                openInstances.put(result.getTask(), result);
            }
        }
        return openInstances;
    }

    /**
     * Close all given workflow instances.
     *
     * @param openInstances open instances
     */
    public void closeOpenInstances(Map<Task, IDProvider> openInstances) {
        for (Map.Entry<Task, IDProvider> entry : openInstances.entrySet()) {
            // remove task
            IDProvider element = entry.getValue();
            try {
                element.setLock(true, true);
                element.removeTask();
                element.save();
                element.setLock(false, true);
                element.refresh();
            } catch (LockException e) {
                Logging.logError(e.getMessage(), e, LOGGER);
            } catch (ElementDeletedException e) {
                Logging.logError(e.getMessage(), e, LOGGER);
            }

            // close task
            Task task = entry.getKey();
            try {
                task.lock();
                task.closeTask();
                task.save();
                task.unlock();
            } catch (LockException e) {
                Logging.logError(e.getMessage(), e, LOGGER);
            }
        }
    }

}
