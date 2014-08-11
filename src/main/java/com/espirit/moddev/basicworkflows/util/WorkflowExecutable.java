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

package com.espirit.moddev.basicworkflows.util;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;
import de.espirit.firstspirit.agency.OperationAgent;
import de.espirit.firstspirit.ui.operations.RequestOperation;

import java.io.Writer;
import java.util.Map;


/**
 * Abstract WorkflowExecutable that contains the basic methods used in all executables.
 *
 * @author stephan
 * @since 1.0
 */
public class WorkflowExecutable {

	/**
	 * The logging class to use.
	 */
	public static final Class<?> LOGGER = WorkflowExecutable.class;


	/**
	 * {@inheritDoc}
	 */
	public Object execute(Map<String, Object> context, Writer out, Writer err) {
		return execute(context);
	}


	/**
	 * Main executable method, replaced by actual implementations.
	 *
	 * @param context The current FirstSpirit context.
	 * @return The context object.
	 */
	protected Object execute(Map<String, Object> context) {
		return context;
	}


	/**
	 * A convenience method to display a message pop-up in the client.
	 *
	 * @param title                 The message title.
	 * @param message               The message to display.
	 * @param workflowScriptContext The context to use.
	 */
	protected void showDialog(WorkflowScriptContext workflowScriptContext, String title, String message) {
		final String suppressDialog = (String) workflowScriptContext.getSession().get("wfSuppressDialog"); // set in integration tests
		if (!"true".equals(suppressDialog)) {
			try {
				OperationAgent operationAgent = workflowScriptContext.requireSpecialist(OperationAgent.TYPE);
				RequestOperation requestOperation = operationAgent.getOperation(RequestOperation.TYPE);
				if (requestOperation != null) {
					requestOperation.setTitle(title);
					requestOperation.perform(message);
				} // else do nothing (requestOperation == null for integration test case)
			} catch (IllegalStateException e) {
				Logging.logError("Show dialog failed.", e, LOGGER);
				// catch exception for integration test case
			}
		}
	}


	/**
	 * A convenience method to display a question pop-up in the client
	 *
	 * @param workflowScriptContext The context to use.
	 * @param title                 The pop-up title.
	 * @param question              The question to display.
	 * @return Boolean indicating whether the question was answered with yes or no
	 */
	protected boolean showQuestionDialog(WorkflowScriptContext workflowScriptContext, String title, String question) {
		final String suppressDialog = (String) workflowScriptContext.getSession().get("wfSuppressDialog"); // set in integration tests
		if (!"true".equals(suppressDialog)) {
			OperationAgent operationAgent = workflowScriptContext.requireSpecialist(OperationAgent.TYPE);
			RequestOperation requestOperation = operationAgent.getOperation(RequestOperation.TYPE);
			boolean answer = true;
			if (requestOperation != null) {
				requestOperation.setTitle(title);
				requestOperation.addYes();
				RequestOperation.Answer noAnswer = requestOperation.addNo();
				RequestOperation.Answer actualAnswer = requestOperation.perform(question);
				answer = !noAnswer.equals(actualAnswer);
			}
			return answer;
		} else {
			// Always return true for integration tests
			return true;
		}
	}


	/**
	 * Convenience method to get the value of a custom workflow attribute.
	 *
	 * @param workflowScriptContext The context to use.
	 * @param attribute             The attribute to get the value for.
	 * @return the value.
	 */
	protected Object getCustomAttribute(WorkflowScriptContext workflowScriptContext, String attribute) {
		return workflowScriptContext.getTask().getCustomAttributes().get(attribute);
	}

}
