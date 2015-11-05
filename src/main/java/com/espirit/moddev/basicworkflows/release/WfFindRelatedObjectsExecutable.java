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
import com.espirit.moddev.basicworkflows.util.FormEvaluator;
import com.espirit.moddev.basicworkflows.util.FsLocale;
import com.espirit.moddev.basicworkflows.util.ReferenceResult;
import com.espirit.moddev.basicworkflows.util.StoreUtil;
import com.espirit.moddev.basicworkflows.util.WorkflowConstants;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.contentstore.ContentWorkflowable;
import de.espirit.firstspirit.access.store.sitestore.PageRef;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * This class is used to find the related objects of the workflow object and to determine if it can be released.
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
    public Object execute(Map<String, Object> params) {
        WorkflowScriptContext workflowScriptContext = (WorkflowScriptContext) params.get(WorkflowConstants.CONTEXT);
        ResourceBundle.clearCache();
        final ResourceBundle bundle = ResourceBundle.getBundle(WorkflowConstants.MESSAGES, new FsLocale(workflowScriptContext).get());
        Boolean isReleasable = true;

        Object relatedPageRefElements = readObjectFromSession(workflowScriptContext, WorkflowConstants.RELATED_PAGEREF_ELEMENTS);
        List<String> relatedPageRefUids = null;
        if(relatedPageRefElements != null) {
            relatedPageRefUids = (List<String>) relatedPageRefElements;
        }

        if (relatedPageRefUids != null && !relatedPageRefUids.isEmpty()) {
            for (String pageRefUid : relatedPageRefUids) {
                PageRef pageRef = new StoreUtil(workflowScriptContext).loadPageRefByUid(pageRefUid);
                if (hasReleaseIssues(workflowScriptContext, pageRef)) {
                    isReleasable = false;
                }
            }
        } else {
            // check if current element is releasable
            if (hasReleaseIssues(workflowScriptContext, workflowScriptContext.getElement())) {
                isReleasable = false;
            }
        }

        if (isReleasable) {
            Logging.logInfo("Can be released", LOGGER);
            try {
                workflowScriptContext.doTransition("trigger_reference_ok");
            } catch (IllegalAccessException e) {
                Logging.logError("Workflow Release failed!", e, LOGGER);
                // show error message
                showDialog(workflowScriptContext, bundle.getString(WorkflowConstants.ERROR_MSG), bundle.getString(WorkflowConstants.RELEASE_FAILED));
            }
        } else {
            Logging.logWarning("Cannot be released!", LOGGER);
            try {
                workflowScriptContext.doTransition("trigger_release_conflict");
            } catch (IllegalAccessException e) {
                Logging.logError("Workflow Release failed!", e, LOGGER);
                // show error message
                showDialog(workflowScriptContext, bundle.getString(WorkflowConstants.ERROR_MSG), bundle.getString(WorkflowConstants.RELEASE_FAILED));
            }
        }
        return true;
    }

    private static boolean hasReleaseIssues(WorkflowScriptContext workflowScriptContext, IDProvider idProvider) {
        final WorkflowObject workflowObject = new WorkflowObject(workflowScriptContext);
        ArrayList<Object> referencedObjects = new ArrayList<Object>();
        if (idProvider != null) {
            workflowObject.setStoreElement(idProvider);
        }

        if (workflowScriptContext.getWorkflowable() != null && workflowScriptContext.getWorkflowable() instanceof ContentWorkflowable) {
            referencedObjects.addAll(workflowObject.getRefObjectsFromEntity(true));
        } else {
            referencedObjects.addAll(workflowObject.getRefObjectsFromStoreElement(true));
        }

        FormEvaluator formEvaluator = new FormEvaluator(workflowScriptContext);
        boolean releaseWithMedia = formEvaluator.getCheckboxValue("wf_releasewmedia");

        final ReferenceResult referenceResult = workflowObject.checkReferences(referencedObjects, releaseWithMedia);

        return referenceResult.hasReleaseIssues(releaseWithMedia);
    }


}
