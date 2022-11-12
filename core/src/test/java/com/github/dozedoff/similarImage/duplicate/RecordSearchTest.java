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
package com.github.dozedoff.similarImage.duplicate;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.jimfs.Jimfs;

@RunWith(MockitoJUnitRunner.class)
public class RecordSearchTest {
	private RecordSearch cut;
	private Multimap<Long, ImageRecord> records;

	private Collection<ImageRecord> dbRecords;
	private FileSystem fs;
	private Path testDir;

	@Before
	public void setUp() throws Exception {
		fs = Jimfs.newFileSystem();
		testDir = fs.getPath(RecordSearchTest.class.getSimpleName());
		Files.createDirectory(testDir);

		dbRecords = new LinkedList<ImageRecord>();
		records = MultimapBuilder.hashKeys().hashSetValues().build();

		populateRecords();

		dbRecords.addAll(records.values());

		cut = new RecordSearch();
		cut.build(dbRecords);
	}

	@After
	public void tearDown() throws Exception {
		fs.close();
	}

	private void populateRecords() throws IOException {
		records.put(1L, generateRecord(1L));

		records.put(2L, generateRecord(2L));
		records.put(2L, generateRecord(2L));

		records.put(3L, generateRecord(3L));
		records.put(3L, generateRecord(3L));

		records.put(6L, generateRecord(6L));
		records.put(6L, generateRecord(6L));
		records.put(6L, generateRecord(6L));
	}

	private ImageRecord generateRecord(long hash) throws IOException {
		return new ImageRecord(Files.createTempFile(testDir, "RecordSearchTest", ".tmp").toString(), hash);
	}

	@Test
	public void testValidateRecordsDistinctKeys() throws Exception {
		assertThat(records.keySet().size(), is(4));
	}

	@Test
	public void testValidateRecordsTotalPairs() throws Exception {
		assertThat(records.size(), is(8));
	}

	@Test
	public void testRemoveSingleImageGroups() throws Exception {
		DuplicateUtil.removeSingleImageGroups(records);

		assertThat(records.keySet().size(), is(3));
	}

	@Test
	public void testDistanceMatchRadius0Size() throws Exception {
		Multimap<Long, ImageRecord> result = cut.distanceMatch(2L, 0L);

		assertThat(result.keySet().size(), is(1));
	}

	@Test
	public void testDistanceMatchRadius0Hash() throws Exception {
		Multimap<Long, ImageRecord> result = cut.distanceMatch(2L, 0L);

		assertThat(result.get(2L).size(), is(2));
	}

	@Test
	public void testDistanceMatchRadius1Size() throws Exception {
		Multimap<Long, ImageRecord> result = cut.distanceMatch(2L, 1L);

		assertThat(result.keySet().size(), is(3));
	}

	@Test
	public void testDistanceMatchRadius1Hash2() throws Exception {
		Multimap<Long, ImageRecord> result = cut.distanceMatch(2L, 1L);

		assertThat(result.containsKey(2L), is(true));
	}

	@Test
	public void testDistanceMatchRadius1Hash3() throws Exception {
		Multimap<Long, ImageRecord> result = cut.distanceMatch(2L, 1L);

		assertThat(result.containsKey(3L), is(true));
	}

	@Test
	public void testDistanceMatchRadius1Hash6() throws Exception {
		Multimap<Long, ImageRecord> result = cut.distanceMatch(2L, 1L);

		assertThat(result.containsKey(6L), is(true));
	}

	@Test
	public void testDistanceMatchRadius2Size() throws Exception {
		Multimap<Long, ImageRecord> result = cut.distanceMatch(2L, 2L);

		assertThat(result.keySet().size(), is(4));
	}

	@Test
	public void testDistanceMatchRadius2Hash1() throws Exception {
		Multimap<Long, ImageRecord> result = cut.distanceMatch(2L, 2L);

		assertThat(result.containsKey(1L), is(true));
	}

	@Test
	public void testSortingEmptyCollection() throws Exception {
		cut.build(Collections.emptyList());

		assertThat(cut.exactMatch(), is(empty()));
	}
}
