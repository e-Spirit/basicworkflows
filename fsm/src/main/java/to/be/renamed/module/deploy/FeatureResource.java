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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The ContentTransport feature exports bundled with the module. Each constant maps
 * to a feature transport zip located on the module classpath.
 *
 * <p>A resource with a non-null {@code prerequisite} is only imported when the prerequisite
 * feature was already present (INSTALLED or SKIPPED_EXISTS) in the target project.
 */
public enum FeatureResource {

    DELETE(null, "basicworkflows-export-basic-delete.zip"),
    RELEASE(null, "basicworkflows-export-basic-release.zip"),
    RELEASE_WITH_AI_REVIEW(RELEASE, "basicworkflows-export-basic-release-with-ai-review.zip");

    private static final String CLASSPATH_DIR = "/to/be/renamed/module/deploy/";

    private final FeatureResource prerequisite;
    private final String fileName;

    FeatureResource(final FeatureResource prerequisite, final String fileName) {
        this.prerequisite = prerequisite;
        this.fileName = fileName;
    }

    /** Returns the feature that must be present before this one can be imported, or {@code null} if none. */
    public FeatureResource getPrerequisite() {
        return prerequisite;
    }

    public String getFileName() {
        return fileName;
    }

    /** Opens a fresh stream to the bundled feature transport zip. */
    public InputStream stream() throws IOException {
        final InputStream stream = FeatureResource.class.getResourceAsStream(CLASSPATH_DIR + fileName);
        if (stream == null) {
            throw new IOException("Feature export not found on classpath: " + CLASSPATH_DIR + fileName);
        }
        return stream;
    }

    /** Reads the bundled feature transport zip fully into memory. */
    public byte[] bytes() throws IOException {
        try (InputStream in = stream()) {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }
}
