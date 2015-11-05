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

import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.contentstore.Content2;
import de.espirit.firstspirit.access.store.pagestore.Section;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;
import de.espirit.firstspirit.agency.BrokerAgent;
import de.espirit.firstspirit.agency.FormValidationReport;
import de.espirit.firstspirit.agency.LanguageAgent;
import de.espirit.firstspirit.agency.MultiFormValidationReport;
import de.espirit.firstspirit.agency.SpecialistsBroker;
import de.espirit.firstspirit.agency.ValidationAgent;
import de.espirit.firstspirit.store.access.globalstore.ProjectPropertiesImpl;
import de.espirit.or.schema.Entity;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

/**
 * Convenience class to determine if a form is valid.
 *
 * @author stephan
 * @since 1.0
 */
public class FormValidator {

    /**
     * The workflowScriptContext from the workflow.
     */
    private WorkflowScriptContext workflowScriptContext;

    /**
     * Constructor for FormValidator.
     *
     * @param workflowScriptContext The workflowScriptContext from the workflow.
     */
    public FormValidator(WorkflowScriptContext workflowScriptContext) {
        this.workflowScriptContext = workflowScriptContext;
    }

    /**
     * Convenience method to check if validation of gui form (of an IDProvider) is successful.
     *
     * @param idProvider The IDProvider to check.
     * @return The error String of null.
     */
    public String isValid(IDProvider idProvider) {
        return isValid(idProvider, null, null);
    }


    /**
     * Convenience method to check if validation of gui form (of an Entity) is successful.
     *
     * @param content2 The Content2 object of the entity.
     * @param entity   The Entity to check.
     * @return The error String of null.
     */
    public String isValid(Content2 content2, Entity entity) {
        return isValid(null, content2, entity);
    }

    /**
     * Main method to check if validation of gui form is successful.
     *
     * @param idProvider The IDProvider to check.
     * @param content2   The Content2 object of the entity.
     * @param entity     The Entity to check.
     * @return The error String or null.
     */
    private String isValid(@Nullable IDProvider idProvider, @Nullable Content2 content2, @Nullable Entity entity) {
        MultiFormValidationReport validationReportsRel;
        SpecialistsBroker broker = workflowScriptContext.getUserService().getConnection().getBroker();
        BrokerAgent brokerAgent = broker.requireSpecialist(BrokerAgent.TYPE);
        SpecialistsBroker projectBroker = brokerAgent.getBrokerByProjectName(workflowScriptContext.getProject().getName());

        LanguageAgent languageAgent = projectBroker.requireSpecialist(LanguageAgent.TYPE);
        ValidationAgent validationAgent = projectBroker.requireSpecialist(ValidationAgent.TYPE);
        String element = "";
        String validationResult = null;

        if (entity != null && content2 != null) {
            validationReportsRel =
                validationAgent
                    .validate(content2.getDataset(entity).getFormData(), languageAgent.getLanguages(), ValidationAgent.ValidationScope.RELEASE);
            // element uid for error msg
            element = "\nContent2: " + content2.getUid() + "\n";
        } else {
            validationReportsRel = validationAgent.validate(idProvider, ValidationAgent.ValidationScope.RELEASE);
            if (idProvider != null) {
                if (idProvider instanceof Section) {
                    // section has no uid so show page instead
                    element = "\n" + idProvider.getParent().getParent().getElementType() + ": " + idProvider.getParent().getParent().getUid() + "\n";
                } else if (idProvider instanceof ProjectPropertiesImpl) {
                    // element uid for error msg
                    element = "\n" + idProvider.getElementType() + "\n";
                } else {
                    // element uid for error msg
                    element = "\n" + idProvider.getElementType() + ": " + idProvider.getUid() + "\n";
                }
            }
        }

        if (!validationReportsRel.isValid()) {
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append(element);
            FormValidationReport metaProblems = validationReportsRel.getProblemsForMetaData();
            if (metaProblems != null) {
                Set<String> gadgets = metaProblems.getGadgets();
                // meta input components
                for (String gadget : gadgets) {
                    Collection<String> messages = metaProblems.getMessages(gadget, workflowScriptContext.getProject().getMasterLanguage());
                    for (String message : messages) {
                        errorMsg.append("  ").append(gadget).append(" (META) = ").append(message).append("\n");
                    }
                }
            }
            // input components
            Collection<Language> langs = validationReportsRel.getLanguages(workflowScriptContext);
            for (Language lang : langs) {
                FormValidationReport problems = validationReportsRel.getProblems(lang);
                if (problems != null) {
                    Set<String> gadgets = problems.getGadgets();
                    for (String gadget : gadgets) {
                        Collection<String> messages = problems.getMessages(gadget, lang);
                        for (String message : messages) {
                            errorMsg.append("  ").append(gadget).append(" (").append(lang).append(") ").append(" = ").append(message).append("\n");
                        }
                    }
                }
            }
            validationResult = errorMsg.toString();
        }
        return validationResult;
    }

}
