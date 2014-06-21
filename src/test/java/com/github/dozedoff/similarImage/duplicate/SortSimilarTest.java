/*  Copyright (C) 2013  Nicholas Wright
    
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
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.similarImage.db.FilterRecord;
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

		assertThat(sort.getNumberOfGroups(), is(3));
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

	@Test
	public void testSortFilterHashOneGroupSize() {
		sort.sortFilter(1, "foo", testRecords, createFilterRecords("foo", 1L));
		List<Long> groups = sort.getDuplicateGroups();

		assertThat(groups, hasItem(1L));
		assertThat(groups.size(), is(1));
	}

	@Test
	public void testSortFilterHashOneRecords() {
		sort.sortFilter(1, "foo", testRecords, createFilterRecords("foo", 1L));

		assertGroupOneCorrect();
	}

	@Test
	public void testSortFilterHashTwoGroupSize() {
		sort.sortFilter(1, "foo", testRecords, createFilterRecords("foo", 2L));
		List<Long> groups = sort.getDuplicateGroups();

		assertThat(groups, hasItem(2L));
		assertThat(groups.size(), is(1));
	}

	@Test
	public void testSortFilterHashTwoRecords() {
		sort.sortFilter(1, "foo", testRecords, createFilterRecords("foo", 2L));

		assertGroupTwoCorrect();
	}

	@Test
	public void testSortFilterMultipleFiltersGroupSize() {
		sort.sortFilter(1, "foo", testRecords, createFilterRecords("foo", 1L, 2L));
		List<Long> groups = sort.getDuplicateGroups();

		assertThat(groups, hasItem(1L));
		assertThat(groups, hasItem(2L));
		assertThat(groups.size(), is(2));
	}

	@Test
	public void testSortFilterMultipleFiltersRecords() {
		sort.sortFilter(1, "foo", testRecords, createFilterRecords("foo", 1L, 2L));

		assertGroupOneCorrect();
		assertGroupTwoCorrect();
	}

	@Test
	public void testSortFilterHashZeroDistanceGroupSize() throws SQLException {
		List<FilterRecord> filterRecords = createFilterRecords("foo", 4L);
		when(mockPersistence.getAllFilters("foo")).thenReturn(filterRecords);

		sort.sortFilter(0, "foo", testRecords, filterRecords);
		List<Long> groups = sort.getDuplicateGroups();

		assertThat(groups, hasItem(4L));
		assertThat(groups.size(), is(1));
	}

	@Test
	public void testSortFilterHashZeroDistanceRecords() throws SQLException {
		List<FilterRecord> filterRecords = createFilterRecords("foo", 4L);
		when(mockPersistence.getAllFilters("foo")).thenReturn(filterRecords);

		sort.sortFilter(0, "foo", testRecords, filterRecords);

		Set<ImageRecord> group = sort.getGroup(4L);

		assertThat(group, hasItem(testRecords.get(1)));
		assertThat(group.size(), is(1));
	}

	@Test
	public void testSortFilterHashZeroDistanceSQLError() throws SQLException {
		List<FilterRecord> filterRecords = createFilterRecords("foo", 4L);
		when(mockPersistence.getAllFilters("foo")).thenThrow(new SQLException("This is a test"));

		sort.sortFilter(0, "foo", testRecords, filterRecords);
		List<Long> groups = sort.getDuplicateGroups();

		assertThat(groups.size(), is(0));
	}

	@Test
	public void testSortFilterHashZeroDistanceNoMatch() throws SQLException {
		List<FilterRecord> filterRecords = createFilterRecords("foo", 42L);
		when(mockPersistence.getAllFilters("foo")).thenReturn(filterRecords);

		sort.sortFilter(0, "foo", testRecords, filterRecords);
		List<Long> groups = sort.getDuplicateGroups();

		assertThat(groups.size(), is(0));
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
	public void testRemoveSingleImageGroups() throws SQLException {
		List<FilterRecord> filterRecords = createFilterRecords("foo", 4L, 3L);
		when(mockPersistence.getAllFilters("foo")).thenReturn(filterRecords);

		sort.sortFilter(0, "foo", testRecords, filterRecords);
		List<Long> groups = sort.getDuplicateGroups();
		assertThat(groups.size(), is(2));

		sort.removeSingleImageGroups();

		groups = sort.getDuplicateGroups();
		assertThat(groups.size(), is(1));
	}
}
