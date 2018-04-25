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

import com.espirit.moddev.basicworkflows.util.Dialog;
import com.espirit.moddev.basicworkflows.util.FormEvaluator;
import com.espirit.moddev.basicworkflows.util.FormValidator;
import com.espirit.moddev.basicworkflows.util.FsLocale;
import com.espirit.moddev.basicworkflows.util.StoreComparator;
import com.espirit.moddev.basicworkflows.util.StoreUtil;
import com.espirit.moddev.basicworkflows.util.WorkflowConstants;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.AccessUtil;
import de.espirit.firstspirit.access.ReferenceEntry;
import de.espirit.firstspirit.access.ServerActionHandle;
import de.espirit.firstspirit.access.store.IDProvider;
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
import de.espirit.or.Session;
import de.espirit.or.schema.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

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
	private final Set<String> validationErrorList = new HashSet<>();
    /**
     * The logging class to use.
     */
    public static final Class<?> LOGGER = ReleaseObject.class;

    /**
     * Constructor for ReleaseObject with an Entity.
     *
     * @param workflowScriptContext The workflowScriptContext from the workflow.
     * @param releaseEntity The Entity that is to be released.
     */
    ReleaseObject(final WorkflowScriptContext workflowScriptContext, final Entity releaseEntity) {
        this(workflowScriptContext);
        this.entity = releaseEntity;
    }

    /**
     * Constructor for ReleaseObject with a list of Objects.
     *
     * @param workflowScriptContext The workflowScriptContext from the workflow.
     * @param releaseObjects The list of objects that are to be released.
     */
    ReleaseObject(final WorkflowScriptContext workflowScriptContext, final List<Object> releaseObjects) {
        this(workflowScriptContext);
        this.releaseObjects = new LinkedList<>(releaseObjects);
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
     * @param releaseRecursively do a recursive release?
     * @return true if successful.
     */
    public boolean release(final boolean checkOnly, final boolean releaseRecursively) {
        final boolean result;
        final Set<Long> lockedList = new HashSet<>();
        final Set<Long> permList = new HashSet<>();

        // release entity
        if (this.entity != null) {
            final ContentWorkflowable contentWorkflowable = (ContentWorkflowable) workflowScriptContext.getWorkflowable();
            final Content2 content2 = contentWorkflowable.getContent();
            result = releaseEntity(content2, this.entity, checkOnly);
        } else {
            result = releaseStoreElement(checkOnly, lockedList, permList, releaseRecursively);
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
            final StringBuilder errorMsg = new StringBuilder();
            for (final String validationError : validationErrorList) {
                errorMsg.append(validationError);
            }
            dialog.showError(bundle.getString("errorValidation"), errorMsg.toString());
        }
    }

    private void showDeniedElementsIfAny(final Set<Long> permList) {
        if (!permList.isEmpty()) {
            final StringBuilder errorMsg = new StringBuilder(bundle.getString("errorPermission")).append(":\n\n");
            Logging.logInfo("MissingPermissionElement", LOGGER);
            for (final Long missing : permList) {
                Logging.logInfo("  id:" + missing, LOGGER);
                errorMsg.append(createErrorString(missing));
            }
            dialog.showError(bundle.getString("errorPermission"), errorMsg.toString());
        }
    }

    private void showLockedElementsIfAny(final Set<Long> lockedList) {
        if (!lockedList.isEmpty()) {
            Logging.logInfo("LockFailedElements:", LOGGER);
            final StringBuilder errorMsg = new StringBuilder(bundle.getString("errorLocked")).append(":\n\n");

            for (final Object locked : lockedList) {
                Logging.logInfo("  id:" + locked, LOGGER);
                errorMsg.append(createErrorString(locked));
            }
            dialog.showError(bundle.getString("errorLocked"), errorMsg.toString());
        }
    }

    private boolean releaseStoreElement(final boolean checkOnly, final Set<Long> lockedList, final Set<Long> permList, final boolean releaseRecursively) {
        boolean result = true;

        ServerActionHandle<? extends ReleaseProgress, Boolean> handle = null;

        final List<IDProvider> customReleaseElements = getCustomReleaseElements(WorkflowConstants.RELEASE_PAGEREF_ELEMENTS);

        try {
            Collections.sort(releaseObjects, new StoreComparator());
            for (final Object object : releaseObjects) {
                if (!(object instanceof ReferenceEntry && ((ReferenceEntry) object).getReferencedObject() instanceof Entity)) {
                    final IDProvider currentObjForRelease;
                    if (object instanceof IDProvider) {
                        currentObjForRelease = (IDProvider) object;
                    } else {
                        currentObjForRelease = ((ReferenceEntry) object).getReferencedElement();
                    }
                    // release only referenced media, workflow object and unreleased parent
                    // pagereffolders
                    if (currentObjForRelease == workflowScriptContext.getElement()
                        || (isPageRef(workflowScriptContext.getElement())
                                && currentObjForRelease == ((PageRef) workflowScriptContext.getElement()).getPage())
                        || isMedia(currentObjForRelease) || isPageRefFolder(currentObjForRelease) || isSiteStoreRoot(currentObjForRelease)
                        || customReleaseElements.contains(currentObjForRelease)
                        || ((new FormEvaluator(workflowScriptContext)).getCheckboxValue(WorkflowConstants.RECURSIVE_FORM_REFNAME)
                                    && (isPageFolder(currentObjForRelease) || isPage(currentObjForRelease) || isMediaFolder(currentObjForRelease)
                                        || isMedia(currentObjForRelease) || isPageRefFolder(currentObjForRelease) || isPageRef(currentObjForRelease)
                                        || isDocumentGroup(currentObjForRelease) || isGcaFolder(currentObjForRelease)
                                        || isGcaPage(currentObjForRelease)))) {
                        if (workflowScriptContext.getProject().getId() == currentObjForRelease.getProject().getId()) {
                            Logging.logInfo("Prepare " + (checkOnly ? "test " : "") + "release for: " + currentObjForRelease.getId(), LOGGER);
                            // only release items that are not yet released
                            if ((currentObjForRelease.getReleaseStatus() != IDProvider.RELEASED || releaseRecursively)
                                && !isTemplate(currentObjForRelease) && !isDataSource(currentObjForRelease) && !isDataSourceFolder(currentObjForRelease)) {
                                // check rules
                                final String validationError = new FormValidator(workflowScriptContext).isValid(currentObjForRelease);
                                if (validationError != null) {
                                    validationErrorList.add(validationError);
                                }
                                // check rules for sections of pages (as checkrules is not
                                // recursive)
                                if (isPage(currentObjForRelease)) {
                                    for (final Section<?> section : currentObjForRelease.getChildren(Section.class, true)) {
                                        final String validationErrorsSection = new FormValidator(workflowScriptContext).isValid(section);
                                        if (validationErrorsSection != null) {
                                            validationErrorList.add(validationErrorsSection);
                                        }
                                    }
                                }
								if (releaseRecursively && isChildrenOf(currentObjForRelease, workflowScriptContext.getElement())) {
									continue;
								}
                                if (validationErrorList.isEmpty()) {
                                    // check release
                                    if (currentObjForRelease == workflowScriptContext.getElement()) {
                                        // unlock element that runs the workflow
                                        currentObjForRelease.setLock(false, false);
                                    }

                                    if (isPageRef(currentObjForRelease)) {
                                        // in order to decide if a pageref can be released, one has
                                        // to check if the referenced page exists
                                        final IDProvider.DependentReleaseType releaseType;
                                        if(checkOnly) {
                                            releaseType = IDProvider.DependentReleaseType.DEPENDENT_RELEASE_NEW_ONLY;
                                        } else {
                                            releaseType = IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE;
                                        }
                                        handle = AccessUtil.release(currentObjForRelease, checkOnly, true, false, releaseType);

                                    } else if (isPageRefFolder(currentObjForRelease)) {
                                        // in order to decide if a pageref can be released, one has
                                        // to check if the referenced page exists
                                        final IDProvider.DependentReleaseType releaseType;
                                        if(checkOnly) {
                                            releaseType = IDProvider.DependentReleaseType.DEPENDENT_RELEASE_NEW_ONLY;
                                        } else {
                                            releaseType = IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE;
                                        }
                                        handle = AccessUtil.release(currentObjForRelease, checkOnly, true, releaseRecursively, releaseType);

                                    } else if (isPage(currentObjForRelease)) {
                                        handle = AccessUtil.release(currentObjForRelease, checkOnly, true, false,
                                            IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);

                                    } else if (isPageFolder(currentObjForRelease)) {
                                        handle = AccessUtil.release(currentObjForRelease, checkOnly, true, releaseRecursively,
                                                IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);

                                    } else if (isDocumentGroup(currentObjForRelease)) {
                                        handle = AccessUtil.release(currentObjForRelease, checkOnly, true, false,
                                            IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);

                                    } else if (isMedia(currentObjForRelease)) {
                                        handle = AccessUtil.release(currentObjForRelease, checkOnly, true, false,
                                            IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);

                                    } else if (isMediaFolder(currentObjForRelease)) {
                                        handle = AccessUtil.release(currentObjForRelease, checkOnly, true, releaseRecursively,
                                                IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);

                                    } else if (isGcaPage(currentObjForRelease)) {
                                        handle = AccessUtil.release(currentObjForRelease, checkOnly, false, false,
                                            IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);

                                    } else if (isGcaFolder(currentObjForRelease)) {
                                        handle = AccessUtil.release(currentObjForRelease, checkOnly, false, releaseRecursively,
                                                IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);

                                    } else if (currentObjForRelease instanceof ProjectProperties) {
                                        handle = AccessUtil.release(currentObjForRelease, checkOnly, false, false,
                                            IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);

                                    } else if (isSiteStoreRoot(currentObjForRelease)) {
                                        handle = AccessUtil.release(currentObjForRelease, checkOnly, false, releaseRecursively,
                                                IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);
                                    }

                                    result = handleResult(lockedList, permList, handle, currentObjForRelease);
                                    if (currentObjForRelease.equals(workflowScriptContext.getElement())) {
                                        currentObjForRelease.setLock(true, false);
                                    }
                                } else {
                                    Logging.logError("Validation failure during release!", LOGGER);
                                    result = false;
                                }
                            }
                        }
                    }

                }
            }
        } catch (final Exception e) {
            Logging.logError("Exception during Release ", e, LOGGER);
            result = false;
        }
        return result;
    }

    private static boolean isDataSourceFolder(final IDProvider idProvider) {
        return idProvider instanceof ContentFolder;
    }

    private static boolean isDataSource(final IDProvider idProvider) {
        return idProvider instanceof Content2;
    }

    private static boolean isTemplate(final IDProvider idProvider) {
        return idProvider instanceof TemplateStoreElement;
    }

    private static boolean isSiteStoreRoot(final IDProvider idProvider) {
        return idProvider instanceof SiteStoreRoot;
    }

    private static boolean isGcaPage(final IDProvider idProvider) {
        return idProvider instanceof GCAPage;
    }

    private static boolean isGcaFolder(final IDProvider idProvider) {
        return idProvider instanceof GCAFolder;
    }

    private static boolean isDocumentGroup(final IDProvider idProvider) {
        return idProvider instanceof DocumentGroup;
    }

    private static boolean isMediaFolder(final IDProvider idProvider) {
        return idProvider instanceof MediaFolder;
    }

    private static boolean isPageRefFolder(final IDProvider idProvider) {
        return idProvider instanceof PageRefFolder;
    }

    private static boolean isMedia(final IDProvider idProvider) {
        return idProvider instanceof Media;
    }

    private static boolean isPageFolder(final IDProvider idProvider) {
        return idProvider instanceof PageFolder;
    }

    private static boolean isPage(final IDProvider idProvider) {
        return idProvider instanceof Page;
    }

    private static boolean isPageRef(final IDProvider idProvider) {
        return idProvider instanceof PageRef;
    }


    private boolean isChildrenOf(final IDProvider children, final IDProvider parent) {
        if (children.getStore() != parent.getStore()) {
            return false;
        }
        IDProvider currentParent = children.getParent();
        while (currentParent != null) {
            if (currentParent.getId() == parent.getId()) {
                return true;
            }
            currentParent = currentParent.getParent();
        }
        return false;
    }


    private static boolean handleResult(final Set<Long> lockedList, final Set<Long> permList, final ServerActionHandle<? extends ReleaseProgress, Boolean> handle,
                                        final IDProvider idProvider) {
        boolean result = true;
        if (handle != null) {
            try {
                handle.checkAndThrow();
                result = handle.getResult();
                Logging.logInfo("Release Result: " + result, LOGGER);
                final ReleaseProgress progress = handle.getProgress(true);
                final Set<Long> lockedFailed = progress.getLockFailedElements();
                final Set<Long> missingPermission = progress.getMissingPermissionElements();
                Logging.logInfo("Released Elements:", LOGGER);
                for (final Long released : progress.getReleasedElements()) {
                    Logging.logInfo("  id:" + released, LOGGER);
                }
                if (lockedFailed != null && !lockedFailed.isEmpty()) {
                    lockedList.addAll(lockedFailed);
                    result = false;
                }
                if (missingPermission != null && !missingPermission.isEmpty()) {
                    permList.addAll(missingPermission);
                    result = false;
                }
            } catch (final Exception e) {
                Logging.logError("Exception during Release of " + idProvider, e, LOGGER);
                result = false;
            }
        }
        return result;
    }

    private List<IDProvider> getCustomReleaseElements(final String type) {
        final List<IDProvider> customReleaseElements = new ArrayList<>();
        if (type.equals(WorkflowConstants.RELEASE_PAGEREF_ELEMENTS)) {
            final Object releasePageRefElements = workflowScriptContext.getSession().get(WorkflowConstants.RELEASE_PAGEREF_ELEMENTS);
            if (releasePageRefElements != null) {
                @SuppressWarnings("unchecked")
                final List<String> releasePageRefUids = (List<String>) releasePageRefElements;
                for (final String pageRefUid : releasePageRefUids) {
                    final PageRef pageRef = new StoreUtil(workflowScriptContext).loadPageRefByUid(pageRefUid);
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
     * @param content2 The content2 object of the entity.
     * @param entity The Entity to release.
     * @param checkOnly Determines if the method should do only a check or really release the object.
     * @return true if successful.
     */
    private boolean releaseEntity(final Content2 content2, final Entity entity, final boolean checkOnly) {
        boolean result = true;

        final String validationError = new FormValidator(workflowScriptContext).isValid(content2, entity);
        if (validationError == null) {
            if (!checkOnly) {
                entity.refresh();
                Session session = entity.getSession();
                session.release(entity);
                session.commit();
                content2.refresh();
                content2.getParent().refresh();
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
    private String createErrorString(final Object id) {
        final QueryAgent queryAgent = workflowScriptContext.requireSpecialist(QueryAgent.TYPE);
        final Iterable<IDProvider> hits = queryAgent.answer("fs.id=" + id);
        if (hits.iterator().hasNext()) {
            final IDProvider element = hits.iterator().next();
            if (element.hasUid()) {
                return element.getDisplayName(new FsLocale(workflowScriptContext).getLanguage()) + " (" + element.getUid() + ", " + element.getId()
                    + ")\n";
            } else {
                return "";
            }
        } else {
            Logging.logError("Found no element with id " + id, LOGGER);
            return "";
        }
    }
}
