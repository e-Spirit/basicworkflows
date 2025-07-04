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

import to.be.renamed.module.util.FsException;
import to.be.renamed.module.util.FsLocale;
import to.be.renamed.module.util.ReferenceResult;
import to.be.renamed.module.util.WorkflowConstants;
import to.be.renamed.module.util.WorkflowSessionHelper;

import de.espirit.common.TypedFilter;
import de.espirit.common.base.Logging;
import de.espirit.common.util.Listable;
import de.espirit.firstspirit.access.BaseContext;
import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.access.ReferenceEntry;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.Store;
import de.espirit.firstspirit.access.store.StoreElement;
import de.espirit.firstspirit.access.store.contentstore.Content2;
import de.espirit.firstspirit.access.store.contentstore.ContentFolder;
import de.espirit.firstspirit.access.store.contentstore.ContentStoreRoot;
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
import de.espirit.firstspirit.access.store.sitestore.SiteStoreFolder;
import de.espirit.firstspirit.access.store.templatestore.Query;
import de.espirit.firstspirit.access.store.templatestore.TemplateStoreElement;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;
import de.espirit.firstspirit.agency.StoreAgent;
import de.espirit.or.schema.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * This class provides methods to get the references of the workflow object and store them in the session.
 *
 * @author stephan
 * @since 1.0
 */
class WorkflowObject {

    /**
     * The storeElement to use.
     */
    private StoreElement storeElement;

    private StoreElement startElement;
    /**
     * The workflowScriptContext from the workflow.
     */
    private final WorkflowScriptContext workflowScriptContext;
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
    private final ResourceBundle bundle;
    /**
     * The logging class to use.
     */
    public static final Class<?> LOGGER = WorkflowObject.class;

    private boolean releaseRecursively = false;

    private final Set<IDProvider> recursiveChildrenList = new HashSet<>();

    /**
     * Constructor for WorkflowObject.
     *
     * @param workflowScriptContext The workflowScriptContext from the workflow.
     */
    WorkflowObject(final WorkflowScriptContext workflowScriptContext) {
        this.workflowScriptContext = workflowScriptContext;
        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(WorkflowConstants.MESSAGES, new FsLocale(workflowScriptContext).get());
        if (workflowScriptContext.getWorkflowable() instanceof ContentWorkflowable) {
            content2 = ((ContentWorkflowable) workflowScriptContext.getWorkflowable()).getContent();
            entity = ((ContentWorkflowable) workflowScriptContext.getWorkflowable()).getEntity();
        } else {
            storeElement = (StoreElement) workflowScriptContext.getWorkflowable();
            startElement = (StoreElement) workflowScriptContext.getWorkflowable();
        }

        // get elements from recursive release
        final Map<Long, Store.Type> childrenIdMap =
            WorkflowSessionHelper.readObjectFromSession(workflowScriptContext, WorkflowConstants.WF_RECURSIVE_CHILDREN);
        if (childrenIdMap != null && !childrenIdMap.isEmpty()) {
            final StoreAgent storeAgent = workflowScriptContext.requireSpecialist(StoreAgent.TYPE);
            for (final Map.Entry<Long, Store.Type> childrenId : childrenIdMap.entrySet()) {
                recursiveChildrenList.add(storeAgent.getStore(childrenId.getValue()).getStoreElement(childrenId.getKey()));
            }
        }
    }


    /**
     * This method gets the referenced objects from the workflow object (StoreElement) that prevent the release.
     *
     * @param releaseWithMedia Determines if media references should also be checked.
     * @param recursive        Whether to include references recursively.
     * @param languages        The languages to consider during the release check.
     * @return a set of elements that reference the workflow object.
     */
    Set<Object> getRefObjectsFromStoreElement(final boolean releaseWithMedia, final boolean recursive, final Language[] languages) {
        return getRefObjectsFromStoreElement(releaseWithMedia, recursive, storeElement, languages);
    }


    /**
     * This method gets the referenced objects from the workflow object (StoreElement) that prevent the release.
     *
     * @param releaseWithMedia Determines if media references should also be checked.
     * @param recursive        Whether to include references recursively.
     * @param storeElement     The store element to check.
     * @param languages        The languages to consider during the release check.
     * @return a set of elements that reference the workflow object.
     */
    private Set<Object> getRefObjectsFromStoreElement(final boolean releaseWithMedia, final boolean recursive, StoreElement storeElement,
                                                      final Language[] languages) {
        Set<Object> referencedObjects = new HashSet<>();

        if (isPageRef(storeElement)) {
            // add outgoing references
            referencedObjects.addAll(getReferences(releaseWithMedia, storeElement, languages));

            // add outgoing references of referenced page if it is not released
            final Page page = ((PageRef) storeElement).getPage();

            addOutgoingReferences(page, referencedObjects, releaseWithMedia);
            final Set<Object> refObjectsFromSection = getRefObjectsFromSection(page, releaseWithMedia);
            referencedObjects.addAll(refObjectsFromSection);
        } else if (recursive && storeElement instanceof SiteStoreFolder) {
            for (IDProvider idProvider : storeElement.getChildren(IDProvider.class)) {
                referencedObjects.addAll(getRefObjectsFromStoreElement(releaseWithMedia, true, idProvider, languages));
            }
        } else if (isValidStoreElement()) {
            // add outgoing references
            referencedObjects.addAll(getReferences(releaseWithMedia, storeElement, languages));
            if (isPage(storeElement)) {
                final Set<Object> refObjectsFromSection = getRefObjectsFromSection(storeElement, releaseWithMedia);
                referencedObjects.addAll(refObjectsFromSection);
            }
        } else if (storeElement instanceof Content2) {
            //Element is a content2 object -> aborting
            workflowScriptContext.gotoErrorState(bundle.getString("releaseC2notPossible"), new FsException());

        } else if (storeElement instanceof ContentFolder) {
            //Element is a content folder object -> aborting
            workflowScriptContext.gotoErrorState(bundle.getString("releaseCFnotPossible"), new FsException());

        }
        return referencedObjects;
    }


    private void addOutgoingReferences(final StoreElement element, final Set<Object> referencedObjects, final boolean releaseWithMedia) {
        addOutgoingReferences(element.getOutgoingReferences(), referencedObjects, releaseWithMedia);
    }


    private void addOutgoingReferences(final ReferenceEntry[] entries, final Set<Object> referencedObjects, final boolean releaseWithMedia) {
        for (ReferenceEntry referenceEntry : entries) {
            boolean referencedElementIsNotSelf = referenceEntry.getReferencedElement() != storeElement;
            boolean referencedElementIsNoTemplate = !(referenceEntry.getReferencedElement() instanceof TemplateStoreElement);
            boolean referencedElementIsNoMedia = !referenceEntry.isType(ReferenceEntry.MEDIA_STORE_REFERENCE);
            boolean releaseReferencedMedia = releaseWithMedia || referencedElementIsNoMedia;
            if (referencedElementIsNoTemplate && releaseReferencedMedia && referencedElementIsNotSelf) {
                referencedObjects.add(referenceEntry);
            }
        }
    }

    /**
     * This method gets the referenced objects from sections of a page that prevent the release.
     *
     * @param page             The page where to check the sections
     * @param releaseWithMedia Determines if media references should also be checked
     * @return a set of elements that reference the workflow object.
     */
    private Set<Object> getRefObjectsFromSection(final StoreElement page, final boolean releaseWithMedia) {
        Set<Object> referencedObjects = new HashSet<>();

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
     * @param includeMedia Determines if media references should also be checked.
     * @return a set of elements that reference the workflow object.
     */
    Set<Object> getRefObjectsFromEntity(final boolean includeMedia) {
        Set<Object> referencedObjects = new HashSet<>();
        addOutgoingReferences(content2.getSchema().getOutgoingReferences(entity), referencedObjects, includeMedia);
        return referencedObjects;
    }

    /**
     * Convenience method to check if element can be released according to defined rules.
     *
     * @param releaseObjects   The list of objects to check.
     * @param releaseWithMedia Determines if media elements should be implicitly released.
     * @param languages        The languages to consider during the release check.
     * @return true if successful
     */
    ReferenceResult checkReferences(final List<Object> releaseObjects, final boolean releaseWithMedia, final Language[] languages) {
        // object to store if elements can be released
        final ReferenceResult referenceResult = new ReferenceResult();

        final Map<String, IDProvider.UidType> notReleasedElements = new HashMap<>();
        final Map<String, IDProvider.UidType> elementsInWorkflow = new HashMap<>();
        final ArrayList<IDProvider> releaseIdProviders = new ArrayList<>();
        final ArrayList<Entity> releaseEntities = new ArrayList<>();

        // iterate over references and get IDProvider and Entities to release
        for (final Object object : releaseObjects) {

            if (isEntity(object) || (isReferenceEntry(object) && isEntity(getReferencedObjectFrom((ReferenceEntry) object)))) {

                final Entity entityFromReference;

                if (isEntity(object)) {
                    entityFromReference = (Entity) object;
                } else {
                    entityFromReference = (Entity) getReferencedObjectFrom((ReferenceEntry) object);
                }
                releaseEntities.add(entityFromReference);
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
                releaseIdProviders.add(idProvider);
            }
            checkBrokenReferences(object, referenceResult);
        }

        checkRulesForEntities(releaseEntities, referenceResult, notReleasedElements);
        checkRulesForIdProviders(releaseIdProviders, referenceResult, notReleasedElements, releaseWithMedia, elementsInWorkflow, languages);

        // put not released elements to session for further use
        final Map<String, IDProvider.UidType> notReleasedElementsFromSession =
            WorkflowSessionHelper.readMapFromSession(workflowScriptContext, WorkflowConstants.WF_NOT_RELEASED_ELEMENTS);
        notReleasedElements.putAll(notReleasedElementsFromSession);
        workflowScriptContext.getSession().put(WorkflowConstants.WF_NOT_RELEASED_ELEMENTS, notReleasedElements);

        // put elements in workflow to session for further use
        final Map<String, IDProvider.UidType> elementsInWorkflowFromSession =
            WorkflowSessionHelper.readMapFromSession(workflowScriptContext, WorkflowConstants.WF_OBJECTS_IN_WORKFLOW);
        elementsInWorkflow.putAll(elementsInWorkflowFromSession);
        workflowScriptContext.getSession().put(WorkflowConstants.WF_OBJECTS_IN_WORKFLOW, elementsInWorkflow);

        // remember broken references
        if (releaseRecursively && workflowScriptContext.getSession().containsKey(WorkflowConstants.WF_BROKEN_REFERENCES)) {
            boolean brokenReferences = WorkflowSessionHelper.readBooleanFromSession(workflowScriptContext, WorkflowConstants.WF_BROKEN_REFERENCES);
            workflowScriptContext.getSession()
                .put(WorkflowConstants.WF_BROKEN_REFERENCES, !referenceResult.isNoBrokenReferences() || brokenReferences);
        } else {
            workflowScriptContext.getSession().put(WorkflowConstants.WF_BROKEN_REFERENCES, !referenceResult.isNoBrokenReferences());
        }

        return referenceResult;
    }

    private void checkObjectInWorkflow(IDProvider idProvider, ReferenceResult referenceResult, Map<String, IDProvider.UidType> elementsInWorkflow) {
        if (idProvider == null) {
            throw new IllegalArgumentException("Object is null");
        }
        idProvider.refresh();

        if (idProvider.hasTask()) {
            Logging.logWarning("Element in workflow:" + idProvider.getUid(), LOGGER);
            referenceResult.setNoObjectsInWorkflow(false);
            recordIncorrectElement(elementsInWorkflow, idProvider);
        }
    }


    private void checkRulesForEntities(ArrayList<Entity> releaseEntities, ReferenceResult referenceResult,
                                       Map<String, IDProvider.UidType> notReleasedElements) {
        for (Entity entityFromReference : releaseEntities) {
            Listable<Content2> suitableContent2Objects = getContent2ObjectsForEntity(entityFromReference);

            if (suitableContent2Objects.toList().isEmpty()) {
                throw new IllegalStateException("No suitable content2 object found for referenced entity!");
            } else {
                boolean schemaIsReadonly = suitableContent2Objects.getFirst().getSchema().isReadOnly();
                if (!schemaIsReadonly && !entityFromReference.isReleased()) {
                    Logging.logWarning(
                        "No media and not released:" + entityFromReference.getIdentifier() + "#" + entityFromReference.get("fs_id"), LOGGER);

                    final String entityIdentifier = entityFromReference.getIdentifier().getEntityTypeName() + " ("
                                                    + entityFromReference.getIdentifier().getEntityTypeName() + ", ID#" + entityFromReference
                                                        .get("fs_id") + ")";

                    referenceResult.setNotMediaReleased(false);
                    referenceResult.setAllObjectsReleased(false);
                    notReleasedElements.put(entityIdentifier, IDProvider.UidType.CONTENTSTORE_DATA);
                }
            }
            referenceResult.setOnlyMedia(false);
        }
    }

    private void checkRulesForIdProviders(ArrayList<IDProvider> releaseIdProviders,
                                          ReferenceResult referenceResult,
                                          Map<String, IDProvider.UidType> notReleasedElements, boolean releaseWithMedia,
                                          Map<String, IDProvider.UidType> elementsInWorkflow,
                                          final Language[] languages) {
        for (IDProvider idProvider : releaseIdProviders) {
            // check if current PAGE within PAGEREF-Release
            boolean isCurrentPage = false;
            boolean isPartOfRelease = recursiveChildrenList.contains(idProvider) && releaseRecursively;
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
                if (!isMedia(idProvider) && !isTemplate(idProvider) && !isDataRecord(idProvider) && !(isQuery(idProvider))) {
                    Logging.logWarning("No media:" + idProvider.getId(), LOGGER);
                    referenceResult.setOnlyMedia(false);
                    // check if is no media and not released
                    if (isNeverReleased(idProvider, languages) && !isPartOfRelease) {
                        Logging.logWarning("No media, but never released:" + idProvider.getId(), LOGGER);
                        referenceResult.setNotMediaReleased(false);
                        recordIncorrectElement(notReleasedElements, idProvider);
                    }
                }
                // check if all references are released
                boolean hasCorrectType = !isTemplate(idProvider) && !isDataRecord(idProvider) && !isQuery(idProvider);
                boolean isReleasable = idProvider.isReleaseSupported() && isNeverReleased(idProvider, languages);

                if (hasCorrectType && isReleasable && !isPartOfRelease) {
                    Logging.logWarning("Never released:" + idProvider.getId(), LOGGER);
                    referenceResult.setAllObjectsReleased(false);
                    recordIncorrectElement(notReleasedElements, idProvider);
                }
            }
            if (idProvider != null) {
                checkObjectInWorkflow(idProvider, referenceResult, elementsInWorkflow);
            }
        }
    }


    private Listable<Content2> getContent2ObjectsForEntity(final Entity entityFromReference) {

        StoreAgent storeAgent = workflowScriptContext.requireSpecialist(StoreAgent.TYPE);
        ContentStoreRoot contentStoreRoot = (ContentStoreRoot) storeAgent.getStore(Store.Type.CONTENTSTORE);

        return contentStoreRoot.getChildren(new TypedFilter<>(Content2.class) {
            @Override
            public boolean accept(Content2 storeElement) {
                return (storeElement.getEntityType().equals(entityFromReference.getEntityType()))
                       && storeElement.getEntity(entityFromReference.getKeyValue()) != null;
            }
        }, true);
    }

    private Object getReferencedObjectFrom(final ReferenceEntry object) {
        return object.getReferencedObject();
    }

    private void checkBrokenReferences(final Object object, final ReferenceResult referenceResult) {
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
                ReferenceEntry[] usages = referenceEntry.getUsages();
                Logging.logInfo("Current usages:", LOGGER);
                for (ReferenceEntry usage : usages) {
                    Logging.logInfo("Reference id: " + usage.getId(), LOGGER);
                }
            } else {
                checkForBrokenRefsOnNonBrokenRefs(referenceResult, referenceEntry);
            }
        }
    }

    private void checkForBrokenRefsOnNonBrokenRefs(final ReferenceResult referenceResult, final ReferenceEntry referenceEntry) {
        if (referenceEntry.getReferencedElement() != null) {
            final ReferenceEntry[] outgoingReferences = referenceEntry.getReferencedElement().getOutgoingReferences();
            for (ReferenceEntry reference : outgoingReferences) {
                Logging.logInfo("Check broken reference for " + reference, LOGGER);
                if (reference.isBroken()) {
                    referenceResult.setNoBrokenReferences(false);
                    Logging.logInfo("Reference broken!", LOGGER);
                    ReferenceEntry[] usages = referenceEntry.getUsages();
                    Logging.logInfo("Current usages:", LOGGER);
                    for (ReferenceEntry usage : usages) {
                        Logging.logInfo("Reference id: " + usage.getId(), LOGGER);
                    }
                }
            }
        }
    }

    private boolean isValidStoreElement() {
        final boolean fragment1 =
            isPage(storeElement) || storeElement instanceof PageFolder || storeElement instanceof DocumentGroup || isMedia(storeElement);

        final boolean fragment2 =
            storeElement instanceof MediaFolder || storeElement instanceof GCAFolder || storeElement instanceof ProjectProperties;

        return fragment1 || fragment2 || storeElement instanceof PageRefFolder || storeElement instanceof GCAPage;
    }

    private boolean isReferenceEntry(Object object) {
        return object instanceof ReferenceEntry;
    }

    private boolean isQuery(final Object o) {
        return o instanceof Query;
    }

    private boolean isDataRecord(final Object o) {
        return o instanceof Content2;
    }

    private boolean isTemplate(final Object o) {
        return o instanceof TemplateStoreElement;
    }

    private boolean isMedia(final Object o) {
        return o instanceof Media;
    }

    private boolean isPageRef(final Object o) {
        return o instanceof PageRef;
    }

    private boolean isPage(final Object o) {
        return o instanceof Page;
    }

    private boolean isIdProvider(final Object object) {
        return object instanceof IDProvider;
    }

    private boolean isEntity(final Object object) {
        return object instanceof Entity;
    }

    private void recordIncorrectElement(final Map<String, IDProvider.UidType> elements, final IDProvider idProvider) {
        if (elements == null) {
            throw new IllegalArgumentException("Map is null");
        }
        if (idProvider == null) {
            throw new IllegalArgumentException("IDProvider is null");
        }
        elements.put(idProvider.getDisplayName(new FsLocale(workflowScriptContext).getLanguage()) + " (" + idProvider.getUid() + ", "
                     + idProvider.getId() + ")", idProvider.getUidType());

    }

    /**
     * Convenience method to get referenced objects of storeElement and its parents.
     *
     * @param releaseWithMedia Determines if media references should also be checked.
     * @param storeElement     The store element to check.
     * @param languages        The languages to consider during the release check.
     * @return the set of referenced objects.
     */
    private Set<Object> getReferences(final boolean releaseWithMedia, StoreElement storeElement, final Language[] languages) {
        Set<Object> references = new HashSet<>();

        addOutgoingReferences(storeElement, references, releaseWithMedia);
        addOutgoingReferencesFromParentObjects(storeElement, references, releaseWithMedia, languages);

        // special case in the ContentCreator
        addParentFolder(references, languages);

        return references;
    }

    /*
     * Adds the outgoing references from the parent objects if the store element was never released before.
     */
    private void addOutgoingReferencesFromParentObjects(final StoreElement storeElement, final Set<Object> references, final boolean releaseWithMedia,
                                                        final Language[] languages) {
        if (isNeverReleased((IDProvider) storeElement, languages) && (!releaseRecursively || !isInChildList())) {
            StoreElement element = storeElement;

            while (element.getParent() != null) {
                element = element.getParent();
                addOutgoingReferences(element, references, releaseWithMedia);
            }
        }
    }

    /*
     * Only required in the ContentCreator workflow.
     *
     * Adds the parent folder if it has changed. It's not necessary to check if the parent folder was released before.
     * The AccessUtil.release() method in the ReleaseObject class ensures the accessibility.
     */
    private void addParentFolder(final Set<Object> references, final Language[] languages) {
        if (workflowScriptContext.is(BaseContext.Env.WEBEDIT) && (!releaseRecursively || !isInChildList())) {
            IDProvider parent = (IDProvider) storeElement.getParent();
            if (parent != null && isChanged(parent, languages)) {
                references.add(parent);
            }
        }
    }

    /*
     * Checks if the 'element' is a child of the 'startElement'.
     */
    private boolean isInChildList() {
        StoreElement element = storeElement;

        while (element != null && element.getParent() != null) {
            element = element.getParent();

            if (element.equals(startElement)) {
                return true;
            }
        }

        return false;
    }

    private boolean isChanged(final IDProvider idProvider, final Language[] languages) {
        int releaseStatus = languages.length > 0 ? idProvider.getReleaseStatus(languages) : idProvider.getReleaseStatus();
        return releaseStatus == IDProvider.CHANGED;
    }

    private boolean isNeverReleased(IDProvider idProvider, final Language[] languages) {
        int releaseStatus = languages.length > 0 ? idProvider.getReleaseStatus(languages) : idProvider.getReleaseStatus();
        return releaseStatus == IDProvider.NEVER_RELEASED;
    }

    void setStoreElement(IDProvider idProvider) {
        storeElement = idProvider;
    }

    void setRecursively(boolean releaseRecursively) {
        this.releaseRecursively = releaseRecursively;
    }
}
