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
