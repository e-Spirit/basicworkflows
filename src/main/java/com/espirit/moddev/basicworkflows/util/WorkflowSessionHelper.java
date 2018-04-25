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

import java.util.Collections;
import java.util.Map;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;

public class WorkflowSessionHelper {

	private WorkflowSessionHelper() {}


	public static Map<String, IDProvider.UidType> readMapFromSession(final WorkflowScriptContext workflowScriptContext, final String key) {
		final Map<String, IDProvider.UidType> map = readObjectFromSession(workflowScriptContext, key);
		return map == null ? Collections.<String, IDProvider.UidType> emptyMap() : map;
	}


	public static boolean readBooleanFromSession(final WorkflowScriptContext workflowScriptContext, final String key) {
		final Boolean value = readObjectFromSession(workflowScriptContext, key);
		return value != null && value;
	}


	/**
	 * Read object from session.
	 *
	 * @param <T> the type parameter
	 * @param workflowScriptContext the workflow script context
	 * @param key the key
	 * @return the t
	 */
	@SuppressWarnings("unchecked")
	public static <T> T readObjectFromSession(final WorkflowScriptContext workflowScriptContext, final String key) {
		return (T) workflowScriptContext.getSession().get(key); // NOSONAR
	}
}
