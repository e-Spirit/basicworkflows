/*
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
 */
package com.espirit.moddev.basicworkflows.util;

import de.espirit.firstspirit.workflow.WebeditElementStatusProviderPlugin;

/**
 * Created by Zaplatynski on 14.10.2014.
 */
public class ReleaseStateChangedTestData {

    private int providedState;
    private WebeditElementStatusProviderPlugin.State expectedState;
    private boolean releaseSupported = false;
    private boolean hasTask = false;

    private ReleaseStateChangedTestData() {
    }

    public static ReleaseStateChangedTestData create(){
        return new ReleaseStateChangedTestData();
    }

    public static ReleaseStateChangedTestData createWith(int state){
        return create().provideState(state);
    }

    public ReleaseStateChangedTestData provideState(int state){
        providedState = state;
        return this;
    }

    public ReleaseStateChangedTestData expectState(WebeditElementStatusProviderPlugin.State state){
        expectedState = state;
        return this;
    }

    public ReleaseStateChangedTestData enableReleaseSupport(){
        releaseSupported = true;
        return this;
    }

    public ReleaseStateChangedTestData withTask(){
        hasTask = true;
        return this;
    }

    public int getProvidedState() {
        return providedState;
    }

    public WebeditElementStatusProviderPlugin.State getExpectedState() {
        return expectedState;
    }

    public boolean isReleaseSupported() {
        return releaseSupported;
    }

    public boolean hasTasks() {
        return hasTask;
    }
}
