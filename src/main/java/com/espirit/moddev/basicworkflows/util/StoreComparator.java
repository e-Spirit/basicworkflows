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

import java.util.Comparator;

import com.espirit.moddev.basicworkflows.release.ReleaseObject;

import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.ReferenceEntry;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.Store;
import de.espirit.firstspirit.access.store.Store.Type;
import de.espirit.firstspirit.access.store.StoreElement;
import de.espirit.or.schema.Entity;

public class StoreComparator implements Comparator<Object> {

    @Override
    public int compare(final Object o1, final Object o2) {
        final StoreElement storeElement1 = getStoreElement(o1);
        final StoreElement storeElement2 = getStoreElement(o2);

        if (storeElement1 == null) {
            if (storeElement2 != null) {
                return -1;
            } else {
                return 0;
            }
        } else {
            if (storeElement2 == null) {
                return 1;
            }
        }
        final Type type1 = storeElement1.getStore().getType();
        final Type type2 = storeElement2.getStore().getType();
        // MediaStoreObject < PageStoreObject < SiteStoreObject < others
        if (isMediaStoreObject(type1)) {
            if (isMediaStoreObject(type2)) {
                return 0;
            } else {
                return -1;
            }
        } else if (isPageStoreObject(type1)) {
            if (isMediaStoreObject(type2)) {
                return 1;
            }
            else if (isPageStoreObject(type2)) {
                return 0;
            } else {
                return -1;
            }
        } else {
            if (isMediaStoreObject(type2) || isPageStoreObject(type2)) {
                return 1;
            } else if (isSiteStoreObject(type2)) {
                return 0;
            } else {
                return -1;
            }
        }
    }
    
    private boolean isMediaStoreObject(final Type type) {
        return type == Store.Type.MEDIASTORE;
    }

    private boolean isPageStoreObject(final Type type) {
        return type == Store.Type.PAGESTORE;
    }


    private boolean isSiteStoreObject(final Type type) {
        return type == Store.Type.SITESTORE;
    }


    private StoreElement getStoreElement(final Object object) {
        StoreElement storeElement1 = null;
        if (!(object instanceof ReferenceEntry && ((ReferenceEntry) object).getReferencedObject() instanceof Entity)) {
            if (object instanceof IDProvider) {
                storeElement1 = (IDProvider) object;
            } else {
                storeElement1 = ((ReferenceEntry) object).getReferencedElement();
            }
        } else {
            Logging.logWarning(String.format("list contains not comparable object [type: %s]", object.getClass()), ReleaseObject.class);
        }
        return storeElement1;
    }

}
