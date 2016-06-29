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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

@RunWith(MockitoJUnitRunner.class)
public class RecordSearchTest {
	private RecordSearch cut;
	private Multimap<Long, ImageRecord> records;

	private Collection<ImageRecord> dbRecords;

	private ImageRecord imageRecord;
	
	@Before
	public void setUp() throws Exception {
		imageRecord = new ImageRecord("foo", 42L);

		dbRecords = new LinkedList<ImageRecord>();
		dbRecords.add(imageRecord);

		cut = new RecordSearch(dbRecords);
		records = MultimapBuilder.hashKeys().hashSetValues().build();

		populateRecords();
	}

	private void populateRecords() throws IOException {
		records.put(1L, generateRecord(1L));

		records.put(2L, generateRecord(2L));
		records.put(2L, generateRecord(2L));

		records.put(3L, generateRecord(3L));
		records.put(3L, generateRecord(3L));
		records.put(3L, generateRecord(3L));
	}

	private ImageRecord generateRecord(long hash) throws IOException {
		return new ImageRecord(Files.createTempFile("RecordSearchTest", ".tmp").toString(), hash);
	}

	@Test
	public void testValidateRecordsDistinctKeys() throws Exception {
		assertThat(records.keySet().size(), is(3));
	}

	@Test
	public void testValidateRecordsTotalPairs() throws Exception {
		assertThat(records.size(), is(6));
	}

	@Test
	public void testRemoveSingleImageGroups() throws Exception {
		Multimap<Long, ImageRecord> result = cut.removeSingleImageGroups(records);

		assertThat(result.keySet().size(), is(2));
	}
}
