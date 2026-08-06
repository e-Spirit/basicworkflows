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
package to.be.renamed.module.deploy;

/** Outcome of a single feature import attempt into one project. */
public enum ImportResult {
    /** Feature was imported. */
    INSTALLED,
    /** Skipped because at least one transported element already exists (no overwrite). */
    SKIPPED_EXISTS,
    /** Skipped because the analyze step reported errors (feature not installable). */
    SKIPPED_NOT_INSTALLABLE,
    /** Skipped because a prerequisite feature was not installed or failed. */
    SKIPPED_PREREQ,
    /** Import was attempted but failed with an exception. */
    FAILED
}
