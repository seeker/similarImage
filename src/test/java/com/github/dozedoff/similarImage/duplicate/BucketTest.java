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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class BucketTest {
	private static final String GUARD_MSG = "Guard condition failed";
	private Bucket<Integer, Integer> bucket;

	@Before
	public void setUp() throws Exception {
		bucket = new Bucket<Integer, Integer>(1, 11);
	}

	@Test
	public void testGetId() throws Exception {
		assertThat(bucket.getId(), is(1));
	}

	@Test
	public void testGetBucketVerifyEntries() throws Exception {
		assertThat(bucket.getBucket(), hasItem(11));
	}

	@Test
	public void testGetBucketVerifySize() throws Exception {
		assertThat(bucket.getBucket(), hasSize(1));
	}

	@Test
	public void testAddVerifyEntry() throws Exception {
		assertThat(GUARD_MSG, bucket.getBucket(), not(hasItem(42)));

		bucket.add(42);
		assertThat(bucket.getBucket(), hasItem(42));
	}

	@Test
	public void testAddVerifySize() throws Exception {
		assertThat(GUARD_MSG, bucket.getBucket(), not(hasItem(42)));

		bucket.add(42);
		assertThat(bucket.getBucket(), hasSize(2));
	}

	@Test
	public void testGetSize() throws Exception {
		assertThat(bucket.getSize(), is(1));
	}

	@Test
	public void testGetSizeAfterAdd() throws Exception {
		bucket.add(42);
		assertThat(bucket.getSize(), is(2));
	}

	@Test
	public void testIsEmptyWhenFilled() throws Exception {
		assertThat(bucket.isEmpty(), is(false));
	}

	@Test
	public void testIsEmptyWhenEmpty() throws Exception {
		bucket = new Bucket<Integer, Integer>(1);
		assertThat(bucket.isEmpty(), is(true));
	}

	@Test
	public void testEqualsNull() throws Exception {
		assertThat(bucket.equals(null), is(false));
	}

	@Test
	public void testEqualsSelf() throws Exception {
		assertThat(bucket.equals(bucket), is(true));
	}

	@Test
	public void testEqualsSameIdAndEntries() throws Exception {
		Bucket<Integer, Integer> other = new Bucket<Integer, Integer>(1, 11);

		assertThat(bucket.equals(other), is(true));
	}

	@Test
	public void testEqualsSameIdDiffrentEntry() throws Exception {
		Bucket<Integer, Integer> other = new Bucket<Integer, Integer>(1, 22);

		assertThat(bucket.equals(other), is(true));
	}

	@Test
	public void testEqualsDiffrentIdSameEntry() throws Exception {
		Bucket<Integer, Integer> other = new Bucket<Integer, Integer>(5, 11);

		assertThat(bucket.equals(other), is(false));
	}

	@Test
	public void testEqualsDiffrentIdDiffrentEntry() throws Exception {
		Bucket<Integer, Integer> other = new Bucket<Integer, Integer>(5, 55);

		assertThat(bucket.equals(other), is(false));
	}

	@Test
	public void testCreateBucketWithListVerifyEntries() {
		LinkedList<Integer> entries = new LinkedList<>();

		entries.add(1);
		entries.add(2);
		entries.add(3);

		bucket = new Bucket<Integer, Integer>(1, entries);

		assertThat(bucket.getBucket(), hasItems(1, 2, 3));
	}

	@Test
	public void testCreateBucketWithListVerifySize() {
		LinkedList<Integer> entries = new LinkedList<>();

		entries.add(1);
		entries.add(2);
		entries.add(3);

		bucket = new Bucket<Integer, Integer>(1, entries);

		assertThat(bucket.getBucket(), hasSize(3));
	}

	@Test
	public void testEquals() {
		EqualsVerifier.forClass(Bucket.class).verify();
	}
}
