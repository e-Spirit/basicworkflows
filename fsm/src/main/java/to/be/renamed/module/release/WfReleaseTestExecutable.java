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
package to.be.renamed.module.release;

import to.be.renamed.module.util.AbstractWorkflowExecutable;
import to.be.renamed.module.util.FormEvaluator;
import to.be.renamed.module.util.StoreUtil;
import to.be.renamed.module.util.WorkflowConstants;
import to.be.renamed.module.util.WorkflowSessionHelper;
import com.espirit.moddev.components.annotations.PublicComponent;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.access.ReferenceEntry;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.Store;
import de.espirit.firstspirit.access.store.contentstore.ContentWorkflowable;
import de.espirit.firstspirit.access.store.sitestore.PageRef;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * This class is used to test the release of the workflow object.
 *
 * @author stephan
 * @since 1.0
 */
@PublicComponent(name = "WorkFlow Release Test Executable")
public class WfReleaseTestExecutable extends AbstractWorkflowExecutable {

    /**
     * The logging class to use.
     */
    public static final Class<?> LOGGER = WfReleaseTestExecutable.class;


    @Override
    @SuppressWarnings("unchecked")
    public Object execute(final Map<String, Object> params) {
        final WorkflowScriptContext workflowScriptContext = (WorkflowScriptContext) params.get(WorkflowConstants.CONTEXT);
        final ResourceBundle bundle = loadResourceBundle(workflowScriptContext);

        final WorkflowObject workflowObject = new WorkflowObject(workflowScriptContext);

        final FormEvaluator formEvaluator = new FormEvaluator(workflowScriptContext);
        final boolean releaseWithMedia = formEvaluator.getCheckboxValue(WorkflowConstants.MEDIA_FORM_REFNAME);
        final boolean releaseRecursively = formEvaluator.getCheckboxValue(WorkflowConstants.RECURSIVE_FORM_REFNAME);
        final Language[] languages = formEvaluator.getLanguages();
        workflowObject.setRecursively(releaseRecursively);

        final boolean releaseStatus;
        final List<Object> releaseObjects = new ArrayList<>();

        final List<IDProvider> childrenList = new ArrayList<>();
        final Map<Long, Store.Type>
            childrenIdMap =
            WorkflowSessionHelper.readObjectFromSession(workflowScriptContext, WorkflowConstants.WF_RECURSIVE_CHILDREN);
        childrenList.addAll(loadChildrenList(workflowScriptContext, childrenIdMap));

        final IDProvider releaseElement = workflowScriptContext.getElement();

        // check test case or skip if wfDoTestFail is set
        if (isNotFailedTest(workflowScriptContext)) {
            final Object
                releasePageRefElements =
                WorkflowSessionHelper.readObjectFromSession(workflowScriptContext, WorkflowConstants.RELEASE_PAGEREF_ELEMENTS);
            List<String> releasePageRefUids = null;
            if (releasePageRefElements instanceof List) {
                releasePageRefUids = (List<String>) releasePageRefElements;
            }

            if (releasePageRefUids != null && !releasePageRefUids.isEmpty()) {
                for (final String pageRefUid : releasePageRefUids) {
                    final StoreUtil storeUtil = new StoreUtil(workflowScriptContext);
                    final PageRef pageRef = storeUtil.loadPageRefByUid(pageRefUid);
                    workflowObject.setStoreElement(pageRef);
                    // add referenced elements from pageref excluding the pagerefs retrieved from
                    // the session since they will be added afterwards
                    final Set<Object> refObjectsFromStoreElement = workflowObject.getRefObjectsFromStoreElement(releaseWithMedia, false, languages);
                    addReferencesExcludingPageRefsFromSession(refObjectsFromStoreElement, releasePageRefUids, releaseObjects);
                    // add the pageref (and page)
                    int
                        pageReleaseStatus =
                        languages.length > 0 ? pageRef.getPage().getReleaseStatus(languages) : pageRef.getPage().getReleaseStatus();
                    if (pageReleaseStatus != IDProvider.RELEASED) {
                        releaseObjects.add(pageRef.getPage());
                    }
                    releaseObjects.add(pageRef);
                }
                // do test release
                final ReleaseObject releaseObject = new ReleaseObject(workflowScriptContext, releaseObjects);
                releaseStatus = releaseObject.release(true, releaseRecursively, languages);
            } else if (isStartedOnDatasource(workflowScriptContext)) {
                // do test release of referenced media if checkbox is checked
                releaseObjects.addAll(workflowObject.getRefObjectsFromEntity(releaseWithMedia));
                // do test release
                final ReleaseObject releaseObject = new ReleaseObject(workflowScriptContext, releaseObjects);
                final boolean releaseStatusWithoutEntity = releaseObject.release(true, releaseRecursively, languages);
                // test release entity
                if (releaseStatusWithoutEntity) {
                    // do release
                    final ReleaseObject releaseObjectEntity = new ReleaseObject(workflowScriptContext, Collections.singletonList(releaseElement));
                    releaseStatus = releaseObjectEntity.release(true, releaseRecursively, languages);
                } else {
                    releaseStatus = false;
                }
            } else {
                final List<IDProvider> releaseElementsWithPossibleChildren = new ArrayList();
                releaseElementsWithPossibleChildren.add(releaseElement);

                // add children to list if release recursively is set
                if (releaseRecursively && !childrenList.isEmpty()) {
                    releaseElementsWithPossibleChildren.addAll(childrenList);
                }

                addChildrenToReleaseObjects(releaseWithMedia, workflowObject, releaseRecursively, releaseObjects, releaseElementsWithPossibleChildren,
                                            languages);

                // do test release
                for (final Object releaseObject : releaseObjects) {
                    if (releaseObject instanceof IDProvider) {
                        Logging.logInfo("Release object id: " + ((IDProvider) releaseObject).getId(), LOGGER);
                    }
                }
                // do test release
                final ReleaseObject releaseObject = new ReleaseObject(workflowScriptContext, releaseObjects);
                releaseStatus = releaseObject.release(true, releaseRecursively, languages);
            }
        } else {
            releaseStatus = false;
        }
        // check if test release was successful (check wfDoTestFail for test case)
        if (releaseStatus) {
            try {
                workflowScriptContext.doTransition("trigger_test_finished");
                Logging.logInfo("Workflow Test Release successful.", LOGGER);
            } catch (final IllegalAccessException e) {
                Logging.logError("Workflow Test Release failed!\n", e, LOGGER);
                // show error message
                showDialog(workflowScriptContext, bundle.getString(WorkflowConstants.ERROR_MSG), bundle.getString("testReleaseFailed"));
            }
        } else {
            try {
                workflowScriptContext.doTransition("trigger_test_failed");
            } catch (final IllegalAccessException e) {
                Logging.logError("Workflow Test Release failed!", e, LOGGER);
                // show error message
                showDialog(workflowScriptContext, bundle.getString(WorkflowConstants.ERROR_MSG), bundle.getString("testReleaseFailed"));
            }
        }
        return true;
    }

    private static void addChildrenToReleaseObjects(final boolean releaseWithMedia, final WorkflowObject workflowObject,
                                                    final boolean releaseRecursively, final List<Object> releaseObjects,
                                                    final List<IDProvider> releaseElementsWithPossibleChildren, final Language[] languages) {
        for (final IDProvider storeElement : releaseElementsWithPossibleChildren) {
            // create workflowObject with current releasable storeElement
            workflowObject.setRecursively(releaseRecursively);
            workflowObject.setStoreElement(storeElement);

            // add dependend objects to releaseObjects list
            final Set<Object> refObjects = workflowObject.getRefObjectsFromStoreElement(releaseWithMedia, releaseRecursively, languages);
            releaseObjects.addAll(refObjects);

            // logging id's only
            for (final Object refObject : refObjects) {
                if (refObject instanceof IDProvider) {
                    Logging.logInfo("IDProvider referenced Element with Id '" + ((IDProvider) refObject).getId() + "' added to release list",
                                    LOGGER);
                } else if (refObject instanceof ReferenceEntry) {
                    final IDProvider referencedElement = ((ReferenceEntry) refObject).getReferencedElement();
                    if (referencedElement != null) {
                        Logging.logInfo("IDProvider referenced Element with Id '" + referencedElement.getId() + "' added to release list", LOGGER);
                    } else {
                        Logging.logInfo("ReferenceEntry element is null. Broken Reference?", LOGGER);
                    }
                } else {
                    Logging.logInfo("Element of class '" + refObject.getClass() + "' can't be fetched", LOGGER);
                }
            }

            if (storeElement instanceof PageRef) {
                int releaseStatus =
                    languages.length > 0 ? ((PageRef) storeElement).getPage().getReleaseStatus(languages)
                                         : ((PageRef) storeElement).getPage().getReleaseStatus();

                if (releaseStatus != IDProvider.RELEASED) {
                    // if object is pageref, add page to release list if unreleased
                    releaseObjects.add(((PageRef) storeElement).getPage());
                }
            }

            // add the object itself to releaseObjects list
            if (!releaseObjects.contains(storeElement)) {
                releaseObjects.add(storeElement);
                Logging.logInfo("IDProvider Element with Id '" + storeElement.getId() + "' added to release list", LOGGER);
            }
        }
    }
}
