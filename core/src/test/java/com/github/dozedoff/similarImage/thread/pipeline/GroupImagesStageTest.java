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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.similarImage.db.ImageRecord;

public class GroupImagesStageTest {
	private static final long HASH_A = 0;
	private static final long HASH_B = 1;
	private static final int DISTANCE = 42;

	private GroupImagesStage cut;

	private ImageRecord imageA;
	private ImageRecord imageB;
	private ImageRecord imageC;

	private List<ImageRecord> images;

	private ImageRecord createImage(long hash) {
		return new ImageRecord(String.valueOf(hash), hash);
	}

	@Before
	public void setUp() throws Exception {
		cut = new GroupImagesStage();

		imageA = createImage(HASH_A);
		imageB = createImage(HASH_B);
		imageC = createImage(HASH_B);

		images = Arrays.asList(new ImageRecord[] { imageA, imageB, imageC });
	}

	@Test
	public void testNoDuplicatesInGroup() throws Exception {
		assertThat(cut.apply(images).get(HASH_B), hasSize(1));
	}

	@Test
	public void testSortedByHash() throws Exception {
		assertThat(cut.apply(images).get(HASH_A), hasItem(imageA));
	}

	@Test
	public void testHammingDistance() throws Exception {
		cut = new GroupImagesStage(1);

		assertThat(cut.apply(images).get(HASH_B), hasItems(imageA, imageB));
	}

	@Test
	public void testGetHammingDistance() throws Exception {
		cut = new GroupImagesStage(DISTANCE);

		assertThat(cut.getHammingDistance(), is(DISTANCE));
	}

	@Test
	public void testDefaultDistance() throws Exception {
		assertThat(cut.getHammingDistance(), is(0));
	}
}
