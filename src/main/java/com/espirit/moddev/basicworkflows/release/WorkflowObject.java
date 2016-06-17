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

import com.espirit.moddev.basicworkflows.util.FsException;
import com.espirit.moddev.basicworkflows.util.FsLocale;
import com.espirit.moddev.basicworkflows.util.ReferenceResult;
import com.espirit.moddev.basicworkflows.util.WorkflowConstants;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.BaseContext;
import de.espirit.firstspirit.access.ReferenceEntry;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.StoreElement;
import de.espirit.firstspirit.access.store.contentstore.Content2;
import de.espirit.firstspirit.access.store.contentstore.ContentFolder;
import de.espirit.firstspirit.access.store.contentstore.ContentWorkflowable;
import de.espirit.firstspirit.access.store.globalstore.GCAFolder;
import de.espirit.firstspirit.access.store.globalstore.GCAPage;
import de.espirit.firstspirit.access.store.globalstore.ProjectProperties;
import de.espirit.firstspirit.access.store.mediastore.Media;
import de.espirit.firstspirit.access.store.mediastore.MediaFolder;
import de.espirit.firstspirit.access.store.pagestore.Content2Section;
import de.espirit.firstspirit.access.store.pagestore.Page;
import de.espirit.firstspirit.access.store.pagestore.PageFolder;
import de.espirit.firstspirit.access.store.pagestore.Section;
import de.espirit.firstspirit.access.store.sitestore.DocumentGroup;
import de.espirit.firstspirit.access.store.sitestore.PageRef;
import de.espirit.firstspirit.access.store.sitestore.PageRefFolder;
import de.espirit.firstspirit.access.store.templatestore.Query;
import de.espirit.firstspirit.access.store.templatestore.TemplateStoreElement;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;
import de.espirit.or.schema.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * This class provides methods to get the references of the workflow object and store them in the session.
 *
 * @author stephan
 * @since 1.0
 */
public class WorkflowObject {

    /**
     * The storeElement to use.
     */
    private StoreElement storeElement;
    /**
     * The workflowScriptContext from the workflow.
     */
    private WorkflowScriptContext workflowScriptContext;
    /**
     * The content2 object from the workflow.
     */
    private Content2 content2;
    /**
     * The Entity to use.
     */
    private Entity entity;
    /**
     * The ResourceBundle that contains language specific labels.
     */
    private ResourceBundle bundle;
    /**
     * The logging class to use.
     */
    public static final Class<?> LOGGER = WorkflowObject.class;

    /**
     * Constructor for WorkflowObject.
     *
     * @param workflowScriptContext The workflowScriptContext from the workflow.
     */
    public WorkflowObject(final WorkflowScriptContext workflowScriptContext) {
        this.workflowScriptContext = workflowScriptContext;
        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(WorkflowConstants.MESSAGES, new FsLocale(workflowScriptContext).get());
        if (workflowScriptContext.getWorkflowable() instanceof ContentWorkflowable) {
            content2 = ((ContentWorkflowable) workflowScriptContext.getWorkflowable()).getContent();
            entity = ((ContentWorkflowable) workflowScriptContext.getWorkflowable()).getEntity();
        } else {
            storeElement = (StoreElement) workflowScriptContext.getWorkflowable();
        }
    }

    /**
     * This method gets the referenced objects from the workflow object (StoreElement) that prevent the release.
     *
     * @param releaseWithMedia Determines if media references should also be checked
     * @return a list of elements that reference the workflow object.
     */
    public List<Object> getRefObjectsFromStoreElement(final boolean releaseWithMedia) {
        List<Object> referencedObjects = new ArrayList<Object>();

        if (isPageRef(storeElement)) {
            // add outgoing references
            referencedObjects.addAll(getReferences(releaseWithMedia));

            // add outgoing references of referenced page if it is not released
            final Page page = ((PageRef) storeElement).getPage();

            addOutgoingReferences(page, referencedObjects, releaseWithMedia);
            final List<ReferenceEntry> refObjectsFromSection = getRefObjectsFromSection(page, releaseWithMedia);
            referencedObjects.addAll(refObjectsFromSection);

        } else if (isValidStoreElement()) {
            // add outgoing references
            referencedObjects.addAll(getReferences(releaseWithMedia));
            if (isPage(storeElement)) {
                final List<ReferenceEntry> refObjectsFromSection = getRefObjectsFromSection(storeElement, releaseWithMedia);
                referencedObjects.addAll(refObjectsFromSection);
            }
        } else if (storeElement instanceof Content2) {
            //Element is a content2 object -> aborting"
            workflowScriptContext.gotoErrorState(bundle.getString("releaseC2notPossible"), new FsException());

        } else if (storeElement instanceof ContentFolder) {
            //Element is a content folder object -> aborting"
            workflowScriptContext.gotoErrorState(bundle.getString("releaseCFnotPossible"), new FsException());

        }
        return referencedObjects;
    }

    private static <L extends List> void addOutgoingReferences(final StoreElement element, final L referencedObjects,
                                                               final boolean releaseWithMedia) {
        addOutgoingReferences(element.getOutgoingReferences(), referencedObjects, releaseWithMedia);
    }

    private static <L extends List> void addOutgoingReferences(final ReferenceEntry[] entries, final L referencedObjects,
                                                               final boolean releaseWithMedia) {
        for (ReferenceEntry referenceEntry : entries) {
            if (releaseWithMedia || !referenceEntry.isType(ReferenceEntry.MEDIA_STORE_REFERENCE)) {
                referencedObjects.add(referenceEntry);
            }
        }
    }

    /**
     * This method gets the referenced objects from sections of a page that prevent the release.
     *
     * @param page             The page where to check the sections
     * @param releaseWithMedia Determines if media references should also be checked
     * @return a list of elements that reference the workflow object.
     */
    public List<ReferenceEntry> getRefObjectsFromSection(final StoreElement page, final boolean releaseWithMedia) {
        List<ReferenceEntry> referencedObjects = new ArrayList<ReferenceEntry>();

        // add outgoing references of page sections
        for (Section<?> section : page.getChildren(Section.class, true)) {
/** documentation example - begin. **/
            if (!(section instanceof Content2Section)) {
/** documentation example - end. **/
                addOutgoingReferences(section, referencedObjects, releaseWithMedia);
            }
        }
        return referencedObjects;
    }


    /**
     * This method gets the referenced objects from the workflow object (Entity) that prevent the release.
     *
     * @param includeMedia Determines if media references should also be checked
     * @return a list of elements that reference the workflow object.
     */
    public List<ReferenceEntry> getRefObjectsFromEntity(final boolean includeMedia) {
        List<ReferenceEntry> referencedObjects = new ArrayList<ReferenceEntry>();
        addOutgoingReferences(content2.getSchema().getOutgoingReferences(entity), referencedObjects, includeMedia);
        return referencedObjects;
    }

    /**
     * Convenience method to check if element can be released according to defined rules.
     *
     * @param releaseObjects   The list of objects to check.
     * @param releaseWithMedia Determines if media elements should be implicitly released.
     * @return true if successfull
     */
    public ReferenceResult checkReferences(final List<Object> releaseObjects, final boolean releaseWithMedia) {
        // object to store if elements can be released
        final ReferenceResult referenceResult = new ReferenceResult();

        final Map<String, IDProvider.UidType> notReleasedElements = new HashMap<String, IDProvider.UidType>();

        // iterate over references and check release rules
        for (final Object object : releaseObjects) {

            if (isEntity(object) || (isReferenceEntry(object) && isEntity(getReferencedObjectFrom((ReferenceEntry) object)))) {
                Entity entityFromReference;
                if (isEntity(object)) {
                    entityFromReference = (Entity) object;
                } else {
                    entityFromReference = (Entity) getReferencedObjectFrom((ReferenceEntry) object);
                }
                referenceResult.setOnlyMedia(false);
                // check if is no media and released
                final String
                    entityIdentifier =
                    entityFromReference.getIdentifier().getEntityTypeName() + " (" + entityFromReference.getIdentifier().getEntityTypeName() + ", ID#"
                    + entityFromReference.get("fs_id")
                    + ")";
                if (!entityFromReference.isReleased()) {
                    Logging.logWarning("No media and not released:" + entityFromReference.getIdentifier() + "#" + entityFromReference.get("fs_id"),
                                       LOGGER);
                    referenceResult.setNotMediaReleased(false);
                    referenceResult.setAllObjectsReleased(false);
                    notReleasedElements.put(entityIdentifier, IDProvider.UidType.CONTENTSTORE_DATA);
                }

                checkBrokenReferences(object, referenceResult);

            } else {

                IDProvider idProvider;
                if (isIdProvider(object)) {
                    idProvider = (IDProvider) object;
                } else {
                    idProvider = (IDProvider) getReferencedObjectFrom((ReferenceEntry) object);
                }

/** documentation example - begin. **/
                if (idProvider instanceof Section) {
                    idProvider = idProvider.getParent().getParent();
                }
/** documentation example - end. **/

                checkBrokenReferences(object, referenceResult);

                // check if current PAGE within PAGEREF-Release
                boolean isCurrentPage = false;
                if (isPage(idProvider) && isPageRef(storeElement)) {
                    Page page = (Page) idProvider;
                    Page curPage = ((PageRef) storeElement).getPage();
                    if (page.getId() == curPage.getId()) {
                        isCurrentPage = true;
                    }
                }

                // if not current PAGE within PAGEREF-Release or media and release with media is not checked
                if (idProvider != null && (!isCurrentPage || (isMedia(idProvider) && !releaseWithMedia))) {
                    idProvider.refresh();
                    // check if only media is referenced except templates
                    if (!isMedia(idProvider) && !isTemplate(idProvider) && !isDataRecord(idProvider)
                        && !(isQuery(idProvider))) {
                        Logging.logWarning("No media:" + idProvider.getUid(), LOGGER);
                        referenceResult.setOnlyMedia(false);
                        // check if is no media and not released
                        if (isNeverReleased(idProvider)) {
                            Logging.logWarning("No media, but never released:" + idProvider.getUid(), LOGGER);
                            referenceResult.setNotMediaReleased(false);
                            recordIncorrectElement(notReleasedElements, idProvider);
                        }
                    }
                    // check if all references are released
                    if (!isTemplate(idProvider) && !isDataRecord(idProvider) && !isQuery(idProvider) && idProvider.isReleaseSupported()
                        && isNeverReleased(idProvider)) {
                        Logging.logWarning("Never released:" + idProvider.getUid(), LOGGER);
                        referenceResult.setAllObjectsReleased(false);
                        recordIncorrectElement(notReleasedElements, idProvider);
                    }
                }
            }
        }

        // put not released elements to session for further use
        workflowScriptContext.getSession().put(WorkflowConstants.WF_NOT_RELEASED_ELEMENTS, notReleasedElements);

        // remember broken references
        workflowScriptContext.getSession().put(WorkflowConstants.WF_BROKEN_REFERENCES, Boolean.valueOf(!referenceResult.isNoBrokenReferences()));

        return referenceResult;
    }

    private static Object getReferencedObjectFrom(final ReferenceEntry object) {
        return object.getReferencedObject();
    }

    private static void checkBrokenReferences(final Object object, final ReferenceResult referenceResult) {
        if (object == null) {
            throw new IllegalArgumentException("Object is null");
        }
        if (referenceResult == null) {
            throw new IllegalArgumentException("ReferenceResult is null");
        }

        if (isReferenceEntry(object)) {
            ReferenceEntry referenceEntry = (ReferenceEntry) object;
            Logging.logInfo("Check broken reference for " + referenceEntry, LOGGER);
            if (referenceEntry.isBroken()) {
                referenceResult.setNoBrokenReferences(false);
                Logging.logInfo("Reference broken!", LOGGER);
            } else {
                checkForBrokenRefsOnNonBrokenRefs(referenceResult, referenceEntry);
            }
        }
    }

    private static void checkForBrokenRefsOnNonBrokenRefs(final ReferenceResult referenceResult, final ReferenceEntry referenceEntry) {
        if (referenceEntry.getReferencedElement() != null) {
            final ReferenceEntry[] outgoingReferences = referenceEntry.getReferencedElement().getOutgoingReferences();
            for (ReferenceEntry reference : outgoingReferences) {
                Logging.logInfo("Check broken reference for " + reference, LOGGER);
                if (reference.isBroken()) {
                    Logging.logInfo("Reference broken!", LOGGER);
                    referenceResult.setNoBrokenReferences(false);
                }
            }
        }
    }

    private boolean isValidStoreElement() {
        final boolean fragement1 =
            isPage(storeElement) || storeElement instanceof PageFolder || storeElement instanceof DocumentGroup || isMedia(storeElement);

        final boolean fragement2 =
            storeElement instanceof MediaFolder || storeElement instanceof GCAFolder || storeElement instanceof ProjectProperties;

        return fragement1 || fragement2 || storeElement instanceof PageRefFolder || storeElement instanceof GCAPage;
    }

    private static boolean isReferenceEntry(Object object) {
        return object instanceof ReferenceEntry;
    }

    private static boolean isNeverReleased(IDProvider idProvider) {
        return idProvider.getReleaseStatus() == IDProvider.NEVER_RELEASED;
    }

    private static boolean isQuery(final Object o) {
        return o instanceof Query;
    }

    private static boolean isDataRecord(final Object o) {
        return o instanceof Content2;
    }

    private static boolean isTemplate(final Object o) {
        return o instanceof TemplateStoreElement;
    }

    private static boolean isMedia(final Object o) {
        return o instanceof Media;
    }

    private static boolean isPageRef(final Object o) {
        return o instanceof PageRef;
    }

    private static boolean isPage(final Object o) {
        return o instanceof Page;
    }

    private static boolean isIdProvider(final Object object) {
        return object instanceof IDProvider;
    }

    private static boolean isEntity(final Object object) {
        return object instanceof Entity;
    }

    private void recordIncorrectElement(final Map<String, IDProvider.UidType> elements, final IDProvider idProvider) {
        if (elements == null) {
            throw new IllegalArgumentException("Map is null");
        }
        if (idProvider == null) {
            throw new IllegalArgumentException("IDProvider is null");
        }
        elements.put(
            idProvider.getDisplayName(new FsLocale(workflowScriptContext).getLanguage()) + " (" + idProvider.getUid() + ", "
            + idProvider.getId() + ")", idProvider.getUidType());

    }

    /**
     * Convenience method to get referenced objects of storeElement and its parents.
     *
     * @param releaseWithMedia Determines if media references should also be checked.
     * @return the list of referenced objects.
     */
    private List<Object> getReferences(final boolean releaseWithMedia) {
        List<Object> references = new ArrayList<Object>();

        // add outgoing references
        addOutgoingReferences(storeElement, references, releaseWithMedia);

        // add outgoing references of parent objects if element was never released before
        if (isNeverReleased((IDProvider) storeElement)) {
            StoreElement elem = storeElement;
            while (elem.getParent() != null) {
                elem = elem.getParent();
                addOutgoingReferences(elem, references, releaseWithMedia);
            }
        }

        //Special cases in ContentCreator
        if (workflowScriptContext.is(BaseContext.Env.WEBEDIT)) {
            addParentFoldersInCaseOfMovedFolder(references);
            addParentFolderIfChanged(references);
        }

        return references;
    }

    private void addParentFolderIfChanged(final List<Object> references) {
        // add parent folder if that has changed (only used in webedit workflow)
        if (isChanged((IDProvider) storeElement)) {
            StoreElement elem = storeElement;
            if (elem.getParent() != null) {
                elem = elem.getParent();
                if (isChanged((IDProvider) elem)) {
                    references.add(elem);
                }
            }
        }
    }

    private void addParentFoldersInCaseOfMovedFolder(final List<Object> references) {
        // add parent folders in case of a moved folder (only used in webedit workflow)
        if (isReleased((IDProvider) storeElement)) {
            StoreElement elem = storeElement;
            while (elem.getParent() != null) {
                elem = elem.getParent();
                if (isChanged((IDProvider) elem)) {
                    references.add(elem);
                }
            }
        }
    }

    private static boolean isChanged(final IDProvider storeElem) {
        return storeElem.getReleaseStatus() == IDProvider.CHANGED;
    }

    private static boolean isReleased(final IDProvider storeElem) {
        return storeElem.getReleaseStatus() == IDProvider.RELEASED;
    }

    /**
     * Sets the workflow object.
     *
     * @param idProvider the workflow object to use.
     */
    public void setStoreElement(IDProvider idProvider) {
        storeElement = idProvider;
    }
}
