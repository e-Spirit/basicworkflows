/*
 * **********************************************************************
 * basicworkflows
 * %%
 * Copyright (C) 2012 - 2015 e-Spirit AG
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

import de.espirit.firstspirit.access.AccessUtil;
import de.espirit.firstspirit.access.Task;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.Store;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.*;

/**
 * @author gremplewski
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(AccessUtil.class)
public class DeleteObjectTest {

	private WorkflowScriptContext workflowScriptContext;
	private IDProvider elementToBeDeleted;
	private Store store;

	@Before
	public void initMocks() {
		PowerMockito.mockStatic(AccessUtil.class);
		when(AccessUtil.delete(anyCollectionOf(IDProvider.class), eq(true))).thenReturn(null);

		Task task = mock(Task.class);
		store = mock(Store.class);

		elementToBeDeleted = mock(IDProvider.class);
		when(elementToBeDeleted.getStore()).thenReturn(store);

		workflowScriptContext = mock(WorkflowScriptContext.class);
		when(workflowScriptContext.getWorkflowable()).thenReturn(null);
		when(workflowScriptContext.getElement()).thenReturn(elementToBeDeleted);
		when(workflowScriptContext.getTask()).thenReturn(task);
	}

	/**
	 * Tests that the parent of a deleted template store element isn't booked for release
	 */
	@Test
	public void testDeleteTemplateStoreElement() {
		when(store.getType()).thenReturn(Store.Type.TEMPLATESTORE);
		runDelete();
		verify(elementToBeDeleted, never()).getParent();
	}

	/**
	 * Tests that the parent of a deleted non template store element is booked for release
	 */
	@Test
	public void testDeleteNonTemplateStoreElement() {
		when(store.getType()).thenReturn(Store.Type.PAGESTORE);
		runDelete();
		verify(elementToBeDeleted).getParent();
	}

	private void runDelete() {
		DeleteObject deleteObject = new DeleteObject(workflowScriptContext);
		deleteObject.delete(false);
	}
}
