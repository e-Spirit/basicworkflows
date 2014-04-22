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

package com.espirit.moddev.basicworkflows.delete;

import com.espirit.moddev.basicworkflows.util.FsException;
import com.espirit.moddev.basicworkflows.util.FsLocale;
import com.espirit.moddev.basicworkflows.util.WorkflowConstants;
import de.espirit.common.TypedFilter;
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
import de.espirit.firstspirit.access.store.pagestore.Page;
import de.espirit.firstspirit.access.store.pagestore.PageFolder;
import de.espirit.firstspirit.access.store.pagestore.Section;
import de.espirit.firstspirit.access.store.sitestore.DocumentGroup;
import de.espirit.firstspirit.access.store.sitestore.PageRef;
import de.espirit.firstspirit.access.store.sitestore.PageRefFolder;
import de.espirit.firstspirit.access.store.templatestore.*;
import de.espirit.firstspirit.store.access.globalstore.GlobalContentAreaImpl;
import de.espirit.or.schema.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * This class provides methods to get the incoming references of the worklfow object and store them in the session.
 *
 * @author stephan
 * @since 1.0
 */
public class WorkflowObject {
    /** The storeElement to use. */
    private StoreElement storeElement;
    /** The workflowScriptContext from the workflow. */
    private WorkflowScriptContext workflowScriptContext;
    /** The content2 object from the workflow. */
    private Content2 content2;
    /** The Entity to use. */
    private Entity entity;
    /** The ResourceBundle that contains language specific labels. */
    private ResourceBundle bundle;

    /**
     * Constructor for WorkflowObject.
     *
     * @param workflowScriptContext The workflowScriptContext from the workflow.
     */
    public WorkflowObject(WorkflowScriptContext workflowScriptContext) {
        this.workflowScriptContext = workflowScriptContext;
        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(WorkflowConstants.MESSAGES, new FsLocale(workflowScriptContext).get());

        if(workflowScriptContext.getWorkflowable() instanceof ContentWorkflowable) {
            content2 = ((ContentWorkflowable) workflowScriptContext.getWorkflowable()).getContent();
            entity = ((ContentWorkflowable) workflowScriptContext.getWorkflowable()).getEntity();
        } else {
            storeElement = (StoreElement) workflowScriptContext.getWorkflowable();
        }

    }


    /**
     * Method to get referenced objects of an entity.
     *
     * @return the list of referenced objects.
     */
    public List<Object> getRefObjectsFromEntity() {
        ArrayList<Object> referencedObjects= new ArrayList<Object>();

        for(ReferenceEntry referenceEntry : content2.getSchema().getIncomingReferences(entity)) {
            referencedObjects.add(referenceEntry.getReferencedObject());
        }
        storeReferences(referencedObjects);
        return referencedObjects;
    }


    /**
     * Method to get referenced objects of an IDProvider.
     *
     * @return the list of referenced objects.
     */
    public List<Object> getRefObjectsFromStoreElement() {
        ArrayList<Object>referencedObjects = new ArrayList<Object>();

        if (storeElement instanceof PageRef) {
            // add outgoing references
            referencedObjects.addAll(getReferences(storeElement));

            // in case of webedit add page references
            if(workflowScriptContext.is(BaseContext.Env.WEBEDIT)) {
                referencedObjects.addAll(getReferences(((PageRef) storeElement).getPage()));
                // remove added workflow element
                referencedObjects.remove(storeElement);

/** documentation example - begin **/
                for (Section section : ((PageRef) storeElement).getPage().getChildren(Section.class, true)) {
                    referencedObjects.addAll(getReferences(section));
                }
/** documentation example - end **/

            }

        } else if (storeElement instanceof DocumentGroup
	                || storeElement instanceof PageRefFolder
	                || storeElement instanceof GCAPage
	                || storeElement instanceof PageFolder
	                || storeElement instanceof Media
	                || storeElement instanceof MediaFolder) {
			// add outgoing references
			referencedObjects.addAll(getReferences(storeElement));

        } else if (storeElement instanceof Page) {
            // add outgoing references
            referencedObjects.addAll(getReferences(storeElement));

/** documentation example - begin **/
            for (Section section : storeElement.getChildren(Section.class, true)) {
                referencedObjects.addAll(getReferences(section));
            }
/** documentation example - end **/

        } else if (storeElement instanceof GlobalContentAreaImpl) {
            // Element is a content folder object -> aborting" (make sure this test occurs before the gcafolder-test)
	        abortDeletion("deleteGCAnotPossible");

        } else if (storeElement instanceof GCAFolder) {
            // add outgoing references
            referencedObjects.addAll(getReferences(storeElement));

        } else if (storeElement instanceof TemplateStoreElement || storeElement instanceof Query) { // In FirstSpirit 5.0 Query does not inherit from TemplateStoreElement
	        // add outgoing references
	        referencedObjects.addAll(getReferences(storeElement));

	        if (storeElement instanceof Schema && referencedObjects.isEmpty()) {
		        TypedFilter<StoreElement> filter = new TypedFilter<StoreElement>(StoreElement.class) {
			        @Override
			        public boolean accept(final StoreElement storeElement) {
				        return storeElement instanceof TableTemplate || storeElement instanceof Query;
			        }
		        };

		        for (StoreElement childElement : storeElement.getChildren(filter, true)) {
			        referencedObjects.addAll(getReferences(childElement));
		        }
	        }

        } else if (storeElement instanceof ProjectProperties) {
            // Element is a project property object -> aborting"
	        abortDeletion("deletePPnotPossible");

        } else if (storeElement instanceof Content2) {
            // Element is a content2 folder object -> aborting"
	        abortDeletion("deleteC2notPossible");

        } else if (storeElement instanceof ContentFolder) {
            // Element is a content folder object -> aborting"
	        abortDeletion("deleteCFnotPossible");
        }
        storeReferences(referencedObjects);
        return referencedObjects;
    }

	private void abortDeletion(String errorMessageKey) {
		workflowScriptContext.gotoErrorState(bundle.getString(errorMessageKey), new FsException());
	}

	/**
     * Convenience method to get referenced objects of storeElement and its parents.
     *
     * @param storeElement The storeElement to get references from.
     * @return the list of references.
     */
    private static List<IDProvider> getReferences(StoreElement storeElement) {
        ArrayList<IDProvider> references = new ArrayList<IDProvider>();

        // add outgoing references
        for(ReferenceEntry referenceEntry : storeElement.getIncomingReferences()) {
            references.add(referenceEntry.getReferencedElement());
        }

        return references;
    }


    /**
     * Convenience method to store uid names of objects that reference the object to be deleted in the workflow session.
     *
     * @param refObjects The list of references to store in the session.
     */
    public void storeReferences(List<Object> refObjects) {
        ArrayList<String> referencedObjects = new ArrayList<String>();
        for(Object obj : refObjects) {
            if(obj  instanceof Section) {
                Section section = (Section) obj ;
                referencedObjects.add(section.getDisplayName(new FsLocale(workflowScriptContext).getLanguage()) + " (" + section.getName() + ", " + section.getId() + ")");
            } else if (obj instanceof Entity) {
                Entity ent = (Entity) obj;
                referencedObjects.add(ent.getIdentifier().getEntityTypeName() + " (" + ent.getIdentifier().getEntityTypeName() + ", ID#" + ent.get("fs_id") + ")");
            } else {
                IDProvider idProv = (IDProvider) obj;
                if(idProv.hasUid()) {
                    referencedObjects.add(idProv.getDisplayName(new FsLocale(workflowScriptContext).getLanguage()) + " (" + idProv.getUid() + ", " + idProv.getId() + ")");
                } else {
                    referencedObjects.add(idProv.getDisplayName(new FsLocale(workflowScriptContext).getLanguage()) + " (" + idProv.getName() + ", " + idProv.getId() + ")");
                }
            }
        }
        // put referenced objects to session for further use
        workflowScriptContext.getSession().put("wfReferencedObjects", referencedObjects);
    }

    /**
     * Convenience method to get an ID from an entity/storeElement.
     *
     * @return the ID.
     */
    public String getId() {
        if(storeElement != null) {
            return storeElement.getName();
        } else {
            return entity.getKeyValue().toString();
        }
    }

}
