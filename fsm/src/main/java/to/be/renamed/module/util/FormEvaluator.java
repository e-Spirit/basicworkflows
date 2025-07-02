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
package to.be.renamed.module.util;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.access.editor.value.Option;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;
import de.espirit.firstspirit.access.store.templatestore.gom.GomFormElement;
import de.espirit.firstspirit.forms.FormData;
import de.espirit.firstspirit.forms.FormField;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Convenience class to determine the value of a form element.
 *
 * @author stephan
 * @since 1.0
 */
public class FormEvaluator {

    public static final Class<?> LOGGER = FormEvaluator.class;
    /**
     * The workflowScriptContext from the workflow.
     */
    private final WorkflowScriptContext workflowScriptContext;

    /**
     * Constructor for FormEvaluator.
     *
     * @param workflowScriptContext The workflowScriptContext from the workflow.
     */
    public FormEvaluator(final WorkflowScriptContext workflowScriptContext) {
        this.workflowScriptContext = workflowScriptContext;
    }

    /**
     * Method to determine the value of a checkbox.
     *
     * @param varname The name of the variable to check.
     * @return true if the checkbox is checked.
     */
    public boolean getCheckboxValue(final String varname) {
        boolean checkboxValue = false;

        final Object attributeValue = workflowScriptContext.getTask().getCustomAttributes().get(varname);
        if (attributeValue != null) {
            // test case
            if (WorkflowConstants.TRUE.equals(attributeValue)) {
                checkboxValue = true;
            }
        } else {
            // standard case
            final FormData formData = workflowScriptContext.getFormData();
            if (formData == null) {
                return false;
            }
            final GomFormElement formElement = formData.getForm().findEditor(varname);
            if (formElement == null) {
                // no formfield with given name
                Logging.logWarning("Form field with name '" + varname
                                   + "' not existing (using default value 'false') - seems your workflow is outdated - please update!", LOGGER);
                return false;
            }
            final FormField<Set<Option>>
                formField =
                (FormField<Set<Option>>) formData.get(workflowScriptContext.getProject().getMasterLanguage(), varname);
            if (formField == null) {
                return false;
            }
            final Set<Option> options = formField.get();
            for (Option option : options) {
                checkboxValue = Boolean.parseBoolean((String) option.getValue());
            }
        }

        return checkboxValue;
    }

    public Language[] getLanguages() {
        List<Language> languages = new ArrayList<>();
        final FormData formData = workflowScriptContext.getFormData();
        if (formData == null) {
            return new Language[0];
        }
        final GomFormElement formElement = formData.getForm().findEditor(WorkflowConstants.LANGUAGES_FORM_REFNAME);
        if (formElement == null) {
            // no formfield with given name
            Logging.logWarning("Form field with name '" + WorkflowConstants.LANGUAGES_FORM_REFNAME
                               + "' not existing (using default value 'false') - seems your workflow is outdated - please update!", LOGGER);
            return new Language[0];
        }
        final FormField<Set<Option>>
            formField =
            (FormField<Set<Option>>) formData.get(workflowScriptContext.getProject().getMasterLanguage(), WorkflowConstants.LANGUAGES_FORM_REFNAME);
        if (formField == null) {
            return new Language[0];
        }
        final Set<Option> options = formField.get();

        for (Option option : options) {
            Language language = (Language) option.getValue();
            if (language != null) {
                languages.add(language);
            }
        }

        return languages.toArray(new Language[0]);
    }
}
