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
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
	private Multimap<Long, Multimap<Long, ImageRecord>> mergeTest;

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

		setupMergeTest();
	}

	private void setupMergeTest() {
		mergeTest = MultimapBuilder.hashKeys().hashSetValues().build();

		mergeTest.put(1L, buildMapWithHashes(2L, 3L, 5L));
		mergeTest.put(3L, buildMapWithHashes(3L, 2L, 5L));
		mergeTest.put(4L, buildMapWithHashes(2L, 8L));
	}

	private void createRecordWithHash(long pHash, int sequenceNumber) {
		records.add(new ImageRecord(Integer.toString(sequenceNumber), pHash));
	}

	private Multimap<Long, ImageRecord> buildMapWithHashes(long... hashes) {
		Multimap<Long, ImageRecord> result = MultimapBuilder.hashKeys().hashSetValues().build();

		for (long hash : hashes) {
			result.put(hash, null);
		}

		return result;
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
	public void testGroupByHashEntryPath() throws Exception {
		Multimap<Long, ImageRecord> group = DuplicateUtil.groupByHash(records);

		assertThat(group.get(2L), hasItem(new ImageRecord("foo", 2L)));
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

	@Test
	public void testMergeSetSize() throws Exception {
		assertThat(mergeTest.keySet().size(), is(3));
	}

	@Test
	public void testRemoveDuplicateSets() throws Exception {
		DuplicateUtil.removeDuplicateSets(mergeTest);

		assertThat(mergeTest.keySet().size(), is(2));
	}

	@Test
	public void testHashSumNoHashes() throws Exception {
		assertThat(DuplicateUtil.hashSum(Collections.emptyList()), is(BigInteger.ZERO));
	}

	@Test
	public void testHashSum() throws Exception {
		List<Long> hashes = new LinkedList<Long>();
		hashes.add(2L);
		hashes.add(3L);

		assertThat(DuplicateUtil.hashSum(hashes), is(new BigInteger("5")));
	}
}
