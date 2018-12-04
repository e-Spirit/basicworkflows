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

import de.espirit.firstspirit.access.ReferenceEntry;
import de.espirit.firstspirit.access.script.Executable;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.Store;
import de.espirit.firstspirit.access.store.contentstore.ContentWorkflowable;
import de.espirit.firstspirit.access.store.sitestore.PageRef;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;
import de.espirit.firstspirit.agency.StoreAgent;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Abstract WorkflowExecutable that contains the basic methods used in all executables.
 *
 * @author stephan
 * @since 1.0
 */
public abstract class AbstractWorkflowExecutable implements Executable {


    /**
     * Is started on a datasource record?.
     *
     * @param workflowScriptContext the workflow script context
     * @return the boolean
     */
    protected static boolean isStartedOnDatasource(final WorkflowScriptContext workflowScriptContext) {
        return workflowScriptContext.getWorkflowable() instanceof ContentWorkflowable;
    }

    /**
     * Adds the elements from objectsToAdd to the resultList list excluding the elements listed in objectsToExclude
     *
     * @param objectsToAdd The objects to release
     * @param objectsToExclude Uids of objects to exclude from list
     * @param resultList The resulting list of objects to release
     */
    protected static void addReferencesExcludingPageRefsFromSession(final Collection<Object> objectsToAdd,
                                                                    final List<String> objectsToExclude, final List<Object> resultList) {
        for (final Object object : objectsToAdd) {
            if (object instanceof ReferenceEntry) {
                final ReferenceEntry refEntry = (ReferenceEntry) object;
                if (refEntry.getReferencedElement() instanceof PageRef) {
                    final PageRef pageRef = (PageRef) refEntry.getReferencedElement();
                    if (objectsToExclude.contains(pageRef.getUid())) {
                        continue;
                    }
                }
            }
            resultList.add(object);
        }
    }

    /**
     * Load resource bundle.
     *
     * @param workflowScriptContext the workflow script context
     * @return the resource bundle
     */
    protected static ResourceBundle loadResourceBundle(final WorkflowScriptContext workflowScriptContext) {
        ResourceBundle.clearCache();
        final FsLocale fsLocale = new FsLocale(workflowScriptContext);
        final Locale locale = fsLocale.get();
        return ResourceBundle.getBundle(WorkflowConstants.MESSAGES, locale);
    }

    @Override
    public final Object execute(final Map<String, Object> args, final Writer out, final Writer err) {
        return execute(args);
    }

    /**
     * A convenience method to display a message pop-up in the client.
     *
     * @param title                 The message title.
     * @param message               The message to display.
     * @param workflowScriptContext The context to use.
     */
    protected void showDialog(final WorkflowScriptContext workflowScriptContext, final String title, final String message) {
        // set in integration tests
        final String suppressDialog = (String) workflowScriptContext.getSession().get(WorkflowConstants.WF_SUPPRESS_DIALOG);
        if (!WorkflowConstants.TRUE.equals(suppressDialog)) {
            final Dialog dialog = new Dialog(workflowScriptContext);
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
    protected boolean showQuestionDialog(final WorkflowScriptContext workflowScriptContext, final String title, final String question) {
        // set in integration tests
        final String suppressDialog = (String) workflowScriptContext.getSession().get(WorkflowConstants.WF_SUPPRESS_DIALOG);
        if (!WorkflowConstants.TRUE.equals(suppressDialog)) {

            final Dialog dialog = new Dialog(workflowScriptContext);

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
    protected static Object getCustomAttribute(final WorkflowScriptContext workflowScriptContext, final String attribute) {
        return workflowScriptContext.getTask().getCustomAttributes().get(attribute);
    }



    protected List<IDProvider> loadChildrenList(final WorkflowScriptContext workflowScriptContext, final Map<Long, Store.Type> childrenIdMap) {
        final List<IDProvider> childrenList = new ArrayList<>();
        if (childrenIdMap != null) {
            final StoreAgent storeAgent = workflowScriptContext.requireSpecialist(StoreAgent.TYPE);
            for (final Map.Entry<Long, Store.Type> childrenId : childrenIdMap.entrySet()) {
                childrenList.add(storeAgent.getStore(childrenId.getValue()).getStoreElement(childrenId.getKey()));
            }
        }
        return childrenList;
    }


    protected static void writeObjectToSession(final WorkflowScriptContext workflowScriptContext, final String key, final Object object) {
        workflowScriptContext.getSession().put(key, object);
    }


    /**
     * Is not failed.
     *
     * @param workflowScriptContext the workflow script context
     * @return the boolean
     */
    protected static boolean isNotFailed(final WorkflowScriptContext workflowScriptContext) {
        final Object wfDoFail = getCustomAttribute(workflowScriptContext, "wfDoFail");
        return wfDoFail == null || WorkflowConstants.FALSE.equals(wfDoFail);
    }

    protected static boolean isNotFailedTest(final WorkflowScriptContext workflowScriptContext) {
        return getCustomAttribute(workflowScriptContext, "wfDoTestFail") == null || WorkflowConstants.FALSE.equals(
            getCustomAttribute(workflowScriptContext, "wfDoTestFail"));
    }
}
