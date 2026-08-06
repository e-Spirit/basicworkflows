/*
 * BasicWorkflows Module
 * %%
 * Copyright (C) 2012 - 2025 Crownpeak Technology GmbH - https://www.crownpeak.com
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

package to.be.renamed.module.util;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;
import de.espirit.firstspirit.workflow.model.Transition;

/**
 * Resolves workflow transitions using a three-layer lookup strategy:
 * <ol>
 *   <li>Check the workflow session for a stored transition name under the given session key</li>
 *   <li>Check available transitions for one matching the default transition name</li>
 *   <li>If exactly one transition is available, use it</li>
 * </ol>
 * Throws {@link IllegalStateException} if no transition can be resolved.
 *
 * @since 1.6.0
 */
public final class TransitionResolver {

    private static final Class<?> LOGGER = TransitionResolver.class;

    private TransitionResolver() {
    }

    /**
     * Resolves and executes a transition on the given context.
     *
     * @param context            the workflow script context
     * @param sessionKey         the session key to check for a stored transition name
     * @param defaultTransition  the default transition name to look for
     * @throws IllegalStateException if no transition can be resolved
     */
    public static void doTransition(final WorkflowScriptContext context,
                                     final String sessionKey,
                                     final String defaultTransition) throws IllegalAccessException {
        final String resolvedName = resolve(context, sessionKey, defaultTransition);
        context.doTransition(resolvedName);
    }

    /**
     * Resolves a transition name using the three-layer lookup strategy.
     */
    static String resolve(final WorkflowScriptContext context,
                           final String sessionKey,
                           final String defaultTransition) {
        final Transition[] transitions = context.getTransitions();

        // Layer 1: check session for stored transition name
        final Object sessionValue = context.getSession().get(sessionKey);
        if (sessionValue instanceof String && !((String) sessionValue).isEmpty()) {
            Logging.logDebug("Found configured transition in session key '" + sessionKey + "': " + sessionValue, LOGGER);
            for (Transition transition : transitions) {
                if (sessionValue.equals(transition.getUid())) {
                    Logging.logInfo("Transition resolved from session key '" + sessionKey + "': " + sessionValue, LOGGER);
                    return (String) sessionValue;
                }
            }
        }

        // Layer 2: check available transitions for default name
        for (Transition transition : transitions) {
            if (defaultTransition.equals(transition.getUid())) {
                Logging.logInfo("Transition resolved by default name: " + defaultTransition, LOGGER);
                return defaultTransition;
            }
        }

        // Layer 3: if exactly one transition available, use it
        if (transitions.length == 1) {
            final String onlyTransition = transitions[0].getUid();
            Logging.logInfo("Only one transition available, using: " + onlyTransition, LOGGER);
            return onlyTransition;
        }

        // No transition found
        final StringBuilder available = new StringBuilder();
        for (Transition t : transitions) {
            if (!available.isEmpty()) {
                available.append(", ");
            }
            available.append(t.getUid());
        }
        throw new IllegalStateException(
                "Cannot resolve transition. Expected '" + defaultTransition
                        + "' but available transitions are: [" + available + "]. "
                        + "Set session key '" + sessionKey + "' or add a transition named '"
                        + defaultTransition + "' to the workflow model.");
    }
}
