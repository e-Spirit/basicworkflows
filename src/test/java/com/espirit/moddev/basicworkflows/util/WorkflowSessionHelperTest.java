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
package com.espirit.moddev.basicworkflows.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;

public class WorkflowSessionHelperTest {

	@Test
	public void testReadObjectFromSession() throws Exception {
		String key = "TEST";

		Map<Object, Object> sessionMap = new HashMap<>();
		Object objectForSessionMap = new Object();
		sessionMap.put(key, objectForSessionMap);

		WorkflowScriptContext workflowScriptContext = mock(WorkflowScriptContext.class);
		when(workflowScriptContext.getSession()).thenReturn(sessionMap);

		Object readObjectFromSession = WorkflowSessionHelper.readObjectFromSession(workflowScriptContext, key);
		assertEquals(objectForSessionMap, readObjectFromSession);
	}


	@Test
	public void testReadBooleanFromSession() throws Exception {
		String key = "TEST";

		Map<Object, Object> sessionMap = new HashMap<>();

		WorkflowScriptContext workflowScriptContext = mock(WorkflowScriptContext.class);
		when(workflowScriptContext.getSession()).thenReturn(sessionMap);

		Object booleanFromSession = WorkflowSessionHelper.readBooleanFromSession(workflowScriptContext, key);
		assertEquals(false, booleanFromSession);

		sessionMap.put(key, false);
		booleanFromSession = WorkflowSessionHelper.readBooleanFromSession(workflowScriptContext, key);
		assertEquals(false, booleanFromSession);

		sessionMap.put(key, true);
		booleanFromSession = WorkflowSessionHelper.readBooleanFromSession(workflowScriptContext, key);
		assertEquals(true, booleanFromSession);
	}


	@Test
	public void testReadMapFromSession() throws Exception {
		String key = "TEST";

		Map<Object, Object> sessionMap = new HashMap<>();

		WorkflowScriptContext workflowScriptContext = mock(WorkflowScriptContext.class);
		when(workflowScriptContext.getSession()).thenReturn(sessionMap);

		Map<String, IDProvider.UidType> mapFromSession = WorkflowSessionHelper.readMapFromSession(workflowScriptContext, key);
		assertEquals(true, mapFromSession.isEmpty());

		Map<String, IDProvider.UidType> elementMap = new HashMap<>();
		sessionMap.put(key, elementMap);
		mapFromSession = WorkflowSessionHelper.readMapFromSession(workflowScriptContext, key);
		assertEquals(elementMap, mapFromSession);
	}
}
