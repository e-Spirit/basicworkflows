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

import com.espirit.moddev.basicworkflows.util.WorkflowExecutable;
import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.script.Executable;
import de.espirit.firstspirit.access.store.StoreElement;
import de.espirit.firstspirit.access.store.StoreElementFolder;
import de.espirit.firstspirit.access.store.contentstore.Content2;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;

import java.util.Map;

public class WfBranchIfFolderExecutable extends WorkflowExecutable implements Executable {

	public static final Class<?> LOGGER = WfBranchIfFolderExecutable.class;

	public Object execute(Map<String, Object> params) {
		WorkflowScriptContext context = (WorkflowScriptContext) params.get("context");
		StoreElement element = context.getStoreElement();
		String transition = null;

		if (element instanceof StoreElementFolder && !(element instanceof Content2)) {
			transition = "folder";
		} else if (element instanceof StoreElement) {
			transition = "element";
		}

		try {
			context.doTransition(transition);
		} catch (IllegalAccessException e) {
			Logging.logError("Element type check failed!", e, LOGGER);
		}
		return true;
	}
}
