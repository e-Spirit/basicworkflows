/*-
 * ========================LICENSE_START=================================
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
 * =========================LICENSE_END==================================
 */
package com.espirit.moddev.basicworkflows.release;

import com.espirit.moddev.basicworkflows.util.AbstractWorkflowExecutable;
import com.espirit.moddev.basicworkflows.util.FormEvaluator;
import com.espirit.moddev.basicworkflows.util.ReferenceResult;
import com.espirit.moddev.basicworkflows.util.StoreUtil;
import com.espirit.moddev.basicworkflows.util.WorkflowConstants;
import com.espirit.moddev.basicworkflows.util.WorkflowSessionHelper;

import de.espirit.common.base.Logging;
import de.espirit.common.util.Listable;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.Store;
import de.espirit.firstspirit.access.store.StoreElement;
import de.espirit.firstspirit.access.store.StoreElementFilter;
import de.espirit.firstspirit.access.store.StoreElementFolder;
import de.espirit.firstspirit.access.store.globalstore.GCAFolder;
import de.espirit.firstspirit.access.store.mediastore.MediaFolder;
import de.espirit.firstspirit.access.store.pagestore.PageFolder;
import de.espirit.firstspirit.access.store.sitestore.PageRef;
import de.espirit.firstspirit.access.store.sitestore.PageRefFolder;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * This class is used to find the related objects of the workflow object and to determine if it can
 * be released.
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
    @SuppressWarnings("unchecked")
    public Object execute(final Map<String, Object> params) {
        final WorkflowScriptContext workflowScriptContext = (WorkflowScriptContext) params.get(WorkflowConstants.CONTEXT);
		removeOldValuesFromSession(workflowScriptContext);
        final StoreUtil storeUtil = new StoreUtil(workflowScriptContext);

        final IDProvider releaseElement = workflowScriptContext.getElement();

        final ResourceBundle bundle = loadResourceBundle(workflowScriptContext);
        boolean isReleasable = true;

        final FormEvaluator formEvaluator = new FormEvaluator(workflowScriptContext);
        boolean releaseRecursively = formEvaluator.getCheckboxValue(WorkflowConstants.RECURSIVE_FORM_REFNAME);

        // for recursive release, write children to session
        if (releaseRecursively) {
            if (releaseElement instanceof PageFolder || releaseElement instanceof MediaFolder || releaseElement instanceof PageRefFolder
                    || releaseElement instanceof GCAFolder) {
                final Map<Long, Store.Type> childrenIdMap = getChildrenOf(releaseElement, storeUtil);
                Logging.logDebug("write IdMap: " + childrenIdMap.toString(), LOGGER);
                writeObjectToSession(workflowScriptContext, WorkflowConstants.WF_RECURSIVE_CHILDREN, childrenIdMap);
            } else {
                releaseRecursively = false;
                Logging.logWarning("Release start node is no folder! Release recursively set back to false!", LOGGER);
            }
        }

        // For A/B Testing it is possible to add an additional Workflow step that collects FirstSpirit Objects to release (i.e. Variants)
		final Object relatedPageRefElements = WorkflowSessionHelper.readObjectFromSession(workflowScriptContext, WorkflowConstants.RELATED_PAGEREF_ELEMENTS);
        List<String> relatedPageRefUids = null;
        if (relatedPageRefElements != null && relatedPageRefElements instanceof List) {
            relatedPageRefUids = (List<String>) relatedPageRefElements;
        }

        // Check the collected FirstSpirit objects otherwise proceed as normal
        if (relatedPageRefUids != null && !relatedPageRefUids.isEmpty()) {
            for (final String pageRefUid : relatedPageRefUids) {
                final PageRef pageRef = new StoreUtil(workflowScriptContext).loadPageRefByUid(pageRefUid);
                if (hasReleaseIssues(workflowScriptContext, pageRef)) {
                    isReleasable = false;
                }
            }
        } else {
            // check if current element is releasable
            if (hasReleaseIssues(workflowScriptContext, workflowScriptContext.getElement())) {
                isReleasable = false;
            }
            if (releaseRecursively) {
                final List<IDProvider> childrenList = new ArrayList<>();
				final Map<Long, Store.Type> childrenIdMap = WorkflowSessionHelper.readObjectFromSession(workflowScriptContext, WorkflowConstants.WF_RECURSIVE_CHILDREN);
                childrenList.addAll(loadChildrenList(workflowScriptContext, childrenIdMap));
                for (final IDProvider idProvider : childrenList) {
                    isReleasable = !hasReleaseIssues(workflowScriptContext, idProvider) && isReleasable;
                }
            }
        }

        if (isReleasable) {
            Logging.logInfo("Can be released", LOGGER);
            try {
                workflowScriptContext.doTransition("trigger_reference_ok");
            } catch (final IllegalAccessException e) {
                Logging.logError("Workflow Release failed!", e, LOGGER);
                // show error message
                showDialog(workflowScriptContext, bundle.getString(WorkflowConstants.ERROR_MSG), bundle.getString(WorkflowConstants.RELEASE_FAILED));
            }
        } else {
            Logging.logWarning("Cannot be released!", LOGGER);
            try {
                workflowScriptContext.doTransition("trigger_release_conflict");
            } catch (final IllegalAccessException e) {
                Logging.logError("Workflow Release failed!", e, LOGGER);
                // show error message
                showDialog(workflowScriptContext, bundle.getString(WorkflowConstants.ERROR_MSG), bundle.getString(WorkflowConstants.RELEASE_FAILED));
            }
        }
        return true;
    }


	private void removeOldValuesFromSession(WorkflowScriptContext workflowScriptContext) {
        Map<Object, Object> workflowSession = workflowScriptContext.getSession();
		workflowSession.remove(WorkflowConstants.WF_NOT_RELEASED_ELEMENTS);
		workflowSession.remove(WorkflowConstants.WF_BROKEN_REFERENCES);
	}


    @NotNull
    private static Map<Long, Store.Type> getChildrenOf(final IDProvider releaseElement, final StoreUtil storeUtil) {
        final StoreElementFilter filter;
        final Map<Long, Store.Type> childrenIdMap = new HashMap<>();

        filter = storeUtil.getChildrenSpecificFilter((StoreElementFolder) releaseElement);
        final Listable<StoreElement> childrenListable = releaseElement.getChildren(filter, true);

        // get referenced objects from child elements and add them to list if release
        // recursively is set true
        for (final StoreElement child : childrenListable) {
            final IDProvider childIdProvider = (IDProvider) child;
            childrenIdMap.put(childIdProvider.getId(), childIdProvider.getStore().getType());
            Logging.logInfo("IDProvider Element with Id '" + childIdProvider.getId() + "' added to release list", LOGGER);
        }
        return childrenIdMap;
    }

    /**
     * Checks if the referenced objects of the supplied idProvider can be released.
     * In case of a recursive release additionally checks the idProvider itself.
     * @param workflowScriptContext the context to use.
     * @param idProvider to check.
     * @return true if there will be some issues during release.
     */
    private static boolean hasReleaseIssues(final WorkflowScriptContext workflowScriptContext, final IDProvider idProvider) {
        final WorkflowObject workflowObject = new WorkflowObject(workflowScriptContext);

        final FormEvaluator formEvaluator = new FormEvaluator(workflowScriptContext);
        final boolean releaseRecursively = formEvaluator.getCheckboxValue(WorkflowConstants.RECURSIVE_FORM_REFNAME);
        workflowObject.setRecursively(releaseRecursively);

        final ArrayList<Object> referencedObjects = new ArrayList<>();
        if (idProvider != null) {
            workflowObject.setStoreElement(idProvider);
        }

        if (isStartedOnDatasource(workflowScriptContext)) {
            referencedObjects.addAll(workflowObject.getRefObjectsFromEntity(true));
        } else {
            referencedObjects.addAll(workflowObject.getRefObjectsFromStoreElement(true, false));
        }

        // check element itself in case of a recursive release, otherwise the element gets already checked by the elementStatusProvider.
        if(releaseRecursively && idProvider != workflowScriptContext.getElement()) {
            referencedObjects.add(idProvider);
        }

        final boolean releaseWithMedia = formEvaluator.getCheckboxValue("wf_releasewmedia");

        final ReferenceResult referenceResult = workflowObject.checkReferences(referencedObjects, releaseWithMedia);

        return referenceResult.hasReleaseIssues(releaseWithMedia);
    }
}
