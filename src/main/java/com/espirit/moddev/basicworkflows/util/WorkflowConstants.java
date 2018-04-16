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
 * Utility class to set constants.
 *
 * @author stephan
 * @since 1.0
 */
public interface WorkflowConstants {

    /**
     * The package name of the resource bundle.
     */
    String MESSAGES = "com.espirit.moddev.basicworkflows.Messages";

    /**
     * String representation of true.
     */
    String TRUE = "true";

    /**
     * String representation of false.
     */
    String FALSE = "false";

    /**
     * Key for context in executable's parameters.
     */
    String CONTEXT = "context";

    /**
     * Resource bundle key for error message title.
     */
    String ERROR_MSG = "errorMsg";

    /**
     * Resource bundle key for delete failed message.
     */
    String DELETE_FAILED = "deleteFailed";

    /**
     * Resource bundle key for release failed message.
     */
    String RELEASE_FAILED = "releaseFailed";

    /**
     * Key to suppress dialog for testing.
     */
    String WF_SUPPRESS_DIALOG = "wfSuppressDialog";

    /**
     * Resource bundle key for warning message.
     */
    String WARNING = "warning";

    /**
     * Key that identifies the releasePageRefElements.
     */
    String RELEASE_PAGEREF_ELEMENTS = "releasePageRefElements";

    /**
     * Key that identifies the relatedPageRefElements.
     */
    String RELATED_PAGEREF_ELEMENTS = "relatedPageRefElements";

    String WF_NOT_RELEASED_ELEMENTS = "wfNotReleasedElements";

    String WF_BROKEN_REFERENCES = "wfBrokenReferences";

    String IMAGE_HAS_REFERENCES = "wfImageHasReferences";

    String IMAGE_FOLDER_HAS_REFERENCES = "wfImageFolderHasReferences";

    String WF_RECURSIVE_CHILDREN = "wfRecursiveChildren";

    String MEDIA_FORM_REFNAME = "wf_releasewmedia";

    String RECURSIVE_FORM_REFNAME = "wf_releaseRecursively";

    String WF_OBJECTS_IN_WORKFLOW = "wfObjectsInWorkflow";
}
