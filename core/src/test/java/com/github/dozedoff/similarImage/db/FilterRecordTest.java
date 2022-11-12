/*  Copyright (C) 2016  Nicholas Wright
    
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
package com.github.dozedoff.similarImage.db;

import org.junit.Rule;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.quality.Strictness;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.util.StringUtil;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class FilterRecordTest {
	public @Rule MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

	private static final String GUARD_MSG = "Guard condition failed";

	private static final Tag TEST_TAG_ONE = new Tag("dontPanic");
	private static final Tag TEST_TAG_TWO = new Tag("towel");
	private static final long HASH_ONE = 42L;
	private static final long HASH_TWO = 7L;

	private FilterRecord filterRecord;

	@Mock
	private FilterRepository filterRepository;

	@Before
	public void setUp() throws Exception {
		filterRecord = new FilterRecord(HASH_ONE, TEST_TAG_ONE, null);
	}

	@Test
	public void testGetpHash() throws Exception {
		assertThat(filterRecord.getpHash(), is(HASH_ONE));
	}

	@Test
	public void testSetpHash() throws Exception {
		assertThat(GUARD_MSG, filterRecord.getpHash(), is(HASH_ONE));

		filterRecord.setpHash(HASH_TWO);

		assertThat(filterRecord.getpHash(), is(HASH_TWO));
	}

	@Test
	public void testGetReason() throws Exception {
		assertThat(filterRecord.getTag(), is(TEST_TAG_ONE));
	}

	@Test
	public void testSetReason() throws Exception {
		assertThat(GUARD_MSG, filterRecord.getTag(), is(TEST_TAG_ONE));

		filterRecord.setTag(TEST_TAG_TWO);

		assertThat(filterRecord.getTag(), is(TEST_TAG_TWO));
	}

	@Test
	public void testEqualsIsEqual() throws Exception {
		FilterRecord other = new FilterRecord(HASH_ONE, TEST_TAG_ONE, null);
		assertThat(filterRecord.equals(other), is(true));
	}

	@Test
	public void testEquals() throws Exception {
		EqualsVerifier.forClass(FilterRecord.class).withIgnoredFields("id").suppress(Warning.NONFINAL_FIELDS).verify();
	}

	@Test
	public void testGetTags() throws Exception {
		FilterRecord.getTags(filterRepository, TEST_TAG_ONE);

		verify(filterRepository).getByTag(TEST_TAG_ONE);
	}

	@Test
	public void testGetTagsAllTags() throws Exception {
		FilterRecord.getTags(filterRepository, new Tag(StringUtil.MATCH_ALL_TAGS));

		verify(filterRepository).getAll();
	}
}
