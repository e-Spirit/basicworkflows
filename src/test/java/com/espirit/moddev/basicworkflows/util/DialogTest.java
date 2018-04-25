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

import de.espirit.firstspirit.agency.OperationAgent;
import de.espirit.firstspirit.ui.operations.RequestOperation;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DialogTest {

    @Rule
    public BaseContextRule contextRule = new BaseContextRule();

    private Dialog testling;
    private RequestOperation requestOperation;
    private boolean verify = true;

    @Before
    public void setUp() throws Exception {
        testling = new Dialog(contextRule.getContext());

        OperationAgent operationAgent = mock(OperationAgent.class);
        when(contextRule.getContext().requireSpecialist(OperationAgent.TYPE)).thenReturn(operationAgent);

        requestOperation = mock(RequestOperation.class);
        when(operationAgent.getOperation(RequestOperation.TYPE)).thenReturn(requestOperation);

        final RequestOperation.Answer answer = mock(RequestOperation.Answer.class);

        when(requestOperation.perform("message")).thenReturn(answer);
    }

    @After
    public void tearDown() throws Exception {
        if (verify) {
            verify(requestOperation).setTitle("title");
            verify(requestOperation).perform("message");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor() throws Exception {
        verify = false;

        new Dialog(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testShowDialogNullKind() throws Exception {
        verify = false;

        testling.showDialog(null, Dialog.QuestionType.NONE, "title", "message");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testShowDialogNullType() throws Exception {
        verify = false;
        testling.showDialog(RequestOperation.Kind.INFO, null, "title", "message");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testShowDialogNullTitle() throws Exception {
        verify = false;

        testling.showDialog(RequestOperation.Kind.INFO, Dialog.QuestionType.NONE, null, "message");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testShowDialogNullMessage() throws Exception {
        verify = false;

        testling.showDialog(RequestOperation.Kind.INFO, Dialog.QuestionType.NONE, "title", null);
    }


    @Test
    public void testShowDialog() throws Exception {
        final boolean result = testling.showDialog(RequestOperation.Kind.INFO, Dialog.QuestionType.NONE, "title", "message");

        assertFalse("Info dialog returns always false", result);
    }

    @Test
    public void testShowInfo() throws Exception {
        testling.showInfo("title", "message");

        verify(requestOperation).setKind(RequestOperation.Kind.INFO);
    }

    @Test
    public void testShowError() throws Exception {
        testling.showError("title", "message");

        verify(requestOperation).setKind(RequestOperation.Kind.ERROR);
    }

    @Test
    public void testShowQuestionYesNo() throws Exception {
        final boolean result = testling.showQuestion(Dialog.QuestionType.YES_NO, "title", "message");

        assertFalse("Question dialog should return false because we have no UI", result);

        verify(requestOperation).setKind(RequestOperation.Kind.QUESTION);
    }

    @Test
    public void testShowQuestionOkCancel() throws Exception {
        final boolean result = testling.showQuestion(Dialog.QuestionType.OK_CANCEL, "title", "message");

        assertFalse("Question dialog should return false because we have no UI", result);

        verify(requestOperation).setKind(RequestOperation.Kind.QUESTION);
    }
}
