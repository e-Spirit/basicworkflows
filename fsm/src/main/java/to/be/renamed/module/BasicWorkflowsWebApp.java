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
package to.be.renamed.module;

import com.espirit.moddev.components.annotations.WebAppComponent;

import de.espirit.firstspirit.module.AbstractWebApp;
import de.espirit.firstspirit.module.WebApp;

@WebAppComponent(
    name = "BasicWorkflows_ContentCreator_Library",
    description = "Web component for BasicWorkflows functionality.",
    xmlSchemaVersion = "6.0",
    webXml = "web/web.xml"
)
public class BasicWorkflowsWebApp extends AbstractWebApp implements WebApp {

    @Override
    public void installed() {
        // Not used
    }

    @Override
    public void updated(String oldVersionString) {
        // Not used
    }
}
