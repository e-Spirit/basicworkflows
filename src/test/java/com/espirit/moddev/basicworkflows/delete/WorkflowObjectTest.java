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

import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.access.ReferenceEntry;
import de.espirit.firstspirit.access.project.Project;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.templatestore.TemplateStoreElement;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;
import de.espirit.firstspirit.agency.UIAgent;
import org.junit.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * @author gremplewski
 */
public class WorkflowObjectTest {

	@Test
	public void testGetRefObjectsFromTemplateStoreElement() throws Exception {
		ReferenceEntry[] referenceEntries = new ReferenceEntry[1];
		referenceEntries[0] = mock(ReferenceEntry.class);
		IDProvider referencedElement = mock(IDProvider.class);
		when(referenceEntries[0].getReferencedElement()).thenReturn(referencedElement);

		TemplateStoreElement templateStoreElement = mock(TemplateStoreElement.class);
		when(templateStoreElement.getIncomingReferences()).thenReturn(referenceEntries);

		Language language = mock(Language.class);
		when(language.getLocale()).thenReturn(Locale.GERMANY);

		UIAgent uiAgent = mock(UIAgent.class);
		when(uiAgent.getDisplayLanguage()).thenReturn(language);

		Project project = mock(Project.class);
		when(project.getLanguage(Locale.GERMANY.getLanguage().toUpperCase())).thenReturn(language);

		WorkflowScriptContext workflowScriptContext = mock(WorkflowScriptContext.class);
		when(workflowScriptContext.getWorkflowable()).thenReturn(templateStoreElement);
		when(workflowScriptContext.requireSpecialist(UIAgent.TYPE)).thenReturn(uiAgent);
		when(workflowScriptContext.getProject()).thenReturn(project);

		WorkflowObject workflowObject = new WorkflowObject(workflowScriptContext);
		List<Object> refObjectsFromStoreElement = workflowObject.getRefObjectsFromStoreElement();

		verify(templateStoreElement).getIncomingReferences();

		assertNotNull("List of referenced objects is null.", refObjectsFromStoreElement);
		assertEquals("Number of referenced objects does not match expected amount.", referenceEntries.length, refObjectsFromStoreElement.size());

		for (int i = 0; i < referenceEntries.length; i++) {
			assertEquals("Referenced element does not equal expected element.", referenceEntries[i].getReferencedElement(), refObjectsFromStoreElement.get(i));
		}
	}
}
