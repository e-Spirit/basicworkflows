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
package com.espirit.moddev.basicworkflows.delete;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.BaseContext;
import de.espirit.firstspirit.access.ReferenceEntry;
import de.espirit.firstspirit.access.store.BasicElementInfo;
import de.espirit.firstspirit.access.store.BasicInfo;
import de.espirit.firstspirit.access.store.ElementDeletedException;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.LockException;
import de.espirit.firstspirit.access.store.ReleaseProblem;
import de.espirit.firstspirit.access.store.Store;
import de.espirit.firstspirit.access.store.StoreElement;
import de.espirit.firstspirit.access.store.StoreElementFilter;
import de.espirit.firstspirit.access.store.contentstore.Content2;
import de.espirit.firstspirit.access.store.contentstore.ContentWorkflowable;
import de.espirit.firstspirit.access.store.mediastore.Media;
import de.espirit.firstspirit.access.store.mediastore.MediaFolder;
import de.espirit.firstspirit.access.store.pagestore.Page;
import de.espirit.firstspirit.access.store.pagestore.PageFolder;
import de.espirit.firstspirit.access.store.sitestore.DocumentGroup;
import de.espirit.firstspirit.access.store.sitestore.PageRef;
import de.espirit.firstspirit.access.store.sitestore.PageRefFolder;
import de.espirit.firstspirit.access.store.sitestore.StartNode;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;
import de.espirit.firstspirit.agency.OperationAgent;
import de.espirit.firstspirit.store.operations.DeleteOperation;
import de.espirit.firstspirit.store.operations.ReleaseOperation;
import de.espirit.or.Session;
import de.espirit.or.schema.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.espirit.firstspirit.access.store.StoreElementFilter.on;

/**
 * This class is used to delete IDProvider/Entity objects.
 *
 * @author stephan
 * @since 1.0
 */
public class DeleteObject {

    /**
     * The Entity to be deleted.
     */
    private Entity entity;
    /**
     * The idProvider to be deleted.
     */
    private IDProvider idProvider;
    /**
     * The workflowScriptContext from the workflow.
     */
    private WorkflowScriptContext workflowScriptContext;
    /**
     * The result of the delete operation, defaults to successful.
     */
    private boolean result = true;
    /**
     * The logging class to use.
     */
    public static final Class<?> LOGGER = DeleteObject.class;
    /**
     * Exception message.
     */
    private static final String EXCEPTION = "Exception during Delete of ";
    /**
     * ID message.
     */
    private static final String ID = "  id:";
    /**
     * Name for variable that holds the objects to release.
     */
    private static final String REL_OBJECTS = "releaseObjects";
    /**
     * Name for variable that holds the objects to delete.
     */
    private static final String DEL_OBJECTS = "deleteObjects";
    /**
     * List of objects that should be deleted.
     */
    private List<IDProvider> deleteObjects = new ArrayList<>();
    /**
     * List of objects that should be released.
     */
    private List<IDProvider> releaseObjects = new ArrayList<>();


    /**
     * Constructor for DeleteObject.
     *
     * @param workflowScriptContext The workflowScriptContext from the workflow.
     */
    DeleteObject(WorkflowScriptContext workflowScriptContext) {

        this.workflowScriptContext = workflowScriptContext;
        // check if content2 object
        if (workflowScriptContext.getWorkflowable() instanceof ContentWorkflowable) {
            ContentWorkflowable contentWorkflowable = (ContentWorkflowable) workflowScriptContext.getWorkflowable();
            entity = contentWorkflowable.getEntity();
        } else {
            idProvider = workflowScriptContext.getElement();
        }
    }


    /**
     * Main delete method, that distinguishes from IDProvider and Entity.
     *
     * @param checkOnly Determines if the method should do only a check or really delete the object.
     * @return true if successful.
     */
    public boolean delete(boolean checkOnly) {
        /* delete entity
           entities cannot be locked, so skip test case
         */
        if (entity != null && !checkOnly) {
            deleteEntity();
        } else if (idProvider != null) {
            deleteIDProvider(checkOnly);
        }
        return result;
    }


    /**
     * Convenience method to delete entities in current/release state.
     */
    private void deleteEntity() {
        final ContentWorkflowable contentWorkflowable = (ContentWorkflowable) workflowScriptContext.getWorkflowable();
        final Content2 content2 = contentWorkflowable.getContent();

        // delete current state
        try {
            Session session = entity.getSession();
            session.delete(entity);
            session.commit();
        } catch (Exception e) {
            Logging.logError(EXCEPTION + entity, e, LOGGER);

        } finally {
            try {
                // lock/unlock content2 to force a refresh
                content2.setLock(true, false);
                content2.save();
                content2.setLock(false, false);
            } catch (LockException e) {
                Logging.logError(EXCEPTION + entity, e, LOGGER);
            } catch (ElementDeletedException e) {
                Logging.logError(EXCEPTION + entity, e, LOGGER);
            }
        }

        // delete release state
        Session releaseSession = content2.getTemplate().getSchema().getSession(true);
        //delete page entity (release state)
        releaseSession.rollback();
        Entity entityRelease = releaseSession.find(entity.getKeyValue());
        if (entityRelease != null) {
            // if in release store
            try {
                releaseSession.delete(entityRelease);
                releaseSession.commit();
            } catch (Exception e) {
                Logging.logError(EXCEPTION + entityRelease, e, LOGGER);
                result = false;
            }
        }
    }


    /**
     * Handler for deleting IDProvider objects, including release (for webedit workflows).
     *
     * @param checkOnly Determines if the method should do only a check or really delete the object.
     */
    private void deleteIDProvider(boolean checkOnly) {
        Logging.logInfo("deleteIDProvider(" + checkOnly + ")", getClass());
        Map<String, List<IDProvider>> elementList = getDeleteElements();
        final List<IDProvider> listOfObjectsToDelete = elementList.get(DEL_OBJECTS);
        if (checkOnly) {
            List<IDProvider> lockedElements = new ArrayList<>();
            if (listOfObjectsToDelete != null) {
                for (IDProvider idProv : listOfObjectsToDelete) {
                    if (idProv.isLockedOnServer(true) && !idProv.isLocked()) {
                        // element is locked on server from different session, delete not possible
                        lockedElements.add(idProv);
                        result = false;
                    }
                }
            }
            if (elementList.get(REL_OBJECTS) != null) {
                for (IDProvider idProv : elementList.get(REL_OBJECTS)) {
                    if (idProv.isLockedOnServer(true) && !idProv.isLocked()) {
                        // element is locked on server from different session, delete not possible
                        result = false;
                        lockedElements.add(idProv);
                    }
                }
            }
            storeReferences(lockedElements);
        } else {
            final boolean listContainsItems = listOfObjectsToDelete != null && !listOfObjectsToDelete.isEmpty();
            Logging.logInfo("listContainsItems: " + listContainsItems, getClass());
            if (listContainsItems) {
                deleteElements(listOfObjectsToDelete);
            }
            if (elementList.get(REL_OBJECTS) != null && !elementList.get(REL_OBJECTS).isEmpty()) {
                releaseElements(elementList.get(REL_OBJECTS));
            }
        }
    }

    /**
     * Convenience method to delete IDProvider objects.
     *
     * @param deleteObjects The list of IDProvider objects to delete.
     */
    private void deleteElements(List<IDProvider> deleteObjects) {

        Logging.logInfo("deleteElements: " + deleteObjects.size(), getClass());

        // lock workflow element first
        try {
            workflowScriptContext.getElement().setLock(false, false);
        } catch (LockException e) {
            Logging.logError(EXCEPTION + idProvider, e, LOGGER);
        } catch (ElementDeletedException e) {
            Logging.logError(EXCEPTION + idProvider, e, LOGGER);
        }

        // delete elements

        DeleteOperation.Result deleteResult = deleteIgnoringReferences(deleteObjects);
        if(deleteResult != null) {
            result = deleteResult.isSuccessful();
            final Set<BasicInfo> lockedFailed = deleteResult.getLockFailedElements();
            final Set<BasicInfo> missingPermission = deleteResult.getMissingPermissionElements();
            Logging.logInfo("Release Result: " + deleteResult.isSuccessful(), LOGGER);
            Logging.logInfo("Released Elements:", LOGGER);
            for (BasicInfo basicInfo : deleteResult.getDeletedElements()) {
                if (!basicInfo.isEntity()) {
                    Logging.logInfo(ID + ((BasicElementInfo) basicInfo).getNodeId(), LOGGER);
                }
            }
            handleLocksAndPermissions(lockedFailed, missingPermission);
        }
        workflowScriptContext.getTask().closeTask();
        workflowScriptContext.getElement().refresh();
    }

    private void handleLocksAndPermissions(Set<BasicInfo> lockedFailed,
                                           Set<BasicInfo> missingPermission) {
        if (lockedFailed != null && !lockedFailed.isEmpty()) {
            Logging.logInfo("LockFailedElements:", LOGGER);
            for (BasicInfo locked : lockedFailed) {
                if(!locked.isEntity()) {
                    Logging.logInfo(ID + ((BasicElementInfo) locked).getNodeId(), LOGGER);
                }
            }
        }
        if (missingPermission != null && !missingPermission.isEmpty()) {
            Logging.logInfo("MissingPermissionElement", LOGGER);

            for (BasicInfo missing : missingPermission) {
                if(!missing.isEntity()) {
                    Logging.logInfo(ID + ((BasicElementInfo) missing).getNodeId(), LOGGER);
                }
            }
        }
    }

    // Needed in Tests
    protected DeleteOperation.Result deleteIgnoringReferences(final List<IDProvider> deleteObjects) {

        OperationAgent operationAgent = workflowScriptContext.requireSpecialist(OperationAgent.TYPE);
        DeleteOperation deleteOperation = operationAgent.getOperation(DeleteOperation.TYPE);
        DeleteOperation.Result deleteResult = null;
        try {
            IDProvider[] idProviderArr = new IDProvider[deleteObjects.size()];
            deleteResult = deleteOperation.ignoreIncomingReferences(true).perform(deleteObjects.toArray(idProviderArr));
        } catch (Exception e) {
            Logging.logError("Failed to deleted objects: " + e, LOGGER);
        }
        return deleteResult;
    }


    /**
     * Convenience method to release IDProvider objects (used in webedit context).
     *
     * @param releaseObjects The list of IDProvider objects to release.
     */
    private void releaseElements(List<IDProvider> releaseObjects) {
        // release parent elements (only used in webedit workflow)
        ReleaseOperation.ReleaseResult releaseResult;
        for (IDProvider idProv : releaseObjects) {
            releaseResult = releaseWithAccessibilityAndNewOnly(idProv);
            if(releaseResult != null) {
                final Set<BasicInfo> lockedFailed = releaseResult.getProblematicElements().get(ReleaseProblem.LOCK_FAILED);
                final Set<BasicInfo> missingPermission = releaseResult.getProblematicElements().get(ReleaseProblem.MISSING_PERMISSION);
                Logging.logInfo("Release Result: " + releaseResult.isSuccessful(), LOGGER);
                Logging.logInfo("Released Elements:", LOGGER);
                result = releaseResult.isSuccessful();
                for (BasicInfo basicInfo : releaseResult.getReleasedElements()) {
                    if (!basicInfo.isEntity()) {
                        Logging.logInfo(ID + ((BasicElementInfo) basicInfo).getNodeId(), LOGGER);
                    }
                }
                handleLocksAndPermissions(lockedFailed, missingPermission);
            }

            // release new startnode (if modified through delete action)
            if (idProv instanceof PageRefFolder) {
                StartNode startNode = ((PageRefFolder) idProv).getStartNode();
                if (startNode != null && startNode.getReleaseStatus() != IDProvider.RELEASED) {
                    List<IDProvider> startNodeList = new ArrayList<>();
                    startNodeList.add(startNode);
                    releaseElements(startNodeList);
                }
            }

        }
    }

    protected ReleaseOperation.ReleaseResult releaseWithAccessibilityAndNewOnly(final IDProvider idProv) {
        OperationAgent operationAgent = workflowScriptContext.requireSpecialist(OperationAgent.TYPE);
        ReleaseOperation releaseOperation = operationAgent.getOperation(ReleaseOperation.TYPE);
        ReleaseOperation.ReleaseResult releaseResult = null;
        try {
            releaseResult = releaseOperation.checkOnly(false).ensureAccessibility(true).recursive(false).dependentReleaseType(IDProvider.DependentReleaseType.DEPENDENT_RELEASE_NEW_ONLY).perform(idProv);
        } catch (Exception e) {
            Logging.logInfo("Failed to release IdProvider with id: " + idProv.getId() +"\n" + e, LOGGER);
        }
        return releaseResult;
    }


    /**
     * Gets delete and release elements, as the result is different for pagerefs in JC/Webedit. In case of JC: The element will be deleted. The parent
     * folder in the page-/sitestore are released. In case of Webedit: The pageref will be deleted. If the pageref is the last node in the folder, the
     * folder is deleted. The page will be deleted if it is no longer used. If the page is the last node in the folder, the folder is deleted
     * (up-recursive). The parent folder in the page-/sitestore are released.
     *
     * @return a map with IDProvider objects to delete.
     */
    private Map<String, List<IDProvider>> getDeleteElements() {
        Map<String, List<IDProvider>> deleteElements = new HashMap<>();

        // Webedit
        if (workflowScriptContext.is(BaseContext.Env.WEBEDIT)) {
            if (idProvider instanceof PageRef) {
                // add Page and PageFolder if no longer referenced
                regardPageStore();
                // add current PageRefStore element and PageRefFolders (up-recursive)
                regardPageRefStore();
            }
            if (idProvider instanceof DocumentGroup) {
                // add current PageRefStore element and PageRefFolders (up-recursive)
                regardPageRefStore();
            }
            //Added for media management in CC (since FS 5.2)
            if (idProvider instanceof Media || idProvider instanceof MediaFolder) {
                //false == don't delete parent folder, see FSFIVE-53
                final boolean deleteEmptyParent = true;
                // add current MediaElement element and MediaFolder (up-recursive)
                regardMediaStore(deleteEmptyParent);
            }
        } else {
            // JC
            deleteObjects.add(idProvider);

            if (idProvider.getStore().getType() != Store.Type.TEMPLATESTORE) {
                // release parent folder
                releaseObjects.add(idProvider.getParent());
            }
        }
        deleteElements.put(DEL_OBJECTS, deleteObjects);
        deleteElements.put(REL_OBJECTS, releaseObjects);

        return deleteElements;
    }

    /**
     * This method adds the current PageRefStore element and PageFolder to the elements that should be deleted if they are no longer referenced.
     */
    private void regardPageRefStore() {
        // add current element
        deleteObjects.add(idProvider);
        // add PagerefFolder (up-recursive) if no child elements are present
        final StoreElementFilter filter = on(PageRefFolder.class, PageRef.class, DocumentGroup.class);
        checkParentPath(idProvider, filter);
    }

    /**
     * This method adds the Page and PageFolder to the elements that should be deleted if they are no longer referenced.
     */
    private void regardPageStore() {
        IDProvider element = ((PageRef) idProvider).getPage();
        // only referenced in pageref that will be deleted
        if (element.getIncomingReferences().length == 1) {
            // add page
            deleteObjects.add(element);
            // delete PageFolder if last child is being deleted
            final StoreElementFilter filter = on(PageFolder.class, Page.class);
            checkParentPath(element, filter);
        }
    }

    /**
     * New since media management in CC added in FS 5.2.
     */
    private void regardMediaStore(final boolean deleteEmptyParent) {
        deleteObjects.add(idProvider);
        if (deleteEmptyParent) {
            // add Media (up-recursive) if no child elements are present
            final StoreElementFilter filter = on(MediaFolder.class, Media.class);
            checkParentPath(idProvider, filter);
        }
    }

    private void checkParentPath(final IDProvider childElement, final StoreElementFilter filter) {
        IDProvider element = childElement;
        while (element.getParent() != null) {
            element = element.getParent();
            final ReferenceEntry[] incomingReferences = element.getIncomingReferences();
            // only use elements that are no longer in use
            if(incomingReferences.length == 0) {
                Logging.logInfo("Checking parent element " + element.getUid() + " of child element " + childElement.getUid(), LOGGER);
                Iterator<StoreElement> iter = element.getChildren(filter, false).iterator();
                // folder has at least one element -- our child
                iter.next();
                // check if there are more
                if (!iter.hasNext()) {
                    Logging.logInfo("parent element has no children - add to delete objects", LOGGER);
                    deleteObjects.add(element);
                } else {
                    Logging.logInfo("parent element has children - abort", LOGGER);
                    releaseObjects.add(element);
                    break;
                }
            } else {
                Logging.logInfo("element has incoming references - abort", LOGGER);
                releaseObjects.add(element);
                break;
            }
        }
    }


    /**
     * Convenience method to store uid names of objects that reference the object to be deleted in the workflow session.
     *
     * @param lckObjects A list of IDProvider objects that reference the object to be deleted.
     */
    private void storeReferences(List<IDProvider> lckObjects) {
        List<List<String>> lockedObjects = new ArrayList<>();
        for (IDProvider idProv : lckObjects) {
            List<String> lockedObjectList = new ArrayList<>();
            lockedObjectList.add(idProv.getElementType());
            if (idProv.hasUid()) {
                lockedObjectList.add(idProv.getUid());
            } else {
                lockedObjectList.add(idProv.getName());
            }
            lockedObjects.add(lockedObjectList);
        }
        // put referenced objects to session for further use
        workflowScriptContext.getSession().put("wfLockedObjects", lockedObjects);
    }
}