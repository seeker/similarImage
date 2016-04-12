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
package com.github.dozedoff.similarImage.db;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.j256.ormlite.dao.CloseableWrappedIterable;

public class PersistenceTest {
	private static final String GUARD_MSG = "Guard condition failed";

	private Persistence persistence;
	private ArrayList<ImageRecord> imageRecords;

	@Before
	public void setUp() throws Exception {
		persistence = new Persistence(Files.createTempFile("PersistenceTest", ".db"));

		createImageRecords();
		persistence.batchAddRecord(imageRecords);

		addFilterRecords();
		addBadFilesRecords();
	}

	private void createImageRecords() {
		imageRecords = new ArrayList<>(6);

		imageRecords.add(new ImageRecord("foo", 0));
		imageRecords.add(new ImageRecord("foobar", 1));
		imageRecords.add(new ImageRecord("moo", 2));
		imageRecords.add(new ImageRecord("ribit", 3));
		imageRecords.add(new ImageRecord("duck", 2));
		imageRecords.add(new ImageRecord("croak", 3));
	}

	private void addFilterRecords() throws SQLException {
		persistence.addFilter(new FilterRecord(3, "frogs"));
		persistence.addFilter(new FilterRecord(2, "animals"));

		persistence.addFilter(new FilterRecord(0, "other"));
		persistence.addFilter(new FilterRecord(1, "other"));
	}

	private void addBadFilesRecords() throws SQLException {
		persistence.addBadFile(new BadFileRecord(Paths.get("bad")));
		persistence.addBadFile(new BadFileRecord(Paths.get("evenWorse")));
	}

	@Test
	public void testAddRecord() throws Exception {
		assertThat(GUARD_MSG, persistence.getRecord(Paths.get("derp")), nullValue());

		persistence.addRecord(new ImageRecord("derp", 123));

		assertThat(GUARD_MSG, persistence.getRecord(Paths.get("derp")), is(new ImageRecord("derp", 123)));
	}

	@Test
	public void testBatchAddRecord() throws Exception {
		LinkedList<ImageRecord> records = new LinkedList<>();

		assertThat(GUARD_MSG, persistence.isPathRecorded(Paths.get("new")), is(false));
		assertThat(GUARD_MSG, persistence.isPathRecorded(Paths.get("fresh")), is(false));

		records.add(new ImageRecord("new", 14));
		records.add(new ImageRecord("fresh", 15));

		persistence.batchAddRecord(records);

		assertThat(persistence.isPathRecorded(Paths.get("new")), is(true));
		assertThat(persistence.isPathRecorded(Paths.get("fresh")), is(true));
	}

	@Test
	public void testGetRecord() throws Exception {
		assertThat(persistence.getRecord(Paths.get("moo")), is(new ImageRecord("moo", 2)));
	}

	@Test
	public void testGetRecordNotFound() throws Exception {
		ImageRecord ir = persistence.getRecord(Paths.get("none"));

		assertThat(ir, nullValue());
	}

	@Test
	public void testGetRecordsVerifyEntries() throws Exception {
		List<ImageRecord> images = persistence.getRecords(2);

		assertThat(images, hasItems(imageRecords.get(2), imageRecords.get(4)));
	}

	@Test
	public void testGetRecordsVerifySize() throws Exception {
		List<ImageRecord> images = persistence.getRecords(2);

		assertThat(images, hasSize(2));
	}

	@Test
	public void testDeleteRecord() throws Exception {
		ImageRecord ir = persistence.getRecord(Paths.get("foo"));
		assertThat(GUARD_MSG, ir, not(nullValue()));

		persistence.deleteRecord(imageRecords.get(0));
		ir = persistence.getRecord(Paths.get("foo"));

		assertThat(ir, nullValue());
	}

	@Test
	public void testIsPathRecordedNotRecorded() throws Exception {
		assertThat(persistence.isPathRecorded(Paths.get("rawr")), is(false));
	}

	@Test
	public void testIsPathRecordedIsRecorded() throws Exception {
		assertThat(GUARD_MSG, persistence.isPathRecorded(Paths.get("rawr")), is(false));

		persistence.addRecord(new ImageRecord("rawr", 42));

		assertThat(persistence.isPathRecorded(Paths.get("rawr")), is(true));
	}

	@Test
	public void testIsBadFileIsBad() throws Exception {
		assertThat(persistence.isBadFile(Paths.get("bad")), is(true));
	}

	@Test
	public void testIsBadFileIsGood() throws Exception {
		assertThat(persistence.isBadFile(Paths.get("good")), is(false));
	}

	@Test
	public void testGetImageRecordIterator() throws Exception {
		CloseableWrappedIterable<ImageRecord> ite = persistence.getImageRecordIterator();

		LinkedList<ImageRecord> results = new LinkedList<>();

		for (ImageRecord ir : ite) {
			results.add(ir);
		}

		assertThat(results, hasSize(6));
	}

	@Test
	public void testGetAllRecordsVerifyEntries() throws Exception {
		List<ImageRecord> records = persistence.getAllRecords();
		ImageRecord[] array = new ImageRecord[imageRecords.size()];
		imageRecords.toArray(array);

		assertThat(records, hasItems(array));
	}

	@Test
	public void testGetAllRecordsVerifySize() throws Exception {
		List<ImageRecord> records = persistence.getAllRecords();

		assertThat(records, hasSize(6));
	}

	@Test
	public void testAddFilter() throws Exception {
		assertThat(GUARD_MSG, persistence.filterExists(55), is(false));

		persistence.addFilter(new FilterRecord(55, "another"));

		assertThat(persistence.filterExists(55), is(true));
	}

	@Test
	public void testAddBadFile() throws Exception {
		assertThat(GUARD_MSG, persistence.isBadFile(Paths.get("drEvil")), is(false));

		persistence.addBadFile(new BadFileRecord(Paths.get("drEvil")));

		assertThat(persistence.isBadFile(Paths.get("drEvil")), is(true));
	}

	@Test
	public void testFilterExistsExistingFilter() throws Exception {
		assertThat(persistence.filterExists(2), is(true));
	}

	@Test
	public void testFilterExistsNonExistingFilter() throws Exception {
		assertThat(persistence.filterExists(42), is(false));
	}

	@Test
	public void testGetFilterExists() throws Exception {
		assertThat(persistence.getFilter(3), is(new FilterRecord(3, "frogs")));
	}

	@Test
	public void testGetFilterNonExistant() throws Exception {
		assertThat(persistence.getFilter(42), nullValue());
	}

	@Test
	public void testGetAllFiltersVerifyEntries() throws Exception {
		List<FilterRecord> filters = persistence.getAllFilters();

		assertThat(
				filters,
				hasItems(new FilterRecord(3, "frogs"), new FilterRecord(2, "animals"), new FilterRecord(0, "other"), new FilterRecord(1,
						"other")));
	}

	@Test
	public void testGetAllFiltersVerifySize() throws Exception {
		List<FilterRecord> filters = persistence.getAllFilters();

		assertThat(filters, hasSize(4));
	}

	@Test
	public void testGetAllFiltersStringVerifyEntries() throws Exception {
		List<FilterRecord> filters = persistence.getAllFilters("other");

		assertThat(filters, hasItems(new FilterRecord(0, "other"), new FilterRecord(1, "other")));
	}

	@Test
	public void testGetAllFiltersStringVerifySize() throws Exception {
		List<FilterRecord> filters = persistence.getAllFilters("other");

		assertThat(filters, hasSize(2));
	}

	@Test
	public void testGetFilterReasonsVerifyEntries() throws Exception {
		List<String> reasons = persistence.getFilterReasons();

		assertThat(reasons, hasItems("other", "frogs", "animals"));
	}

	@Test
	public void testGetFilterReasonsVerifySize() throws Exception {
		List<String> reasons = persistence.getFilterReasons();

		assertThat(reasons, hasSize(3));
	}

	@Test
	public void testFilterByPathVerifyRecords() throws Exception {
		List<ImageRecord> records = persistence.filterByPath(Paths.get("foo"));

		assertThat(records, hasItems(new ImageRecord("foo", 0), new ImageRecord("foobar", 1)));
	}

	@Test
	public void testFilterByPathVerifySize() throws Exception {
		List<ImageRecord> records = persistence.filterByPath(Paths.get("foo"));

		assertThat(records, hasSize(2));
	}

	@Test
	public void testDistinctHashes() throws Exception {
		assertThat(persistence.distinctHashes(), is(4L));
	}
}
