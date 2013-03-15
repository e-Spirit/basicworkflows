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

package com.espirit.moddev;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration test for basicworkflows
 */
public class basicworkflowsTestIT {

    String MODULE_DESCRIPTOR = "META-INF/module.xml";

    /**
     * Check if FSM is valid
     */
    @Test
    public void testisFSMValid() {
        try {
            File directory = new File("target");
            Collection files = FileUtils.listFiles(directory, new WildcardFileFilter("*.fsm"), null);
            assertTrue("FSM doesn't contain any files",files.iterator().hasNext());
            if(files.iterator().hasNext()) {
                ZipFile _fsmZip = new ZipFile((File) files.iterator().next());
                ZipEntry fsmEntry = _fsmZip.getEntry(MODULE_DESCRIPTOR);
                assertNotNull("Couldn't find module descriptor (module.xml) in fsm file",fsmEntry);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
