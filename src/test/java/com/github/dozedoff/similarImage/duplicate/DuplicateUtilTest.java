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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

@SuppressWarnings("deprecation")
public class DuplicateUtilTest {
	private static final int NUM_OF_RECORDS = 10;
	private LinkedList<ImageRecord> records;
	private Set<Bucket<Long, ImageRecord>> conversionSet;

	private Multimap<Long, ImageRecord> conversionMap;

	@Before
	public void setUp() throws Exception {
		records = new LinkedList<>();

		for (int i = 0; i < NUM_OF_RECORDS; i++) {
			createRecordWithHash(i, i);
		}

		records.add(new ImageRecord("foo", 2));
		createRecordWithHash(5, 42);
		createRecordWithHash(5, 43);
		
		conversionSet = new HashSet<>();
		conversionSet.add(new Bucket<Long, ImageRecord>(1L, new ImageRecord("a", 1L)));
		conversionSet.add(
				new Bucket<Long, ImageRecord>(2L, Arrays.asList(new ImageRecord[] { new ImageRecord("b", 2L), new ImageRecord("c", 2L) })));

		conversionMap = MultimapBuilder.hashKeys().hashSetValues().build();
		conversionMap.put(1L, new ImageRecord("a", 1L));

		conversionMap.put(2L, new ImageRecord("b", 2L));
		conversionMap.put(2L, new ImageRecord("c", 2L));

	}

	private void createRecordWithHash(long pHash, int sequenceNumber) {
		records.add(new ImageRecord(Integer.toString(sequenceNumber), pHash));
	}

	@Test
	public void testSortIntoBucketsSize() throws Exception {
		ArrayList<Bucket<Long, ImageRecord>> buckets = DuplicateUtil.sortIntoBuckets(records);
		assertThat(buckets.size(), is(10));
	}

	@Test
	public void testSortIntoBucketsBucket2Size() throws Exception {
		ArrayList<Bucket<Long, ImageRecord>> buckets = DuplicateUtil.sortIntoBuckets(records);
		int index = Collections.binarySearch(buckets, new Bucket<Long, ImageRecord>(2L), new BucketComperator());
		Bucket<Long, ImageRecord> bucket = buckets.get(index);

		assertThat(bucket.getSize(), is(2));
	}

	@Test
	public void testSortIntoBucketsBucket2Path() throws Exception {
		ArrayList<Bucket<Long, ImageRecord>> buckets = DuplicateUtil.sortIntoBuckets(records);
		int index = Collections.binarySearch(buckets, new Bucket<Long, ImageRecord>(2L), new BucketComperator());
		Bucket<Long, ImageRecord> bucket = buckets.get(index);

		assertThat(bucket.getBucket(), hasItem(new ImageRecord("foo", 2L)));
	}

	@Test
	public void testSortIntoBucketsBucket2PathNot() throws Exception {
		ArrayList<Bucket<Long, ImageRecord>> buckets = DuplicateUtil.sortIntoBuckets(records);
		int index = Collections.binarySearch(buckets, new Bucket<Long, ImageRecord>(2L), new BucketComperator());
		Bucket<Long, ImageRecord> bucket = buckets.get(index);

		assertThat(bucket.getBucket(), not(hasItem(new ImageRecord("bar", 2L))));
	}

	@Test
	public void testSortIntoBucketsBucket5Size() throws Exception {
		ArrayList<Bucket<Long, ImageRecord>> buckets = DuplicateUtil.sortIntoBuckets(records);
		int index = Collections.binarySearch(buckets, new Bucket<Long, ImageRecord>(5L), new BucketComperator());
		Bucket<Long, ImageRecord> bucket = buckets.get(index);

		assertThat(bucket.getSize(), is(3));
	}

	@Test
	public void testGroupByHashNumberOfGroups() throws Exception {
		Multimap<Long, ImageRecord> group = DuplicateUtil.groupByHash(records);
		assertThat(group.keySet().size(), is(10));
	}

	@Test
	public void testGroupByHashSizeOfGroup() throws Exception {
		Multimap<Long, ImageRecord> group = DuplicateUtil.groupByHash(records);
		assertThat(group.get(5L).size(), is(3));
	}

	@Test
	public void testMultimapToBucketSetSize() throws Exception {
		assertThat(DuplicateUtil.multimapToBucketSet(conversionMap).size(), is(2));
	}

	@Test
	public void testMultimapToBucketSetKey1() throws Exception {
		assertThat(DuplicateUtil.multimapToBucketSet(conversionMap).contains(new Bucket<Long, ImageRecord>(1L)), is(true));
	}

	@Test
	public void testMultimapToBucketSetKey2() throws Exception {
		assertThat(DuplicateUtil.multimapToBucketSet(conversionMap).contains(new Bucket<Long, ImageRecord>(2L)), is(true));
	}

	@Test
	public void testMultimapToBucketSetKey2Size() throws Exception {
		assertThat(DuplicateUtil.multimapToBucketSet(conversionMap).toArray().length, is(2));
	}

	@Test
	public void testBucketSetToMultimapSetSize() throws Exception {
		assertThat(DuplicateUtil.bucketSetToMultimap(conversionSet).keySet().size(), is(2));
	}

	@Test
	public void testBucketSetToMultimapSet1Size() throws Exception {
		assertThat(DuplicateUtil.bucketSetToMultimap(conversionSet).get(1L).size(), is(1));
	}

	@Test
	public void testBucketSetToMultimapSet2Size() throws Exception {
		assertThat(DuplicateUtil.bucketSetToMultimap(conversionSet).get(2L).size(), is(2));
	}
}
