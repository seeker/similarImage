/*  Copyright (C) 2017  Nicholas Wright
    
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
package com.github.dozedoff.similarImage.result;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Collection;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.db.ImageRecord;

@RunWith(MockitoJUnitRunner.class)
public class ResultGroupTest {
	private static final long HASH = 42;
	private static final String PATH_A = "foo";
	private static final String PATH_B = "bar";

	private Collection<ImageRecord> records;
	private ImageRecord recordA;
	private ImageRecord recordB;

	private Result resultA;

	@Mock
	private GroupList parent;

	private ResultGroup cut;

	@Before
	public void setUp() throws Exception {
		buildRecords();

		cut = new ResultGroup(parent, HASH, records);

		resultA = new Result(cut, recordA);
	}

	private void buildRecords() {
		records = new LinkedList<ImageRecord>();

		recordA = new ImageRecord(PATH_A, HASH);
		recordB = new ImageRecord(PATH_B, HASH);

		records.add(recordA);
		records.add(recordB);
	}

	@Test
	public void testGetHash() throws Exception {
		assertThat(cut.getHash(), is(HASH));
	}

	@Test
	public void testGetResults() throws Exception {
		assertThat(cut.getResults(), hasItems(resultA, new Result(cut, recordB)));
	}

	@Test
	public void testRemove() throws Exception {
		assertThat(cut.remove(resultA), is(true));
	}

	@Test
	public void testRemoveNonExistingResult() throws Exception {
		cut.remove(resultA);

		assertThat(cut.remove(resultA), is(false));
	}

	@Test
	public void testNotPresentAfterRemove() throws Exception {
		assertThat(cut.getResults(), hasItem(resultA)); // guard assert

		cut.remove(resultA);

		assertThat(cut.getResults(), not(hasItem(resultA)));
	}

	@Test
	public void testNotifyParentOnResultRemoval() throws Exception {
		cut.remove(resultA);

		verify(parent).remove(resultA);
	}

	@Test
	public void testRemoveWithoutNotification() throws Exception {
		cut.remove(resultA, false);

		verify(parent, never()).remove(resultA);
	}

	@Test
	public void testHasResults() throws Exception {
		assertThat(cut.hasResults(), is(true));
	}

	@Test
	public void testHasResultsWhenEmpty() throws Exception {
		cut.getResults().clear();

		assertThat(cut.hasResults(), is(false));
	}

	@Test
	public void testToString() throws Exception {
		assertThat(cut.toString(), is("42 (2)"));
	}
}
