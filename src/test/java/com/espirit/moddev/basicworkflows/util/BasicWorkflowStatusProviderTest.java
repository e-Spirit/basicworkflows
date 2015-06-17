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
package com.espirit.moddev.basicworkflows.util;

import de.espirit.common.util.CollectionListable;
import de.espirit.common.util.Listable;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.StoreElement;
import de.espirit.firstspirit.access.store.contentstore.Dataset;
import de.espirit.firstspirit.access.store.pagestore.Page;
import de.espirit.firstspirit.access.store.sitestore.DocumentGroup;
import de.espirit.firstspirit.access.store.sitestore.PageRef;
import de.espirit.firstspirit.workflow.WebeditElementStatusProviderPlugin;
import de.espirit.firstspirit.workflow.WorkflowGroup;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.lang.Boolean;import java.lang.Exception;import java.lang.NullPointerException;import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(Theories.class)
public class BasicWorkflowStatusProviderTest {

    @Rule
    public BaseContextRule contextRule = new BaseContextRule(Locale.GERMANY);

    private BasicWorkflowStatusProvider testling;

    @Before
    public void setUpTest() throws Exception {
        testling = new BasicWorkflowStatusProvider();
        testling.setUp(contextRule.getContext());
    }

    @DataPoints
    public static ReleaseStateChangedTestData[] testCases = {
        /* -> CHANGED */
        ReleaseStateChangedTestData.createWith(IDProvider.CHANGED).enableReleaseSupport()
            .expectState(WebeditElementStatusProviderPlugin.State.CHANGED),
        ReleaseStateChangedTestData.createWith(IDProvider.NEVER_RELEASED).enableReleaseSupport()
            .expectState(WebeditElementStatusProviderPlugin.State.CHANGED),

        /* -> RELEASED */
        ReleaseStateChangedTestData.createWith(IDProvider.RELEASED).enableReleaseSupport()
            .expectState(WebeditElementStatusProviderPlugin.State.RELEASED),
        /* missing Release support */
        ReleaseStateChangedTestData.createWith(IDProvider.CHANGED).expectState(WebeditElementStatusProviderPlugin.State.RELEASED),
        ReleaseStateChangedTestData.createWith(IDProvider.NEVER_RELEASED).expectState(WebeditElementStatusProviderPlugin.State.RELEASED),

        /* -> IN WORKFLOW */
        ReleaseStateChangedTestData.createWith(IDProvider.CHANGED).enableReleaseSupport().withTask().expectState(
            WebeditElementStatusProviderPlugin.State.IN_WORKFLOW),
        ReleaseStateChangedTestData.createWith(IDProvider.NEVER_RELEASED).enableReleaseSupport().withTask()
            .expectState(WebeditElementStatusProviderPlugin.State.IN_WORKFLOW),
        ReleaseStateChangedTestData.createWith(IDProvider.RELEASED).enableReleaseSupport().withTask()
            .expectState(WebeditElementStatusProviderPlugin.State.IN_WORKFLOW),
        /* missing Release support */
        ReleaseStateChangedTestData.createWith(IDProvider.CHANGED).withTask().expectState(WebeditElementStatusProviderPlugin.State.IN_WORKFLOW),
        ReleaseStateChangedTestData.createWith(IDProvider.NEVER_RELEASED).withTask()
            .expectState(WebeditElementStatusProviderPlugin.State.IN_WORKFLOW),
        ReleaseStateChangedTestData.createWith(IDProvider.RELEASED).withTask().expectState(WebeditElementStatusProviderPlugin.State.IN_WORKFLOW)
    };

    @Theory
    public void testGetReleaseStateSimple(ReleaseStateChangedTestData testData) throws Exception {

        IDProvider element = mock(IDProvider.class);

        when(element.isReleaseSupported()).thenReturn(testData.isReleaseSupported());
        when(element.getReleaseStatus()).thenReturn(testData.getProvidedState());
        when(element.hasTask()).thenReturn(testData.hasTasks());

        WebeditElementStatusProviderPlugin.State releaseState = testling.getReleaseState(element);

        assertThat("unexpected State", releaseState, is(testData.getExpectedState()));
    }

    @Theory
    public void testGetReleaseStatePage(ReleaseStateChangedTestData testData) throws Exception {

        PageRef element = mock(PageRef.class);
        Page page = mock(Page.class);

        when(element.isReleaseSupported()).thenReturn(Boolean.TRUE);
        when(element.getReleaseStatus()).thenReturn(IDProvider.RELEASED);
        when(element.hasTask()).thenReturn(Boolean.FALSE);
        when(element.getPage()).thenReturn(page);
        when(page.isReleaseSupported()).thenReturn(testData.isReleaseSupported());
        when(page.getReleaseStatus()).thenReturn(testData.getProvidedState());
        when(page.hasTask()).thenReturn(testData.hasTasks());
        when(element.getParent()).thenReturn(element);

        WebeditElementStatusProviderPlugin.State releaseState = testling.getReleaseState(element);

        assertThat("unexpected State", releaseState, is(testData.getExpectedState()));
    }

    @Theory
    public void testGetReleaseStateDocumentGroup(ReleaseStateChangedTestData testData) throws Exception {

        DocumentGroup group = mock(DocumentGroup.class);
        PageRef element = mock(PageRef.class);
        Page page = mock(Page.class);

        Listable<StoreElement> listable = new CollectionListable<StoreElement>(Arrays.asList((StoreElement) element));

        when(group.getChildCount()).thenReturn(1);
        when(group.getChildren()).thenReturn(listable);

        when(element.isReleaseSupported()).thenReturn(Boolean.TRUE);
        when(element.getReleaseStatus()).thenReturn(IDProvider.RELEASED);
        when(element.hasTask()).thenReturn(Boolean.FALSE);
        when(element.getPage()).thenReturn(page);
        when(page.isReleaseSupported()).thenReturn(testData.isReleaseSupported());
        when(page.getReleaseStatus()).thenReturn(testData.getProvidedState());
        when(page.hasTask()).thenReturn(testData.hasTasks());
        when(element.getParent()).thenReturn(element);

        WebeditElementStatusProviderPlugin.State releaseState = testling.getReleaseState(group);

        assertThat("unexpected State", releaseState, is(testData.getExpectedState()));
    }

    @DataPoints
    public static IDProvider[] elements = {mock(Page.class), mock(PageRef.class), mock(Dataset.class), mock(DocumentGroup.class)};

    @Theory(nullsAccepted = false)
    public void testGetWorkflowGroupsNotNull(IDProvider element) {

        List<WorkflowGroup> workflowGroupList = testling.getWorkflowGroups(element);

        assertThat(workflowGroupList, hasSize(1));

        WorkflowGroup workflowGroup = workflowGroupList.get(0);

        assertThat(workflowGroup.getElements(), hasSize(1));
        assertThat(workflowGroup.getElements(), contains(sameInstance(element)));

        final ResourceBundle bundle = ResourceBundle.getBundle(WorkflowConstants.MESSAGES, Locale.GERMANY);

        if (element instanceof PageRef || element instanceof Page) {
            assertThat(workflowGroup.getDisplayName(), is(bundle.getString("pageReference")));
        }

        if (element instanceof DocumentGroup) {
            assertThat(workflowGroup.getDisplayName(), is(bundle.getString("documentGroup")));
        }

        if (element instanceof Dataset) {
            assertThat(workflowGroup.getDisplayName(), is(bundle.getString("dataset")));
        }
    }

    @Test
    public void testGetWorkflowGroupsEmptyList() {
        List<WorkflowGroup> workflowGroupList = testling.getWorkflowGroups(mock(IDProvider.class));
        assertThat(workflowGroupList, hasSize(0));
    }

    @Test
    public void testGetWorkflowGroupsEmptyListNullElement() {
        List<WorkflowGroup> workflowGroupList = testling.getWorkflowGroups(null);
        assertThat(workflowGroupList, hasSize(0));
    }

    @Test(expected = NullPointerException.class)
    public void testGetReleaseStateNull() {
        testling.getReleaseState(null);
    }
}
