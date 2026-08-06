/*
 * BasicWorkflows Module
 * %%
 * Copyright (C) 2012 - 2025 Crownpeak Technology GmbH - https://www.crownpeak.com
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
package to.be.renamed.module.projectapp;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.io.FileHandle;
import de.espirit.firstspirit.io.FileSystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Reads and writes the BasicWorkflows project-app configuration, a single
 * {@code config.properties} file living in the project-app config file system.
 * Shared by {@link BasicWorkflowsProjectAppConfig} (writer) and the report plugin (reader)
 * so both agree on file name and key.
 */
public final class BasicWorkflowsProjectAppConfigStore {

    public static final Class<?> LOGGER = BasicWorkflowsProjectAppConfigStore.class;

    public static final String CONFIG_FILE_NAME = "config.properties";
    public static final String KEY_SHOW_REPORT = "showAiReviewReport";

    private BasicWorkflowsProjectAppConfigStore() {
    }

    /** Returns the persisted "show report" flag; {@code false} when absent or unreadable. */
    public static boolean readShowReport(final FileSystem<?> fs) {
        try {
            final FileHandle handle = fs.obtain(CONFIG_FILE_NAME);
            if (!handle.exists()) {
                return false;
            }
            final Properties props = new Properties();
            try (InputStream in = handle.load()) {
                props.load(in);
            }
            return Boolean.parseBoolean(props.getProperty(KEY_SHOW_REPORT, "false"));
        } catch (final IOException e) {
            Logging.logWarning("Could not read BasicWorkflows project-app config: " + e.getMessage(), LOGGER);
            return false;
        }
    }

    /** Persists the "show report" flag into {@code config.properties}. */
    public static void writeShowReport(final FileSystem<?> fs, final boolean value) throws IOException {
        final FileHandle handle = fs.obtain(CONFIG_FILE_NAME);
        final Properties props = new Properties();
        if (handle.exists()) {
            try (InputStream in = handle.load()) {
                props.load(in);
            }
        }
        props.setProperty(KEY_SHOW_REPORT, Boolean.toString(value));
        try (OutputStream out = handle.getOutputStream()) {
            props.store(out, "BasicWorkflows project-app configuration");
        }
    }
}
