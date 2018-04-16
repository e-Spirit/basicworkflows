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
import com.espirit.moddev.basicworkflows.util.StoreUtil;
import com.espirit.moddev.basicworkflows.util.WorkflowConstants;
import com.espirit.moddev.basicworkflows.util.WorkflowSessionHelper;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.ReferenceEntry;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.Store;
import de.espirit.firstspirit.access.store.contentstore.ContentWorkflowable;
import de.espirit.firstspirit.access.store.sitestore.PageRef;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * This class is used to actually release the workflow object.
 *
 * @author stephan
 * @since 1.0
 */
public class WfReleaseExecutable extends AbstractWorkflowExecutable {

    /**
     * The logging class to use.
     */
    public static final Class<?> LOGGER = WfReleaseExecutable.class;


    @Override
    @SuppressWarnings("unchecked")
    public Object execute(final Map<String, Object> params) {
        final WorkflowScriptContext workflowScriptContext = (WorkflowScriptContext) params.get(WorkflowConstants.CONTEXT);
        final ResourceBundle bundle = loadResourceBundle(workflowScriptContext);

        final WorkflowObject workflowObject = new WorkflowObject(workflowScriptContext);

        final FormEvaluator formEvaluator = new FormEvaluator(workflowScriptContext);
        final boolean releaseWithMedia = formEvaluator.getCheckboxValue(WorkflowConstants.MEDIA_FORM_REFNAME);
        final boolean releaseRecursively = formEvaluator.getCheckboxValue(WorkflowConstants.RECURSIVE_FORM_REFNAME);
        workflowObject.setRecursively(releaseRecursively);

        final boolean releaseStatus;
        final List<Object> releaseObjects = new ArrayList<>();

        final List<IDProvider> childrenList = new ArrayList<>();
		final Map<Long, Store.Type> childrenIdMap = WorkflowSessionHelper.readObjectFromSession(workflowScriptContext, WorkflowConstants.WF_RECURSIVE_CHILDREN);
        childrenList.addAll(loadChildrenList(workflowScriptContext, childrenIdMap));

        final IDProvider releaseElement = workflowScriptContext.getElement();

        // check test case or skip if wfDoFail is set
        if (isNotFailed(workflowScriptContext)) {
			final Object releasePageRefElements = WorkflowSessionHelper.readObjectFromSession(workflowScriptContext, WorkflowConstants.RELEASE_PAGEREF_ELEMENTS);
            List<String> releasePageRefUids = null;
            if (releasePageRefElements != null && releasePageRefElements instanceof List) {
                releasePageRefUids = (List<String>) releasePageRefElements;
            }

            if (releasePageRefUids != null && !releasePageRefUids.isEmpty()) {
                for (final String pageRefUid : releasePageRefUids) {
                    final StoreUtil storeUtil = new StoreUtil(workflowScriptContext);
                    final PageRef pageRef = storeUtil.loadPageRefByUid(pageRefUid);
                    workflowObject.setStoreElement(pageRef);
                    // add referenced elements from pageref excluding the pagerefs retrieved from
                    // the session since they will be added afterwards
                    final Set<Object> refObjectsFromStoreElement = workflowObject.getRefObjectsFromStoreElement(releaseWithMedia, false);
                    addReferencesExcludingPageRefsFromSession(refObjectsFromStoreElement, releasePageRefUids, releaseObjects);
                    // add the pageref (and page)
                    if ((pageRef.getPage()).getReleaseStatus() != IDProvider.RELEASED) {
                        releaseObjects.add(pageRef.getPage());
                    }
                    releaseObjects.add(pageRef);
                }
                // do release
                final ReleaseObject releaseObject = new ReleaseObject(workflowScriptContext, releaseObjects);
                releaseStatus = releaseObject.release(false, releaseRecursively);

            } else if (isStartedOnDatasource(workflowScriptContext)) {
                // do release of referenced media if checkbox is checked
                final Set<Object> refObjectsFromEntity = workflowObject.getRefObjectsFromEntity(releaseWithMedia);
                releaseObjects.addAll(refObjectsFromEntity);
                final ReleaseObject releaseObject = new ReleaseObject(workflowScriptContext, releaseObjects);
                final boolean releaseStatusWithoutEntity = releaseObject.release(false, releaseRecursively);
                // release entity
                if (releaseStatusWithoutEntity) {
                    final ContentWorkflowable contentWorkflowable = (ContentWorkflowable) workflowScriptContext.getWorkflowable();
                    // do release
                    final ReleaseObject releaseObjectWithEntity = new ReleaseObject(workflowScriptContext, contentWorkflowable.getEntity());
                    releaseStatus = releaseObjectWithEntity.release(false, releaseRecursively);
                } else {
                    releaseStatus = false;
                }

            } else {
                final List<IDProvider> releaseElementsWithPossibleChildren = new ArrayList<>();
                releaseElementsWithPossibleChildren.add(releaseElement);

                // add children to list if release recursively is set
                if (releaseRecursively && !childrenList.isEmpty()) {
                    releaseElementsWithPossibleChildren.addAll(childrenList);
                }

                addChildrenToReleaseObjects(workflowObject, releaseWithMedia, releaseRecursively, releaseObjects,
                                            releaseElementsWithPossibleChildren);

                // do release
                final ReleaseObject releaseObject = new ReleaseObject(workflowScriptContext, releaseObjects);
                releaseStatus = releaseObject.release(false, releaseRecursively);
            }
        } else {
            releaseStatus = false;
        }
        // check if release was successful (check wfDoFail for test case)
        if (releaseStatus) {
            try {
                // refresh workflow object
                if (isStartedOnDatasource(workflowScriptContext)) {
                    ((ContentWorkflowable) workflowScriptContext.getWorkflowable()).getEntity().refresh();
                } else {
                    workflowScriptContext.getElement().refresh();
                }
                // do final transition
                workflowScriptContext.doTransition("trigger_finish");
                Logging.logInfo("Workflow Release successful.", LOGGER);
            } catch (final IllegalAccessException e) {
                Logging.logError("Workflow Release failed!", e, LOGGER);
                // show error message
                showDialog(workflowScriptContext, bundle.getString(WorkflowConstants.ERROR_MSG), bundle.getString("releaseFailed"));
            }
        } else {
            try {
                workflowScriptContext.doTransition("trigger_release_failed");
            } catch (final IllegalAccessException e) {
                Logging.logError("Workflow Release failed!", e, LOGGER);
                // show error message
                showDialog(workflowScriptContext, bundle.getString(WorkflowConstants.ERROR_MSG), bundle.getString("releaseFailed"));
            }
        }
        return true;
    }

    private static void addChildrenToReleaseObjects(final WorkflowObject workflowObject, final boolean releaseWithMedia, final boolean releaseRecursively,
                                                    final List<Object> releaseObjects, final List<IDProvider> releaseElementsWithPossibleChildren) {
        for (final IDProvider storeElement : releaseElementsWithPossibleChildren) {
            // create workflowObject with current releasable storeElement
            workflowObject.setRecursively(releaseRecursively);
            workflowObject.setStoreElement(storeElement);

            Logging.logInfo("IDProvider Element with Id '" + storeElement.getId() + "' added to release list", LOGGER);
            // add dependend objects to releaseObjects list
            final Set<Object> refObjects = workflowObject.getRefObjectsFromStoreElement(releaseWithMedia, releaseRecursively);
            releaseObjects.addAll(refObjects);

            // logging id's only
            for (final Object refObject : refObjects) {
                if (refObject instanceof IDProvider) {
                    Logging.logInfo("IDProvider referenced Element with Id '" + ((IDProvider) refObject).getId() + "' added to release list",
                            LOGGER);
                } else if (refObject instanceof ReferenceEntry) {
                    final IDProvider referencedElement = ((ReferenceEntry) refObject).getReferencedElement();
                    if (referencedElement != null) {
                        Logging.logInfo("IDProvider referenced Element with Id '" + referencedElement.getId()
                                        + "' added to release list", LOGGER);
                    } else {
                        Logging.logInfo("ReferenceEntry element is null. Broken Reference?", LOGGER);
                    }
                } else {
                    Logging.logInfo("Element of class '" + refObject.getClass().toString() + "' can't be fetched", LOGGER);
                }
            }


            if (storeElement instanceof PageRef && (((PageRef) storeElement).getPage()).getReleaseStatus() != IDProvider.RELEASED) {
                // if object is pageref, add page to release list if unreleased
                releaseObjects.add(((PageRef) storeElement).getPage());
            }

            // add the object itself to releaseObjects list
            if (!releaseObjects.contains(storeElement)) {
                releaseObjects.add(storeElement);
            }
        }
    }

}
