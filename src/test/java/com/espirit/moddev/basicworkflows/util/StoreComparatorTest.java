package com.espirit.moddev.basicworkflows.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.Store;
import de.espirit.firstspirit.access.store.Store.Type;

public class StoreComparatorTest {

	@Test
	public void testStoreComparator() throws Exception {
		List<IDProvider> releaseObjects = new ArrayList<>();
		releaseObjects.add(createStoreElement(Store.Type.SITESTORE));
		releaseObjects.add(createStoreElement(Store.Type.MEDIASTORE));
		releaseObjects.add(createStoreElement(Store.Type.MEDIASTORE));
		releaseObjects.add(createStoreElement(Store.Type.PAGESTORE));
		releaseObjects.add(createStoreElement(Store.Type.PAGESTORE));
		releaseObjects.add(createStoreElement(Store.Type.SITESTORE));

		Collections.sort(releaseObjects, new StoreComparator());

		assertEquals(releaseObjects.get(0).getStore().getType(), Store.Type.PAGESTORE);
		assertEquals(releaseObjects.get(1).getStore().getType(), Store.Type.PAGESTORE);
		assertEquals(releaseObjects.get(2).getStore().getType(), Store.Type.SITESTORE);
		assertEquals(releaseObjects.get(3).getStore().getType(), Store.Type.SITESTORE);
		assertEquals(releaseObjects.get(4).getStore().getType(), Store.Type.MEDIASTORE);
		assertEquals(releaseObjects.get(5).getStore().getType(), Store.Type.MEDIASTORE);
	}


	private IDProvider createStoreElement(Type type) {
		IDProvider idProvider = mock(IDProvider.class);
		Store store = mock(Store.class);
		when(store.getType()).thenReturn(type);
		when(idProvider.getStore()).thenReturn(store);
		return idProvider;
	}
}
