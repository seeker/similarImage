/*  Copyright (C) 2014  Nicholas Wright
    
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
package com.github.dozedoff.similarImage.duplicate;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.similarImage.db.FilterRecord;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.google.common.collect.Lists;

public class SortSimilarTest {
	SortSimilar sort;
	LinkedList<ImageRecord> testRecords;
	Persistence mockPersistence;
	Set<Bucket<Long, ImageRecord>> result;
	Set<Bucket<Long, ImageRecord>> toMerge;

	@Before
	public void setUp() throws Exception {
		mockPersistence = mock(Persistence.class);
		sort = new SortSimilar(mockPersistence);

		result = new HashSet<Bucket<Long, ImageRecord>>();
		toMerge = new HashSet<Bucket<Long, ImageRecord>>();

		buildTestSets();
		createTestRecords();
	}

	private void buildTestSets() {
		result.add(createBucketWithRecords(1, "a", "b"));
		result.add(createBucketWithRecords(2, "c", "d"));
		result.add(createBucketWithRecords(3, "e", "f"));

		toMerge.add(createBucketWithRecords(1, "a", "h"));
		toMerge.add(createBucketWithRecords(3, "g"));
		toMerge.add(createBucketWithRecords(4, "i"));
	}

	private List<ImageRecord> createImageRecords(long id, String... paths) {
		LinkedList<ImageRecord> records = new LinkedList<>();

		for (String path : paths) {
			records.add(new ImageRecord(path, id));
		}

		return records;
	}

	private Bucket<Long, ImageRecord> createBucketWithRecords(long id, String... paths) {
		return new Bucket<Long, ImageRecord>(id, createImageRecords(id, paths));

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

	private List<FilterRecord> createFilterRecords(String reason, long... pHash) {
		LinkedList<FilterRecord> filterRecords = new LinkedList<>();

		for (long h : pHash) {
			filterRecords.add(new FilterRecord(h, reason));
		}

		return filterRecords;
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
	public void testSortHammingDistance() {
		sort.sortHammingDistance(1, testRecords);

		assertThat(sort.getNumberOfGroups(), is(4));
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

	private void assertGroupOneCorrect() {
		Set<ImageRecord> group = sort.getGroup(1L);
		assertThat(group, hasItem(testRecords.get(0)));
		assertThat(group, hasItem(testRecords.get(2)));
		assertThat(group, hasItem(testRecords.get(3)));
		assertThat(group, hasItem(testRecords.get(4)));
		assertThat(group.size(), is(4));
	}

	private void assertGroupTwoCorrect() {
		Set<ImageRecord> group = sort.getGroup(2L);
		assertThat(group, hasItem(testRecords.get(0)));
		assertThat(group, hasItem(testRecords.get(3)));
		assertThat(group, hasItem(testRecords.get(5)));
		assertThat(group.size(), is(3));
	}

	@Test
	public void testMergeSetsSetSize() {
		sort.mergeSets(result, toMerge);

		assertThat(result.size(), is(4));
	}

	@Test
	public void testMergeSetsMergeGroup1() {
		Bucket<Long, ImageRecord> toTest =null;
		
		sort.mergeSets(result, toMerge);

		for(Bucket<Long, ImageRecord> bucket : result) {
			if(bucket.getId() == 1) {
				toTest = bucket;
			}
		}

		assertThat(toTest, is(notNullValue())); // Guard condition

		assertThat(toTest.getBucket(), hasItems(Lists.newArrayList(createImageRecords(1, "a", "b")).toArray(new ImageRecord[0])));
	}

	@Test
	public void testMergeSetsMergeGroup3() {
		Bucket<Long, ImageRecord> toTest = null;

		sort.mergeSets(result, toMerge);

		for (Bucket<Long, ImageRecord> bucket : result) {
			if (bucket.getId() == 3) {
				toTest = bucket;
			}
		}

		assertThat(toTest, is(notNullValue())); // Guard condition

		assertThat(toTest.getBucket(), hasItems(Lists.newArrayList(createImageRecords(3, "e", "f", "g")).toArray(new ImageRecord[0])));
	}

	@Test
	public void testMergeSetsMergeGroup4() {
		Bucket<Long, ImageRecord> toTest = null;

		sort.mergeSets(result, toMerge);

		for (Bucket<Long, ImageRecord> bucket : result) {
			if (bucket.getId() == 4) {
				toTest = bucket;
			}
		}

		assertThat(toTest, is(notNullValue())); // Guard condition

		assertThat(toTest.getBucket(),
				hasItems(Lists.newArrayList(createImageRecords(4, "i")).toArray(new ImageRecord[0])));
	}
}
