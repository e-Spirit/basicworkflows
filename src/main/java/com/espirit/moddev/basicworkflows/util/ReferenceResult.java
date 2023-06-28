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

import de.espirit.common.base.Logging;

/**
 * Class to store information if all release conditions are met.
 *
 * @author stephan
 * @since 1.0
 */
public class ReferenceResult {

    /**
     * This variable determines if only media is referenced.
     */
    private boolean onlyMedia;
    /**
     * This variable determines if all referenced non media elements are released.
     */
    private boolean notMediaReleased;
    /**
     * This variable determines if all referenced elements are released.
     */
    private boolean allObjectsReleased;
    /**
     * This variable determines if there are broken references.
     */
    private boolean noBrokenReferences;
    /**
     * This variable determines if there are no objects in a workflow.
     */
    private boolean noObjectsInWorkflow;

    /**
     * Constructor for ReferenceResult.
     */
    public ReferenceResult() {
        onlyMedia = true;
        notMediaReleased = true;
        allObjectsReleased = true;
        noBrokenReferences = true;
        noObjectsInWorkflow = true;
    }

    /**
     * Is broken references.
     *
     * @return the boolean
     */
    public boolean isNoBrokenReferences() {
        return noBrokenReferences;
    }

    /**
     * Sets broken references.
     *
     * @param noBrokenReferences the broken references
     */
    public void setNoBrokenReferences(boolean noBrokenReferences) {
        this.noBrokenReferences = noBrokenReferences;
    }

    /**
     * Method to check if only media is referenced.
     *
     * @return true if only media is referenced.
     */
    private boolean isOnlyMedia() {
        return onlyMedia;
    }

    /**
     * Setter for the field onlyMedia.
     *
     * @param onlyMedia Set to false if not only media is referenced.
     */
    public void setOnlyMedia(boolean onlyMedia) {
        this.onlyMedia = onlyMedia;
    }

    /**
     * Method to check if all non media elements are released.
     *
     * @return true if not media and released.
     */
    private boolean isNotMediaReleased() {
        return notMediaReleased;
    }

    /**
     * Setter for the field notMediaReleased.
     *
     * @param notMediaReleased Set to false if not a medium and not/never released.
     */
    public void setNotMediaReleased(boolean notMediaReleased) {
        this.notMediaReleased = notMediaReleased;
    }

    /**
     * Method to check if all objects are released.
     *
     * @return true if all objects are released.
     */
    private boolean isAllObjectsReleased() {
        return allObjectsReleased;
    }

    /**
     * Setter for the field allObjectsReleased.
     *
     * @param allObjectsReleased Set to false if not all objects are released.
     */
    public void setAllObjectsReleased(boolean allObjectsReleased) {
        this.allObjectsReleased = allObjectsReleased;
    }

    /**
     * Checks if there are any release issues.
     *
     * @param releaseWithMedia the release with media
     * @return true if it has release issues
     */
    public boolean hasReleaseIssues(boolean releaseWithMedia) {
        boolean allReleased = false;

        // check result if can be released
        if (isOnlyMedia() && releaseWithMedia) {
            Logging.logWarning("Is only media and checked", getClass());
            allReleased = true;
        } else if (isNotMediaReleased() && releaseWithMedia) {
            Logging.logWarning("All non media released and checked", getClass());
            allReleased = true;
        } else if (isAllObjectsReleased() && !releaseWithMedia) {
            Logging.logWarning("Everything released and not checked", getClass());
            allReleased = true;
        }

        return !(allReleased && isNoBrokenReferences() && hasNoObjectsInWorkflow());
    }

    /**
     * Setter for the field allObjectsReleased.
     *
     * @param noObjectsInWorkflow Set to false some objects are in a workflow.
     */
    public void setNoObjectsInWorkflow(boolean noObjectsInWorkflow) {
        this.noObjectsInWorkflow = noObjectsInWorkflow;
    }

    /**
     * Check if there are no objects in a workflow.
     *
     * @return the boolean
     */
    private boolean hasNoObjectsInWorkflow() {
        return noObjectsInWorkflow;
    }
}
