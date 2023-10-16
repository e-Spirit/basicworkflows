/*
 * BasicWorkflows Module
 * %%
 * Copyright (C) 2012 - 2023 Crownpeak Technology GmbH - https://www.crownpeak.com
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
package com.espirit.moddev.basicworkflows.util;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.BaseContext;
import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;
import de.espirit.firstspirit.agency.LanguageAgent;
import de.espirit.firstspirit.agency.UIAgent;
import de.espirit.firstspirit.webedit.WebeditUiAgent;

import java.util.Locale;

/**
 * Convenience class to get/create locale from java/webclient context.
 *
 * @author stephan
 * @since 1.0
 */

public class FsLocale {

    /**
     * The current locale.
     */
    private Locale locale;

    /**
     * The current workflowScriptContext.
     */
    private WorkflowScriptContext workflowScriptContext;

    /**
     * The current context (for WorkflowStatusProvider).
     */
    private BaseContext baseContext;

    /**
     * Logger class.
     */
    public static final Class<?> LOGGER = FsLocale.class;

    /**
     * Constructor for FsLocale.
     *
     * @param baseContext The baseContext of webedit (for WorkflowStatusProvider).
     */
    public FsLocale(BaseContext baseContext) {
        WebeditUiAgent uiAgent = baseContext.requireSpecialist(WebeditUiAgent.TYPE);
        set(uiAgent.getDisplayLanguage().getLocale());
        this.baseContext = baseContext;
    }


    /**
     * Constructor for FsLocale.
     *
     * @param workflowScriptContext The workflowScriptContext from the workflow.
     */
    public FsLocale(WorkflowScriptContext workflowScriptContext) {
        this.workflowScriptContext = workflowScriptContext;
        if (workflowScriptContext.is(BaseContext.Env.WEBEDIT)) {
            WebeditUiAgent uiAgent = workflowScriptContext.requireSpecialist(WebeditUiAgent.TYPE);
            set(uiAgent.getDisplayLanguage().getLocale());
        } else {
            try {
                UIAgent uiAgent = workflowScriptContext.requireSpecialist(UIAgent.TYPE);
                set(uiAgent.getDisplayLanguage().getLocale());
            } catch (IllegalStateException e) {
                // catch exception for integration test case
                set(Locale.getDefault());
                Logging.logWarning("Falling back to default locale: " + Locale.getDefault(), e, LOGGER);
            }
        }
    }

    /**
     * Setter for the field locale.
     *
     * @param locale The locale to set.
     */
    private void set(Locale locale) {
        this.locale = locale;
    }

    /**
     * Method to get the current locale.
     *
     * @return the current locale.
     */
    public Locale get() {
        return locale;
    }

    /**
     * Method to get the current language.
     *
     * @return the current language.
     */
    public Language getLanguage() {
        if (workflowScriptContext == null) {
            LanguageAgent languageAgent = baseContext.requestSpecialist(LanguageAgent.TYPE);
            Language language = languageAgent.getMasterLanguage();
            for (Language lang : languageAgent.getLanguages()) {
                if (lang.getLocale().equals(locale)) {
                    language = lang;
                    break;
                }
            }
            return language;
        } else {
            return workflowScriptContext.getProject().getLanguage(locale.getLanguage().toUpperCase());
        }
    }


}
