/*-
 * ========================LICENSE_START=================================
 * BasicWorkflows Module
 * %%
 * Copyright (C) 2012 - 2018 e-Spirit AG
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
 * =========================LICENSE_END==================================
 */
package com.espirit.moddev.basicworkflows.util;

import de.espirit.firstspirit.access.BaseContext;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.Store;
import de.espirit.firstspirit.access.store.StoreElementFilter;
import de.espirit.firstspirit.access.store.StoreElementFolder;
import de.espirit.firstspirit.access.store.globalstore.GCAFolder;
import de.espirit.firstspirit.access.store.globalstore.GCAPage;
import de.espirit.firstspirit.access.store.mediastore.Media;
import de.espirit.firstspirit.access.store.mediastore.MediaFolder;
import de.espirit.firstspirit.access.store.pagestore.Page;
import de.espirit.firstspirit.access.store.pagestore.PageFolder;
import de.espirit.firstspirit.access.store.sitestore.DocumentGroup;
import de.espirit.firstspirit.access.store.sitestore.PageRef;
import de.espirit.firstspirit.access.store.sitestore.PageRefFolder;
import de.espirit.firstspirit.access.store.sitestore.SiteStoreRoot;
import de.espirit.firstspirit.agency.StoreAgent;

/**
 * Utility class to find a specified IdProvider.
 */
public class StoreUtil {

    private final BaseContext context;

    /**
     * Constructor.
     * @param context the context to use.
     */
    public StoreUtil(BaseContext context) {
        this.context = context;
    }

    /**
     * Load page ref by uid.
     *
     * @param srcUid  the src uid
     * @return the page ref or null if srcUid is null or empty
     */
    public PageRef loadPageRefByUid(final String srcUid) {
        if (context == null) {
            throw new IllegalArgumentException("BaseContext is null");
        }
        if (srcUid != null && !srcUid.isEmpty()) {
            StoreAgent storeAgent = context.requireSpecialist(StoreAgent.TYPE);
            final SiteStoreRoot siteStoreRoot = (SiteStoreRoot) storeAgent.getStore(Store.Type.SITESTORE, false);
            return (PageRef) siteStoreRoot.getStoreElement(srcUid, IDProvider.UidType.SITESTORE_LEAF);
        }
        return null;
    }


    /**
     * Get a filter to get recursively only the children that needs to be released
     *
     * @param folder, the root folder where to call .getChildren()
     * @return StoreElementFilter
     */
    public StoreElementFilter getChildrenSpecificFilter(StoreElementFolder folder) {
        StoreElementFilter filter = null;

        if (folder instanceof PageFolder) {
            filter = StoreElementFilter.on(PageFolder.class, Page.class);
        } else if (folder instanceof MediaFolder) {
            filter = StoreElementFilter.on(MediaFolder.class, Media.class);
        } else if (folder instanceof PageRefFolder) {
            filter = StoreElementFilter.on(PageRefFolder.class, PageRef.class, DocumentGroup.class);
        } else if (folder instanceof GCAFolder) {
            filter = StoreElementFilter.on(GCAFolder.class, GCAPage.class);
        }

        return filter;
    }

}
