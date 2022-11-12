/*  Copyright (C) 2017  Nicholas Wright
    
    This file is part of similarImage - A similar image finder using pHash
    
    similarImage is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.dozedoff.similarImage.thread.pipeline;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

@RunWith(MockitoJUnitRunner.class)
public class RemoveSingleImageSetStageTest {
	private static final long HASH_A = 0;
	private static final long HASH_B = 1;

	private ImageRecord imageA;

	private ImageRecord imageB;

	private Multimap<Long, ImageRecord> testMap;

	private RemoveSingleImageSetStage cut;

	@Before
	public void setUp() throws Exception {
		cut = new RemoveSingleImageSetStage();
		testMap = MultimapBuilder.hashKeys().hashSetValues().build();
		
		imageA = new ImageRecord("", HASH_A);
		imageB = new ImageRecord("", HASH_B);

		testMap.put(HASH_A, imageA);

		testMap.put(HASH_B, imageA);
		testMap.put(HASH_B, imageB);
	}

	@Test
	public void testRemoveSingleImageGroup() throws Exception {
		cut.apply(testMap);

		assertThat(testMap.containsKey(HASH_A), is(false));
	}

	@Test
	public void testParameterReturned() throws Exception {
		assertThat(cut.apply(testMap), is(sameInstance(testMap)));
	}
}
