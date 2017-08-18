/*
 * **********************************************************************
 * basicworkflows
 * %%
 * Copyright (C) 2012 - 2015 e-Spirit AG
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

import de.espirit.firstspirit.access.BaseContext;
import de.espirit.firstspirit.access.ServerActionHandle;
import de.espirit.firstspirit.access.Task;
import de.espirit.firstspirit.access.store.DeleteProgress;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.ReleaseProgress;
import de.espirit.firstspirit.access.store.Store;
import de.espirit.firstspirit.access.store.mediastore.Media;
import de.espirit.firstspirit.access.store.templatestore.WorkflowScriptContext;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author gremplewski
 */
public class DeleteObjectTest {

    private WorkflowScriptContext workflowScriptContext;
    private IDProvider elementToBeDeleted;
    private Store store;

    @Before
    public void initMocks() {
              store = mock(Store.class);
    }

    private void setUpNonMediaStore() {
        Task task = mock(Task.class);
        elementToBeDeleted = mock(IDProvider.class);
        when(elementToBeDeleted.getStore()).thenReturn(store);

        workflowScriptContext = mock(WorkflowScriptContext.class);
        when(workflowScriptContext.getWorkflowable()).thenReturn(null);
        when(workflowScriptContext.getElement()).thenReturn(elementToBeDeleted);
        when(workflowScriptContext.getTask()).thenReturn(task);
    }

    /**
     * Tests that the parent of a deleted template store element isn't booked for release
     */
    @Test
    public void testDeleteTemplateStoreElement() {
        setUpNonMediaStore();
        when(store.getType()).thenReturn(Store.Type.TEMPLATESTORE);
        runDelete();
        verify(elementToBeDeleted, never()).getParent();
    }

    /**
     * Tests that the parent of a deleted non template store element is booked for release
     */
    @Test
    public void testDeleteNonTemplateStoreElement() {
        setUpNonMediaStore();
        when(store.getType()).thenReturn(Store.Type.PAGESTORE);
        runDelete();
        verify(elementToBeDeleted).getParent();
    }

    @Test
    public void testDeleteMediaStoreElement() {
        when(store.getType()).thenReturn(Store.Type.MEDIASTORE);
        Task task = mock(Task.class);
        elementToBeDeleted = mock(Media.class);
        when(elementToBeDeleted.getStore()).thenReturn(store);

        workflowScriptContext = mock(WorkflowScriptContext.class);
        when(workflowScriptContext.getWorkflowable()).thenReturn(null);
        when(workflowScriptContext.getElement()).thenReturn(elementToBeDeleted);
        when(workflowScriptContext.getTask()).thenReturn(task);
        when(workflowScriptContext.is(BaseContext.Env.WEBEDIT)).thenReturn(Boolean.TRUE);

        runDelete();

        verify(workflowScriptContext, atLeastOnce()).is(BaseContext.Env.WEBEDIT);

        //deletion of empty parent folder
        verify(elementToBeDeleted, atLeastOnce()).getParent();
    }

    private void runDelete() {
        DeleteObject deleteObject = new DeleteObject(workflowScriptContext){
            @Override
            protected ServerActionHandle<? extends DeleteProgress, Boolean> deleteIgnoringReferences(List<IDProvider> deleteObjects) {
                return null;
            }

            @Override
            protected ServerActionHandle<? extends ReleaseProgress, Boolean> releaseWithAccessibilityAndNewOnly(IDProvider idProv) {
                return null;
            }
        };
        final boolean deleted = deleteObject.delete(false);

        assertTrue("Expect true", deleted);
    }


}
