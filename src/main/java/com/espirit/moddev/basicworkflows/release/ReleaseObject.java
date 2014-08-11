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

import com.espirit.moddev.basicworkflows.util.FormValidator;
import com.espirit.moddev.basicworkflows.util.FsLocale;
import com.espirit.moddev.basicworkflows.util.WorkflowConstants;
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
import de.espirit.firstspirit.access.store.templatestore.TemplateStoreElement;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;
import de.espirit.firstspirit.agency.OperationAgent;
import de.espirit.firstspirit.agency.QueryAgent;
import de.espirit.firstspirit.ui.operations.RequestOperation;
import de.espirit.or.schema.Entity;

import java.util.*;

/**
 * This class is used to release IDProvider/Entity objects.
 *
 * @author stephan
 * @since 1.0
 */
public class ReleaseObject {
    /** The Entity to be released. */
    private Entity entity;
    /** The workflowScriptContext from the workflow. */
    private WorkflowScriptContext workflowScriptContext;
    /** The Objects to be released. */
    private List<Object> releaseObjects;
    /** The ResourceBundle that contains language specific labels. */
    private ResourceBundle bundle;
    /** The List of validation Errors. */
    private List<String> validationErrorList = new ArrayList<String>();
    /** The logging class to use. */
    public static final Class<?> LOGGER = ReleaseObject.class;

    /**
     * Constructor for ReleaseObject with an Entity.
     *
     * @param workflowScriptContext The workflowScriptContext from the workflow.
     * @param releaseEntity The Entity that is to be released.
     */
    public ReleaseObject(WorkflowScriptContext workflowScriptContext, Entity releaseEntity) {
        this.entity = releaseEntity;
        this.workflowScriptContext = workflowScriptContext;
        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(WorkflowConstants.MESSAGES, new FsLocale(workflowScriptContext).get());
    }

    /**
     * Constructor for ReleaseObject with a list of Objects.
     *
     * @param workflowScriptContext The workflowScriptContext from the workflow.
     * @param releaseObjects The list of objects that are to be released.
     */
    public ReleaseObject(WorkflowScriptContext workflowScriptContext, List<Object> releaseObjects) {
        this.workflowScriptContext = workflowScriptContext;
        this.releaseObjects = releaseObjects;
        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(WorkflowConstants.MESSAGES, new FsLocale(workflowScriptContext).get());
    }


    /**
     * Main release method that distinguishes between Entity and StoreElement.
     * StoreElements are released within this method.
     * For entities the releaseEntity method is called.
     *
     * @param checkOnly Determines if the method should do only a check or really release the object.
     * @return true if successful.
     */
    public boolean release(boolean checkOnly) {
        boolean result = true;
        HashSet<Long> lockedList = new HashSet<Long>();
        HashSet<Long> permList = new HashSet<Long>();

        // release entity
        if(this.entity != null) {
            final ContentWorkflowable contentWorkflowable = (ContentWorkflowable) workflowScriptContext.getWorkflowable();
            final Content2 content2 = contentWorkflowable.getContent();
            result = releaseEntity(content2, this.entity, checkOnly);
        } else {
            // release StoreElement
            ServerActionHandle<? extends ReleaseProgress, Boolean> handle = null;

            try{
                for(Object object : releaseObjects) {
                    if(!(object instanceof ReferenceEntry && ((ReferenceEntry) object).getReferencedObject() instanceof Entity)) {
                        IDProvider idProvider;
                        if (object instanceof IDProvider) {
                            idProvider = (IDProvider) object;
                        } else {
                            idProvider = ((ReferenceEntry) object).getReferencedElement();
                        }


                        // release only referenced media and workflow object

                        if(idProvider == workflowScriptContext.getStoreElement() || (workflowScriptContext.getStoreElement() instanceof PageRef && idProvider == ((PageRef) workflowScriptContext.getStoreElement()).getPage()) || idProvider instanceof Media) {
                            Logging.logInfo("Prepare release for: " + idProvider.getId(), LOGGER);

                            // only release items that are not yet released
                            if(idProvider.getReleaseStatus() != IDProvider.RELEASED && !(idProvider instanceof TemplateStoreElement) && !(idProvider instanceof Content2) && !(idProvider instanceof ContentFolder)) {
                                // check rules
                                String validationError = new FormValidator(workflowScriptContext).isValid(idProvider);
                                if(validationError != null) {
                                    validationErrorList.add(validationError);
                                }
                                // check rules for sections of pages (as checkrules is not recursive)
                                if(idProvider instanceof Page) {
                                    for(Section section : idProvider.getChildren(Section.class, true)) {
                                        String validationErrorsSection = new FormValidator(workflowScriptContext).isValid(section);
                                        if(validationErrorsSection != null) {
                                            validationErrorList.add(validationErrorsSection);
                                        }
                                    }
                                }
                                if(validationErrorList.isEmpty()) {
                                    // check release
                                    if(idProvider == workflowScriptContext.getStoreElement()) {
                                        // unlock element that runs the workflow
                                        idProvider.setLock(false,false);
                                    }
                                    //release(IDProvider releaseStartNode, boolean checkOnly, boolean releaseParentPath, boolean recursive, IDProvider.DependentReleaseType dependentType)
                                    if (idProvider instanceof PageRef) {
                                        handle = AccessUtil.release(idProvider, checkOnly, true, false, IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);
                                    } else if (idProvider instanceof PageRefFolder) {
                                        handle = AccessUtil.release(idProvider, checkOnly, true, false, IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);
                                    } else if (idProvider instanceof Page) {
                                        handle = AccessUtil.release(idProvider, checkOnly, true, false, IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);
                                    } else if (idProvider instanceof PageFolder) {
                                        handle = AccessUtil.release(idProvider, checkOnly, true, false, IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);
                                    } else if (idProvider instanceof DocumentGroup) {
                                        handle = AccessUtil.release(idProvider, checkOnly, true, false, IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);
                                    } else if (idProvider instanceof Media) {
                                        handle = AccessUtil.release(idProvider, checkOnly, true, false, IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);
                                    } else if (idProvider instanceof MediaFolder) {
                                        handle = AccessUtil.release(idProvider, checkOnly, true, false, IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);
                                    } else if (idProvider instanceof GCAPage) {
                                        handle = AccessUtil.release(idProvider, checkOnly, false, false, IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);
                                    } else if (idProvider instanceof GCAFolder) {
                                        handle = AccessUtil.release(idProvider, checkOnly, false, false, IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);
                                    } else if (idProvider instanceof ProjectProperties) {
                                        handle = AccessUtil.release(idProvider, checkOnly, false, false, IDProvider.DependentReleaseType.NO_DEPENDENT_RELEASE);
                                    }
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
                                                if (missingPermission.size() > 0) {
                                                    for (Long missing : missingPermission) {
                                                        permList.add(missing);
                                                    }
                                                }
                                                result = false;
                                            }
                                        } catch (Exception e) {
                                            Logging.logError("Exception during Release of " + idProvider, e, LOGGER);
                                            result = false;
                                        }
                                    }
                                    if(idProvider == workflowScriptContext.getStoreElement()) {
                                        idProvider.setLock(true,false);
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
        }

		final String suppressDialog = (String) workflowScriptContext.getSession().get("wfSuppressDialog"); // set in integration tests
		if (!"true".equals(suppressDialog)) {
			// show locked elements if any
			if(!lockedList.isEmpty()) {
				Logging.logInfo("LockFailedElements:", LOGGER);
				StringBuilder errorMsg = new StringBuilder(bundle.getString("errorLocked")).append(":\n\n");

				for (Object locked : lockedList) {
					Logging.logInfo("  id:" + locked, LOGGER);
					errorMsg.append(createErrorString(locked));
				}
				try {
					// get operation agent
					OperationAgent operationAgent = workflowScriptContext.requireSpecialist(OperationAgent.TYPE);
					// get request operation
					RequestOperation requestOperation = operationAgent.getOperation(RequestOperation.TYPE);
					// perform request to open dialog
					requestOperation.setTitle(bundle.getString("errorLocked"));
					requestOperation.perform(errorMsg.toString());
				} catch (IllegalStateException e) {
					Logging.logWarning("Displaying locked elements failed.", e, LOGGER);
				}
			}

			// show elements without permission if any
			if(!permList.isEmpty()) {
				StringBuilder errorMsg = new StringBuilder(bundle.getString("errorPermission")).append(":\n\n");
				Logging.logInfo("MissingPermissionElement", LOGGER);
				for (Long missing : permList) {
					Logging.logInfo("  id:" + missing, LOGGER);
					errorMsg.append(createErrorString(missing));
				}
				try {
					// get operation agent
					OperationAgent operationAgent = workflowScriptContext.requireSpecialist(OperationAgent.TYPE);
					// get request operation
					RequestOperation requestOperation = operationAgent.getOperation(RequestOperation.TYPE);
					// perform request to open dialog
					requestOperation.setTitle(bundle.getString("errorPermission"));
					requestOperation.perform(errorMsg.toString());
				} catch (IllegalStateException e) {
					Logging.logWarning("Displaying elements without permission failed.", e, LOGGER);
				}
			}

			// show invalid elements if any
			if(!validationErrorList.isEmpty()) {
				// show dialog
				try {
					// get operation agent
					OperationAgent operationAgent = workflowScriptContext.requireSpecialist(OperationAgent.TYPE);
					// get request operation
					RequestOperation requestOperation = operationAgent.getOperation(RequestOperation.TYPE);
					// perform request to open dialog
					requestOperation.setTitle(bundle.getString("errorValidation"));
					StringBuilder errorMsg = new StringBuilder();
					for(String validationError : validationErrorList) {
						errorMsg.append(validationError);
					}
					requestOperation.perform(errorMsg.toString());
				} catch (IllegalStateException e) {
					Logging.logWarning("Displaying unreleased elements failed.", e, LOGGER);
				}
			}
		}
        return result;
    }

    /**
     * This method is used to release an entity.
     *
     * @param content2 The content2 object of the entity.
     * @param entity The Entity to release.
     * @param checkOnly Determines if the method should do only a check or really release the object.
     * @return true if successful.
     */
    private boolean releaseEntity(Content2 content2, Entity entity, boolean checkOnly) {
        boolean result = true;
        String validationError = new FormValidator(workflowScriptContext).isValid(content2, entity);
        if(validationError == null) {
            if(!checkOnly) {
                try {
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
        if(element.hasUid()) {
            return element.getDisplayName(new FsLocale(workflowScriptContext).getLanguage()) + " (" + element.getUid() + ", " + element.getId() + ")\n";
        } else {
            return "";
        }

    }
}
