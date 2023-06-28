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

import de.espirit.firstspirit.access.BaseContext;
import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.webedit.WebeditUiAgent;

import org.junit.rules.ExternalResource;

import java.lang.Override;import java.lang.Throwable;import java.util.Locale;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by Zaplatynski on 15.10.2014.
 */
public class BaseContextRule extends ExternalResource {

    private final Locale locale;
    private BaseContext context;

    public BaseContextRule(){
        locale = Locale.getDefault();
    }

    public BaseContextRule(final Locale locale) {
        this.locale = locale;
    }

    @Override
    protected void before() throws Throwable {
        Language language = mock(Language.class);
        when(language.getLocale()).thenReturn(locale);

        WebeditUiAgent agent = mock(WebeditUiAgent.class);
        when(agent.getDisplayLanguage()).thenReturn(language);

        context = mock(BaseContext.class);
        when(context.requireSpecialist(WebeditUiAgent.TYPE)).thenReturn(agent);
    }

    public BaseContext getContext() {
        return context;
    }
}
