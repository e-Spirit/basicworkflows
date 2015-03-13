/*
 * **********************************************************************
 * basicworkflows
 * %%
 * Copyright (C) 2012 - 2013 e-Spirit AG
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

/**
 * Class to store information if all release conditions are met.
 *
 * @author stephan
 * @since 1.0
 */
public class ReferenceResult {
    /** This variable determines if only media is referenced. */
    private boolean onlyMedia;
    /** This variable determines if all referenced non media elements are released. */
    private boolean notMediaReleased;
    /** This variable determines if all referenced elements are released. */
    private boolean allObjectsReleased;

    /**
     * Constructor for ReferenceResult.
     */
    public ReferenceResult() {
        this.onlyMedia = true;
        this.notMediaReleased = true;
        this.allObjectsReleased = true;
    }

    /**
     * Method to check if only media is referenced.
     *
     * @return true if only media is referenced.
     */
    public boolean isOnlyMedia() {
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
    public boolean isNotMediaReleased() {
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
    public boolean isAllObjectsReleased() {
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
}