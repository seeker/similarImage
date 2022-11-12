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

import org.junit.Rule;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.quality.Strictness;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.DefaultListModel;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.github.dozedoff.similarImage.db.ImageRecord;

public class GroupListTest {
	public @Rule MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

	private static final long HASH_A = 42;
	private static final long HASH_B = 43;

	private ImageRecord recordA;
	private ImageRecord recordB;
	private ImageRecord recordC;

	private ResultGroup groupA;
	private ResultGroup groupB;

	@Mock
	private ResultGroup groupUnknown;

	private Collection<ImageRecord> recordsA;
	private Collection<ImageRecord> recordsB;

	private GroupList cut;

	private DefaultListModel<ResultGroup> dlm;

	@Before
	public void setUp() throws Exception {
		cut = new GroupList();

		initGroupList();

		dlm = new DefaultListModel<ResultGroup>();
		dlm.addElement(groupA);
		dlm.addElement(groupB);
	}

	private void initGroupList() {
		createRecords();
		createGroups();

		cut.populateList(Arrays.asList(new ResultGroup[] { groupA, groupB }));
	}

	private void createGroups(){
		groupA = new ResultGroup(cut, HASH_A, recordsA);
		groupB = new ResultGroup(cut, HASH_B, recordsB);
	}

	private void createRecords() {
		recordA = new ImageRecord("", 0);
		recordB = new ImageRecord("", 1);
		recordC = new ImageRecord("", 2);

		recordsA = new LinkedList<ImageRecord>();
		recordsB = new LinkedList<ImageRecord>();

		recordsA.add(recordA);
		recordsA.add(recordC);

		recordsB.add(recordB);
		recordsB.add(recordC);
	}

	@Test
	public void testRemoveViaGroup() throws Exception {
		Result result = new Result(groupB, recordC);
		assertThat(groupB.getResults(), hasItem(result)); // guard assert

		groupA.remove(result);

		assertThat(groupB.getResults(), not(hasItem(result)));
	}

	@Test
	public void testGroupCount() throws Exception {
		assertThat(cut.groupCount(), is(2));
	}

	@Test
	public void testEmptyGroupsRemoved() throws Exception {
		groupA.remove(new Result(groupA, recordC));
		groupA.remove(new Result(groupA, recordA));

		assertThat(groupA.hasResults(), is(false));

		assertThat(cut.groupCount(), is(1));
	}

	@Test
	public void testGetGroup() throws Exception {
		assertThat(cut.getGroup(HASH_A), is(groupA));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetInvalidHash() throws Exception {
		cut.getGroup(-1);
	}

	@Test
	public void testListModelContainsGroups() throws Exception {
		assertThat(dlm.contains(groupA), is(true));
		assertThat(dlm.contains(groupB), is(true));
	}

	@Test
	public void testEmptyGroupRemovedFromGui() throws Exception {
		cut = new GroupList(dlm);
		initGroupList();
		
		groupA.remove(new Result(groupA, recordC));
		groupA.remove(new Result(groupA, recordA));

		assertThat(dlm.contains(groupA), is(false));
	}

	@Test
	public void testPreviousGroup() throws Exception {
		assertThat(cut.previousGroup(groupB), is(groupA));
	}

	@Test
	public void testPreviousGroupWithFirstGroup() throws Exception {
		assertThat(cut.previousGroup(groupA), is(groupA));
	}

	@Test
	public void testPreviousGroupWithUnknownGroup() throws Exception {
		assertThat(cut.previousGroup(groupUnknown), is(groupA));
	}

	@Test
	public void testNextGroup() throws Exception {
		assertThat(cut.nextGroup(groupA), is(groupB));
	}

	@Test
	public void testNextGroupWithLastGroup() throws Exception {
		assertThat(cut.nextGroup(groupB), is(groupB));
	}

	@Test
	public void testNextGroupWithUnknownGroup() throws Exception {
		assertThat(cut.nextGroup(groupUnknown), is(groupA));
	}
}
