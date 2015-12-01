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

import com.espirit.moddev.basicworkflows.util.*;
import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.AccessUtil;
import de.espirit.firstspirit.access.ReferenceEntry;
import de.espirit.firstspirit.access.ServerActionHandle;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.LockException;
import de.espirit.firstspirit.access.store.ReleaseProgress;
import de.espirit.firstspirit.access.store.contentstore.Content2;
import de.espirit.firstspirit.access.store.contentstore.ContentFolder;
import de.espirit.firstspirit.access.store.contentstore.ContentWorkflowable;
import de.espirit.firstspirit.access.store.globalstore.GCAFolder;
import de.espirit.firstspirit.access.store.globalstore.GCAPage;
import de.espirit.firstspirit.access.store.globalstore.ProjectProperties;
import de.espirit.firstspirit.access.store.mediastore.Media;
import de.espirit.firstspirit.access.store.mediastore.MediaFolder;
import de.espirit.firstspirit.access.store.pagestore.Page;
import de.espirit.firstspirit.access.store.pagestore.PageFolder;
import de.espirit.firstspirit.access.store.pagestore.Section;
import de.espirit.firstspirit.access.store.sitestore.DocumentGroup;
import de.espirit.firstspirit.access.store.sitestore.PageRef;
import de.espirit.firstspirit.access.store.sitestore.PageRefFolder;
import de.espirit.firstspirit.access.store.sitestore.SiteStoreRoot;
import de.espirit.firstspirit.access.store.templatestore.TemplateStoreElement;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;
import de.espirit.firstspirit.agency.QueryAgent;
import de.espirit.or.schema.Entity;

import java.util.*;

/**
 * This class is used to release IDProvider/Entity objects.
 *
 * @author stephan
 * @since 1.0
 */
public class ReleaseObject {

    private final Dialog dialog;

    /**
     * The Entity to be released.
     */
    private Entity entity;
    /**
     * The workflowScriptContext from the workflow.
     */
    private final WorkflowScriptContext workflowScriptContext;
    /**
     * The Objects to be released.
     */
    private List<Object> releaseObjects;
    /**
     * The ResourceBundle that contains language specific labels.
     */
    private final ResourceBundle bundle;
    /**
     * The List of validation Errors.
     */
    private final List<String> validationErrorList = new ArrayList<String>();
    /**
     * The logging class to use.
     */
    public static final Class<?> LOGGER = ReleaseObject.class;

    /**
     * Constructor for ReleaseObject with an Entity.
     *
     * @param workflowScriptContext The workflowScriptContext from the workflow.
     * @param releaseEntity         The Entity that is to be released.
     */
    public ReleaseObject(final WorkflowScriptContext workflowScriptContext, final Entity releaseEntity) {
        this(workflowScriptContext);
        this.entity = releaseEntity;
    }

    /**
     * Constructor for ReleaseObject with a list of Objects.
     *
     * @param workflowScriptContext The workflowScriptContext from the workflow.
     * @param releaseObjects        The list of objects that are to be released.
     */
    public ReleaseObject(final WorkflowScriptContext workflowScriptContext, final List<Object> releaseObjects) {
        this(workflowScriptContext);
        this.releaseObjects = new LinkedList<Object>(releaseObjects);
    }

    private ReleaseObject(final WorkflowScriptContext workflowScriptContext) {
        this.workflowScriptContext = workflowScriptContext;
        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(WorkflowConstants.MESSAGES, new FsLocale(workflowScriptContext).get());
        dialog = new Dialog(workflowScriptContext);
    }

    /**
     * Main release method that distinguishes between Entity and StoreElement. StoreElements are released within this method. For entities the
     * releaseEntity method is called.
     *
     * @param checkOnly Determines if the method should do only a check or really release the object.
     * @return true if successful.
     */
    public boolean release(boolean checkOnly) {
        final boolean result;
        Set<Long> lockedList = new HashSet<Long>();
        Set<Long> permList = new HashSet<Long>();

        // release entity
        if (this.entity != null) {
            final ContentWorkflowable contentWorkflowable = (ContentWorkflowable) workflowScriptContext.getWorkflowable();
            final Content2 content2 = contentWorkflowable.getContent();
            result = releaseEntity(content2, this.entity, checkOnly);
        } else {
            result = releaseStoreElement(checkOnly, lockedList, permList);
        }

        // set in integration tests
        final String suppressDialog = (String) workflowScriptContext.getSession().get("wfSuppressDialog");
        if (!WorkflowConstants.TRUE.equals(suppressDialog)) {
            showLockedElementsIfAny(lockedList);
            showDeniedElementsIfAny(permList);
            showInvalidElementsIfAny();
        }
        return result;
    }

    private void showInvalidElementsIfAny() {
        if (!validationErrorList.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder();
            for (String validationError : validationErrorList) {
                errorMsg.append(validationError);
            }
            dialog.showError(bundle.getString("errorValidation"), errorMsg.toString());
        }
    }

    private void showDeniedElementsIfAny(Set<Long> permList) {
        if (!permList.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder(bundle.getString("errorPermission")).append(":\n\n");
            Logging.logInfo("MissingPermissionElement", LOGGER);
            for (Long missing : permList) {
                Logging.logInfo("  id:" + missing, LOGGER);
                errorMsg.append(createErrorString(missing));
            }
            dialog.showError(bundle.getString("errorPermission"), errorMsg.toString());
        }
    }

    private void showLockedElementsIfAny(Set<Long> lockedList) {
        if (!lockedList.isEmpty()) {
            Logging.logInfo("LockFailedElements:", LOGGER);
            StringBuilder errorMsg = new StringBuilder(bundle.getString("errorLocked")).append(":\n\n");

            for (Object locked : lockedList) {
                Logging.logInfo("  id:" + locked, LOGGER);
                errorMsg.append(createErrorString(locked));
            }
            dialog.showError(bundle.getString("errorLocked"), errorMsg.toString());
        }
    }

    private boolean releaseStoreElement(boolean checkOnly, Set<Long> lockedList, Set<Long> permList) {
        boolean result = true;

        ServerActionHandle<? extends ReleaseProgress, Boolean> handle = null;

        List<IDProvider> customReleaseElements = getCustomReleaseElements(WorkflowConstants.RELEASE_PAGEREF_ELEMENTS);

        try {
            for (Object object : releaseObjects) {
                if (!(object instanceof ReferenceEntry && ((ReferenceEntry) object).getReferencedObject() instanceof Entity)) {
                    IDProvider idProvider;
                    if (object instanceof IDProvider) {
                        idProvider = (IDProvider) object;
                    } else {
                        idProvider = ((ReferenceEntry) object).getReferencedElement();
                    }

                    // release only referenced media, workflow object and unreleased parent pagereffolders
                    if (idProvider == workflowScriptContext.getElement()
                        || (workflowScriptContext.getElement() instanceof PageRef && idProvider == ((PageRef) workflowScriptContext.getElement())
                        .getPage())
                        || idProvider instanceof Media
                        || idProvider instanceof PageRefFolder
                        || idProvider instanceof SiteStoreRoot
                        || customReleaseElements.contains(idProvider)) {
                        Logging.logInfo("Prepare release for: " + idProvider.getId(), LOGGER);

                        // only release items that are not yet released
                        if (idProvider.getReleaseStatus() != IDProvider.RELEASED && !(idProvider instanceof TemplateStoreElement)
                            && !(idProvider instanceof Content2) && !(idProvider instanceof ContentFolder)) {
                            // check rules
                            String validationError = new FormValidator(workflowScriptContext).isValid(idProvider);
                            if (validationError != null) {
                                validationErrorList.add(validationError);
                            }
                            // check rules for sections of pages (as checkrules is not recursive)
                            if (idProvider instanceof Page) {
                                for (Section<?> section : idProvider.getChildren(Section.class, true)) {
                                    String validationErrorsSection = new FormValidator(workflowScriptContext).isValid(section);
                                    if (validationErrorsSection != null) {
                                        validationErrorList.add(validationErrorsSection);
                                    }
                                }
                            }
                            if (validationErrorList.isEmpty()) {
                                // check release
                                if (idProvider == workflowScriptContext.getElement()) {
                                    // unlock element that runs the workflow
                                    idProvider.setLock(false, false);
                                }
                                //release(IDProvider releaseStartNode, boolean checkOnly, boolean releaseParentPath, boolean recursive, IDProvider.DependentReleaseType dependentType)
                                if (idProvider instanceof PageRef) {
                                    // in order to decide if a pageref can be released, one has to check if the referenced page exists
                                    IDProvider.DependentReleaseType releaseType = checkOnly ? IDProvider.DependentReleaseType.DEPENDENT_RELEASE_NEW_ONLY : IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE;
                                    handle = AccessUtil.release(idProvider, checkOnly, true, false, releaseType);
                                } else if (idProvider instanceof PageRefFolder) {
                                    handle = AccessUtil.release(idProvider, checkOnly, true, false,
                                                                IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);
                                } else if (idProvider instanceof Page) {
                                    handle = AccessUtil.release(idProvider, checkOnly, true, false,
                                                                IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);
                                } else if (idProvider instanceof PageFolder) {
                                    handle = AccessUtil.release(idProvider, checkOnly, true, false,
                                                                IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);
                                } else if (idProvider instanceof DocumentGroup) {
                                    handle = AccessUtil.release(idProvider, checkOnly, true, false,
                                                                IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);
                                } else if (idProvider instanceof Media) {
                                    handle = AccessUtil.release(idProvider, checkOnly, true, false,
                                                                IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);
                                } else if (idProvider instanceof MediaFolder) {
                                    handle = AccessUtil.release(idProvider, checkOnly, true, false,
                                                                IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);
                                } else if (idProvider instanceof GCAPage) {
                                    handle = AccessUtil.release(idProvider, checkOnly, false, false,
                                                                IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);
                                } else if (idProvider instanceof GCAFolder) {
                                    handle = AccessUtil.release(idProvider, checkOnly, false, false,
                                                                IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);
                                } else if (idProvider instanceof ProjectProperties) {
                                    handle = AccessUtil.release(idProvider, checkOnly, false, false,
                                                                IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);
                                } else if (idProvider instanceof SiteStoreRoot) {
                                    handle = AccessUtil.release(idProvider, checkOnly, false, false,
                                                                IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);
                                }

                                result = handleResult(lockedList, permList, handle, idProvider);
                                if (idProvider.equals(workflowScriptContext.getElement())) {
                                    idProvider.setLock(true, false);
                                }
                            } else {
                                Logging.logError("Validation failure during release!", LOGGER);
                                result = false;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logging.logError("Exception during Release ", e, LOGGER);
            result = false;
        }
        return result;
    }

    private static boolean handleResult(Set<Long> lockedList, Set<Long> permList, ServerActionHandle<? extends ReleaseProgress, Boolean> handle,
                                        IDProvider idProvider) {
        boolean result = true;
        if (handle != null) {
            try {
                handle.checkAndThrow();
                handle.getResult();
                result = handle.getResult();
                Logging.logInfo("Release Result: " + result, LOGGER);
                final ReleaseProgress progress = handle.getProgress(true);
                final Set<Long> lockedFailed = progress.getLockFailedElements();
                final Set<Long> missingPermission = progress.getMissingPermissionElements();
                Logging.logInfo("Released Elements:", LOGGER);
                for (Long released : progress.getReleasedElements()) {
                    Logging.logInfo("  id:" + released, LOGGER);
                }
                if (lockedFailed != null && !lockedFailed.isEmpty()) {
                    for (Long locked : lockedFailed) {
                        lockedList.add(locked);
                    }
                    result = false;
                }
                if (missingPermission != null && !missingPermission.isEmpty()) {
                    for (Long missing : missingPermission) {
                        permList.add(missing);
                    }
                    result = false;
                }
            } catch (Exception e) {
                Logging.logError("Exception during Release of " + idProvider, e, LOGGER);
                result = false;
            }
        }
        return result;
    }

    private List<IDProvider> getCustomReleaseElements(String type) {
        List<IDProvider> customReleaseElements = new ArrayList<IDProvider>();
        if (type.equals(WorkflowConstants.RELEASE_PAGEREF_ELEMENTS)) {
            Object releasePageRefElements = workflowScriptContext.getSession().get(WorkflowConstants.RELEASE_PAGEREF_ELEMENTS);
            if (releasePageRefElements != null) {
                @SuppressWarnings("unchecked")
                final List<String> releasePageRefUids = (List<String>) releasePageRefElements;
                for (String pageRefUid : releasePageRefUids) {
                    PageRef pageRef = new StoreUtil(workflowScriptContext).loadPageRefByUid(pageRefUid);
                    customReleaseElements.add(pageRef);
                    customReleaseElements.add(pageRef.getPage());
                }
            }
        }
        return customReleaseElements;
    }

    /**
     * This method is used to release an entity.
     *
     * @param content2  The content2 object of the entity.
     * @param entity    The Entity to release.
     * @param checkOnly Determines if the method should do only a check or really release the object.
     * @return true if successful.
     */
    private boolean releaseEntity(Content2 content2, Entity entity, boolean checkOnly) {
        boolean result = true;

        String validationError = new FormValidator(workflowScriptContext).isValid(content2, entity);
        if (validationError == null) {
            if (!checkOnly) {
                try {
                    entity.refresh();
                    content2.refresh();
                    content2.release(entity);
                    content2.refresh();
                    content2.getParent().refresh();
                } catch (LockException e) {
                    Logging.logError("Exception during Release of " + entity, e, LOGGER);
                    result = false;
                }
            }
        } else {
            Logging.logError("Validation failure during release!", LOGGER);
            validationErrorList.add(validationError);
            result = false;
        }
        return result;
    }

    /**
     * Convenience method to generate an error message for an object.
     *
     * @param id the object to generate a message for.
     * @return the error message.
     */
    private String createErrorString(Object id) {
        final QueryAgent queryAgent = workflowScriptContext.requireSpecialist(QueryAgent.TYPE);
        final Iterable<IDProvider> hits = queryAgent.answer("fs.id=" + id);
        IDProvider element = hits.iterator().next();
        if (element.hasUid()) {
            return element.getDisplayName(new FsLocale(workflowScriptContext).getLanguage()) + " (" + element.getUid() + ", " + element.getId()
                   + ")\n";
        } else {
            return "";
        }

    }
}
