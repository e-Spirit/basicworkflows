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
import de.espirit.firstspirit.agency.OperationAgent;
import de.espirit.firstspirit.agency.SpecialistsBroker;
import de.espirit.firstspirit.ui.operations.RequestOperation;

/**
 * The type Dialog.
 */
public class Dialog {

    private SpecialistsBroker context;

    /**
     * Instantiates a new Dialog.
     *
     * @param broker the broker
     */
    public Dialog(SpecialistsBroker broker) {
        if (broker == null) {
            throw new IllegalArgumentException("SpecialistsBroker is null");
        }
        this.context = broker;
    }


    /**
     * Show dialog.
     *
     * @param kind    the kind of request operation
     * @param type    the type of question if the kind is question
     * @param title   the title of the request
     * @param message the message of the request
     * @return true for positive answer such as yes or ok, false otherwise
     */
    public boolean showDialog(RequestOperation.Kind kind, QuestionType type, String title, String message) {
        if (kind == null) {
            throw new IllegalArgumentException("RequestOperation.Kind is null");
        }
        if (type == null) {
            throw new IllegalArgumentException("QuestionType is null");
        }
        if (title == null) {
            throw new IllegalArgumentException("title is null");
        }
        if (message == null) {
            throw new IllegalArgumentException("message is null");
        }
        boolean result;
        try {
            RequestOperation requestOperation = loadRequestOperation();
            if (requestOperation != null) {
                requestOperation.setTitle(title);
                requestOperation.setKind(kind);
                RequestOperation.Answer positiveAnswer = getPositiveAnswerFrom(type, requestOperation);
                final RequestOperation.Answer answer = requestOperation.perform(message);
                result = answer != null && answer.equals(positiveAnswer);
            } else {
                result = false;
            }
        } catch (Exception e) { //NOSONAR
            result = false;
            Logging.logError("Show dialog (" + kind.name() + "/" + type.name() + ") failed: " + e.getMessage(), e, getClass());

        }
        return result;
    }

    private static RequestOperation.Answer getPositiveAnswerFrom(final QuestionType type, final RequestOperation requestOperation) {
        RequestOperation.Answer positiveAnswer = null;
        switch (type) {
            case YES_NO:
                positiveAnswer = requestOperation.addYes();
                requestOperation.addNo();
                break;
            case OK_CANCEL:
                positiveAnswer = requestOperation.addOk();
                requestOperation.addCancel();
                break;
            default:
                break;
        }
        return positiveAnswer;
    }

    private RequestOperation loadRequestOperation() {
        OperationAgent operationAgent = context.requireSpecialist(OperationAgent.TYPE);
        return operationAgent.getOperation(RequestOperation.TYPE);
    }

    /**
     * Show info dialog.
     *
     * @param title   the title
     * @param message the info message
     */

    public void showInfo(String title, String message) {
        showDialog(RequestOperation.Kind.INFO, QuestionType.NONE, title, message);
    }

    /**
     * Show error dialog.
     *
     * @param title   the title
     * @param message the error message
     */
    public void showError(String title, String message) {
        showDialog(RequestOperation.Kind.ERROR, QuestionType.NONE, title, message);
    }

    /**
     * Show question dialog.
     *
     * @param type     the type of question
     * @param title    the title of the request
     * @param question the question message of the request
     * @return true for positive answer such as yes or ok, false otherwise
     */
    public boolean showQuestion(QuestionType type, String title, String question) {
        return showDialog(RequestOperation.Kind.QUESTION, type, title, question);
    }

    /**
     * The enum Question type.
     */
    public enum QuestionType {

        /**
         * The NONE Type.
         */
        NONE,

        /**
         * The YES_NO Type.
         */
        YES_NO,

        /**
         * The OK_CANCEL Type.
         */
        OK_CANCEL
    }

}
