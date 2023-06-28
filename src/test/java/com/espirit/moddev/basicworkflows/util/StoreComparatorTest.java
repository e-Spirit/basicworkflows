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

		assertEquals(Store.Type.MEDIASTORE, releaseObjects.get(0).getStore().getType());
		assertEquals(Store.Type.MEDIASTORE, releaseObjects.get(1).getStore().getType());
		assertEquals(Store.Type.PAGESTORE, releaseObjects.get(2).getStore().getType());
		assertEquals(Store.Type.PAGESTORE, releaseObjects.get(3).getStore().getType());
		assertEquals(Store.Type.SITESTORE, releaseObjects.get(4).getStore().getType());
		assertEquals(Store.Type.SITESTORE, releaseObjects.get(5).getStore().getType());
	}


	private IDProvider createStoreElement(Type type) {
		IDProvider idProvider = mock(IDProvider.class);
		Store store = mock(Store.class);
		when(store.getType()).thenReturn(type);
		when(idProvider.getStore()).thenReturn(store);
		return idProvider;
	}
}
