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
        if (isPageStoreObject(type1)) {
            if (isPageStoreObject(type2)) {
                return 0;
            } else {
                return -1;
            }
        } else { 
            if (isPageStoreObject(type2)) {
                return 1;
            } else if (isSiteStoreObject(type2)) {
                return 0;
            } else {
                return -1;
            }
        }
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
