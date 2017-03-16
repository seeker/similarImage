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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.similarImage.db.ImageRecord;

public class GroupListTest {
	private static final long HASH_A = 42;
	private static final long HASH_B = 43;

	private ImageRecord recordA;
	private ImageRecord recordB;
	private ImageRecord recordC;

	private ResultGroup groupA;
	private ResultGroup groupB;

	private Collection<ImageRecord> recordsA;
	private Collection<ImageRecord> recordsB;

	private ResultGroup[] groups;

	private GroupList cut;

	@Before
	public void setUp() throws Exception {
		cut = new GroupList();

		createRecords();
		createGroups();

		groups = new ResultGroup[] { groupA, groupB };

		cut.populateList(groups);
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
}
