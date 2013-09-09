/*  Copyright (C) 2013  Nicholas Wright
    
    This file is part of similarImage - A similar image finder using pHash
    
    mmut is free software: you can redistribute it and/or modify
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
package com.github.dozedoff.similarImage.duplicate;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.LinkedList;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;

public class SortSimilarTest {
	SortSimilar sort;
	LinkedList<ImageRecord> testRecords;
	Persistence mockPersistence;

	@Before
	public void setUp() throws Exception {
		mockPersistence = mock(Persistence.class);
		sort = new SortSimilar(mockPersistence);
		createTestRecords();
	}

	private void createTestRecords() {
		testRecords = new LinkedList<ImageRecord>();

		testRecords.add(new ImageRecord("/foo/bar/1", 3));
		testRecords.add(new ImageRecord("/foo/bar/2", 4));
		testRecords.add(new ImageRecord("/foo/bar/3", 5));
		testRecords.add(new ImageRecord("/foo/foo/1", 3));
		testRecords.add(new ImageRecord("/foo/foo/5", 5));
		testRecords.add(new ImageRecord("/foo/foo/8", 2));
	}

	@Test
	public void testSortExactMatch() {
		sort.sortExactMatch(testRecords);

		ImageRecord testRecords[] = { new ImageRecord("/foo/bar/1", 3), new ImageRecord("/foo/foo/1", 3) };
		ImageRecord testRecords2[] = { new ImageRecord("/foo/bar/3", 5), new ImageRecord("/foo/foo/5", 5) };

		Set<ImageRecord> records = sort.getGroup(3);
		assertThat(records, hasItems(testRecords));

		Set<ImageRecord> records2 = sort.getGroup(5);
		assertThat(records2, hasItems(testRecords2));
	}

	@Test
	public void testGetNumberOfGroups() {
		sort.sortExactMatch(testRecords);
		assertThat(sort.getNumberOfGroups(), is(2));
	}

	@Test
	public void testIsEmpty() {
		assertThat(sort.isEmpty(), is(true));
		sort.sortExactMatch(testRecords);
		assertThat(sort.isEmpty(), is(false));
	}

	@Test
	public void testClear() {
		sort.sortExactMatch(testRecords);
		assertThat(sort.isEmpty(), is(false));
		sort.clear();
		assertThat(sort.isEmpty(), is(true));
	}
}
