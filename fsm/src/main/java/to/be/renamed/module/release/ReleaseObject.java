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

import to.be.renamed.module.util.Dialog;
import to.be.renamed.module.util.FormEvaluator;
import to.be.renamed.module.util.FormValidator;
import to.be.renamed.module.util.FsLocale;
import to.be.renamed.module.util.StoreComparator;
import to.be.renamed.module.util.StoreUtil;
import to.be.renamed.module.util.WorkflowConstants;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.access.ReferenceEntry;
import de.espirit.firstspirit.access.store.BasicElementInfo;
import de.espirit.firstspirit.access.store.BasicInfo;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.ReleaseProblem;
import de.espirit.firstspirit.access.store.contentstore.Content2;
import de.espirit.firstspirit.access.store.contentstore.ContentFolder;
import de.espirit.firstspirit.access.store.contentstore.ContentWorkflowable;
import de.espirit.firstspirit.access.store.contentstore.Dataset;
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
import de.espirit.firstspirit.agency.OperationAgent;
import de.espirit.firstspirit.agency.QueryAgent;
import de.espirit.firstspirit.store.operations.ReleaseOperation;
import de.espirit.or.Session;
import de.espirit.or.schema.Entity;

import java.util.ArrayList;
import java.util.Arrays;
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

    /**
     * The dialog used for displaying messages to the user.
     */
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
     * @param releaseEntity         The Entity that is to be released.
     */
    ReleaseObject(final WorkflowScriptContext workflowScriptContext, final Entity releaseEntity) {
        this(workflowScriptContext);
        this.entity = releaseEntity;
    }

    /**
     * Constructor for ReleaseObject with a list of Objects.
     *
     * @param workflowScriptContext The workflowScriptContext from the workflow.
     * @param releaseObjects        The list of objects that are to be released.
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
     * @param checkOnly          Determines if the method should do only a check or really release the object.
     * @param releaseRecursively do a recursive release?
     * @param languages          The languages to release.
     * @return true if successful.
     */
    public boolean release(final boolean checkOnly, final boolean releaseRecursively, final Language[] languages) {
        final boolean result;
        final Set<BasicInfo> lockedList = new HashSet<>();
        final Set<BasicInfo> permList = new HashSet<>();

        // release entity
        if (this.entity != null) {
            final ContentWorkflowable contentWorkflowable = (ContentWorkflowable) workflowScriptContext.getWorkflowable();
            final Content2 content2 = contentWorkflowable.getContent();
            result = releaseEntity(content2, this.entity, checkOnly);
        } else {
            result = releaseStoreElement(checkOnly, lockedList, permList, releaseRecursively, languages);
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

    private void showDeniedElementsIfAny(final Set<BasicInfo> permList) {
        if (!permList.isEmpty()) {
            final StringBuilder errorMsg = new StringBuilder(bundle.getString("errorPermission")).append(":\n\n");
            Logging.logInfo("MissingPermissionElement", LOGGER);
            createErrorMessage(permList, errorMsg);
            dialog.showError(bundle.getString("errorPermission"), errorMsg.toString());
        }
    }

    private void createErrorMessage(Set<BasicInfo> infoList, StringBuilder errorMsg) {
        for (final BasicInfo missing : infoList) {
            long missingId = 0;
            if (!missing.isEntity()) {
                BasicElementInfo basicElementInfo = (BasicElementInfo) missing;
                missingId = basicElementInfo.getNodeId();
            }
            Logging.logInfo("  id:" + missingId, LOGGER);
            errorMsg.append(createErrorString(missingId));
        }
    }

    private void showLockedElementsIfAny(final Set<BasicInfo> lockedList) {
        if (!lockedList.isEmpty()) {
            Logging.logInfo("LockFailedElements:", LOGGER);
            final StringBuilder errorMsg = new StringBuilder(bundle.getString("errorLocked")).append(":\n\n");
            createErrorMessage(lockedList, errorMsg);
            dialog.showError(bundle.getString("errorLocked"), errorMsg.toString());
        }
    }

    private boolean releaseStoreElement(final boolean checkOnly, final Set<BasicInfo> lockedList, final Set<BasicInfo> permList,
                                        final boolean releaseRecursively, final Language[] languages) {
        boolean result = true;

        OperationAgent operationAgent = workflowScriptContext.requireSpecialist(OperationAgent.TYPE);
        ReleaseOperation releaseOperation;
        ReleaseOperation.ReleaseResult releaseResult;

        final List<IDProvider> customReleaseElements = getCustomReleaseElements(WorkflowConstants.RELEASE_PAGEREF_ELEMENTS);

        try {
            releaseObjects.sort(new StoreComparator());
            for (final Object object : releaseObjects) {
                if (!(object instanceof ReferenceEntry && ((ReferenceEntry) object).getReferencedObject() instanceof Entity)) {
                    IDProvider currentObjForRelease = getObjectAsIDProvider(object);
                    if (isReleasable(currentObjForRelease, customReleaseElements)) {
                        if (workflowScriptContext.getProject().getId() == currentObjForRelease.getProject().getId()) {
                            Logging.logInfo("Prepare " + (checkOnly ? "test " : "") + "release for: " + currentObjForRelease.getId(), LOGGER);
                            // only release items that are not yet released
                            int
                                releaseStatus =
                                languages.length > 0 ? currentObjForRelease.getReleaseStatus(languages) : currentObjForRelease.getReleaseStatus();
                            if (shouldBeReleased(releaseStatus, releaseRecursively, currentObjForRelease)) {
                                // check rules
                                final String validationError = new FormValidator(workflowScriptContext).isValid(currentObjForRelease);
                                if (validationError != null) {
                                    validationErrorList.add(validationError);
                                }
                                // check rules for sections of pages (as checkrules is not recursive)
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

                                    releaseOperation = operationAgent.getOperation(ReleaseOperation.TYPE);

                                    releaseOperation.checkOnly(checkOnly);
                                    if (languages.length > 0) {
                                        releaseOperation.languages(languages);
                                        Logging.logInfo("Releasing languages: " + Arrays.toString(languages), getClass());
                                    } else {
                                        Logging.logInfo("Releasing all languages.", getClass());
                                    }

                                    releaseResult = performRelease(currentObjForRelease, checkOnly, releaseOperation, releaseRecursively);
                                    result = handleResult(lockedList, permList, releaseResult, currentObjForRelease);
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
                    currentObjForRelease.refresh();
                }
            }
        } catch (final Exception e) {
            Logging.logError("Exception during Release ", e, LOGGER);
            result = false;
        }
        return result;
    }

    private ReleaseOperation.ReleaseResult performRelease(final IDProvider currentObjForRelease, final boolean checkOnly,
                                                          final ReleaseOperation releaseOperation, final boolean releaseRecursively) throws Exception {
        ReleaseOperation.ReleaseResult releaseResult = null;
        if (isPageRef(currentObjForRelease)) {
            // in order to decide if a pageref can be released, one has
            // to check if the referenced page exists
            final IDProvider.DependentReleaseType releaseType;
            if (checkOnly) {
                releaseType = IDProvider.DependentReleaseType.DEPENDENT_RELEASE_NEW_ONLY;
            } else {
                releaseType = IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE;
            }
            releaseResult =
                releaseOperation.ensureAccessibility(true).recursive(false).dependentReleaseType(releaseType)
                    .perform(currentObjForRelease);
        } else if (isPageRefFolder(currentObjForRelease)) {
            // in order to decide if a pageref can be released, one has
            // to check if the referenced page exists
            final IDProvider.DependentReleaseType releaseType;
            if (checkOnly) {
                releaseType = IDProvider.DependentReleaseType.DEPENDENT_RELEASE_NEW_ONLY;
            } else {
                releaseType = IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE;
            }
            releaseResult =
                releaseOperation.ensureAccessibility(true).recursive(releaseRecursively)
                    .dependentReleaseType(releaseType).perform(currentObjForRelease);
        } else if (isPage(currentObjForRelease) || isDocumentGroup(currentObjForRelease) || isMedia(currentObjForRelease)) {
            releaseResult =
                releaseOperation.ensureAccessibility(true).recursive(false)
                    .dependentReleaseType(IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE)
                    .perform(currentObjForRelease);
        } else if (isPageFolder(currentObjForRelease) || isMediaFolder(currentObjForRelease)) {
            releaseResult =
                releaseOperation.ensureAccessibility(true).recursive(releaseRecursively)
                    .dependentReleaseType(IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE)
                    .perform(currentObjForRelease);
        } else if (isGcaPage(currentObjForRelease) || currentObjForRelease instanceof ProjectProperties || isDataset(currentObjForRelease)) {
            releaseResult =
                releaseOperation.ensureAccessibility(false).recursive(false)
                    .dependentReleaseType(IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE)
                    .perform(currentObjForRelease);
        } else if (isGcaFolder(currentObjForRelease) || isSiteStoreRoot(currentObjForRelease)) {
            releaseResult =
                releaseOperation.ensureAccessibility(false).recursive(releaseRecursively)
                    .dependentReleaseType(IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE)
                    .perform(currentObjForRelease);
        }
        return releaseResult;
    }

    private boolean shouldBeReleased(final int releaseStatus, final boolean releaseRecursively, final IDProvider currentObjForRelease) {
        return (releaseStatus != IDProvider.RELEASED || releaseRecursively)
               && !isTemplate(currentObjForRelease) && !isDataSource(currentObjForRelease) && !isDataSourceFolder(
            currentObjForRelease);
    }

    private boolean isReleasable(final IDProvider currentObjForRelease, final List<IDProvider> customReleaseElements) {
        return currentObjForRelease == workflowScriptContext.getElement()
        || (isPageRef(workflowScriptContext.getElement())
            && currentObjForRelease == ((PageRef) workflowScriptContext.getElement()).getPage())
        || isMedia(currentObjForRelease) || isPageRefFolder(currentObjForRelease) || isSiteStoreRoot(currentObjForRelease)
        || customReleaseElements.contains(currentObjForRelease)
        || ((new FormEvaluator(workflowScriptContext)).getCheckboxValue(WorkflowConstants.RECURSIVE_FORM_REFNAME)
            && (isPageFolder(currentObjForRelease) || isPage(currentObjForRelease) || isMediaFolder(currentObjForRelease)
                || isMedia(currentObjForRelease) || isPageRefFolder(currentObjForRelease) || isPageRef(currentObjForRelease)
                || isDocumentGroup(currentObjForRelease) || isGcaFolder(currentObjForRelease)
                || isGcaPage(currentObjForRelease)));
    }

    private IDProvider getObjectAsIDProvider(final Object object) {
        return object instanceof IDProvider ? (IDProvider) object : ((ReferenceEntry) object).getReferencedElement();
    }

    private boolean isDataSourceFolder(final IDProvider idProvider) {
        return idProvider instanceof ContentFolder;
    }

    private boolean isDataSource(final IDProvider idProvider) {
        return idProvider instanceof Content2;
    }

    private boolean isDataset(final IDProvider idProvider) {
        return idProvider instanceof Dataset;
    }

    private boolean isTemplate(final IDProvider idProvider) {
        return idProvider instanceof TemplateStoreElement;
    }

    private boolean isSiteStoreRoot(final IDProvider idProvider) {
        return idProvider instanceof SiteStoreRoot;
    }

    private boolean isGcaPage(final IDProvider idProvider) {
        return idProvider instanceof GCAPage;
    }

    private boolean isGcaFolder(final IDProvider idProvider) {
        return idProvider instanceof GCAFolder;
    }

    private boolean isDocumentGroup(final IDProvider idProvider) {
        return idProvider instanceof DocumentGroup;
    }

    private boolean isMediaFolder(final IDProvider idProvider) {
        return idProvider instanceof MediaFolder;
    }

    private boolean isPageRefFolder(final IDProvider idProvider) {
        return idProvider instanceof PageRefFolder;
    }

    private boolean isMedia(final IDProvider idProvider) {
        return idProvider instanceof Media;
    }

    private boolean isPageFolder(final IDProvider idProvider) {
        return idProvider instanceof PageFolder;
    }

    private boolean isPage(final IDProvider idProvider) {
        return idProvider instanceof Page;
    }

    private boolean isPageRef(final IDProvider idProvider) {
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


    private boolean handleResult(final Set<BasicInfo> lockedList, final Set<BasicInfo> permList, ReleaseOperation.ReleaseResult releaseResult,
                                 final IDProvider idProvider) {
        boolean result = true;
        try {
            Logging.logInfo("Release Result: " + releaseResult.isSuccessful(), LOGGER);

            final Set<BasicInfo> lockedFailed = releaseResult.getProblematicElements().get(ReleaseProblem.LOCK_FAILED);
            final Set<BasicInfo> missingPermission = releaseResult.getProblematicElements().get(ReleaseProblem.MISSING_PERMISSION);
            Logging.logInfo("Released Elements:", LOGGER);
            for (final BasicInfo released : releaseResult.getReleasedElements()) {
                if (!released.isEntity()) {
                    Logging.logInfo("  id:" + ((BasicElementInfo) released).getNodeId(), LOGGER);
                } else {
                    Logging.logInfo("  name:" + released.getName(), LOGGER);
                }
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
        return result;
    }

    private List<IDProvider> getCustomReleaseElements(final String type) {
        final List<IDProvider> customReleaseElements = new ArrayList<>();
        if (type.equals(WorkflowConstants.RELEASE_PAGEREF_ELEMENTS)) {
            final Object releasePageRefElements = workflowScriptContext.getSession().get(WorkflowConstants.RELEASE_PAGEREF_ELEMENTS);
            if (releasePageRefElements != null) {
                @SuppressWarnings("unchecked") final List<String> releasePageRefUids = (List<String>) releasePageRefElements;
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
     * @param content2  The content2 object of the entity.
     * @param entity    The Entity to release.
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
        final Iterable<IDProvider> hits = queryAgent.answer("fs.id=" + id, null);
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
