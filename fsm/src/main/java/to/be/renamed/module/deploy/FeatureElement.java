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

import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.Store;

/** A single store element declared in a feature transport's {@code feature.xml}. */
public final class FeatureElement {

    private final Store.Type storeType;
    private final IDProvider.UidType uidType;
    private final String uid;

    public FeatureElement(final Store.Type storeType, final IDProvider.UidType uidType, final String uid) {
        this.storeType = storeType;
        this.uidType = uidType;
        this.uid = uid;
    }

    public Store.Type getStoreType() {
        return storeType;
    }

    public IDProvider.UidType getUidType() {
        return uidType;
    }

    public String getUid() {
        return uid;
    }

    @Override
    public String toString() {
        return storeType + ":" + uidType + ":" + uid;
    }
}
