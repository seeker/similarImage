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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.db.ImageRecord;

import nl.jqno.equalsverifier.EqualsVerifier;

@RunWith(MockitoJUnitRunner.class)
public class ResultTest {
	private static final String PATH = "foo";
	private static final long HASH = 42;

	private ImageRecord imageRecord;

	@Mock
	private ResultGroup parentGroup;

	@Mock
	private ResultGroup parentGroup2;
	
	@Mock
	private GroupList parentGroupList;

	private Result cut;

	@Before
	public void setUp() throws Exception {
		imageRecord = new ImageRecord(PATH, HASH);
		cut = new Result(parentGroup, imageRecord);
	}

	@Test
	public void testGetImageRecord() throws Exception {
		assertThat(cut.getImageRecord(), is(imageRecord));
	}

	@Test
	public void testRemoveResult() throws Exception {
		cut.remove();

		verify(parentGroup).remove(cut);
	}

	@Test
	public void testHashAndEquals() throws Exception {
		EqualsVerifier.forClass(Result.class).withIgnoredFields("parentGroup")
		.withPrefabValues(ResultGroup.class, new ResultGroup(parentGroupList, 0, new LinkedList<ImageRecord>()), new ResultGroup(parentGroupList, 1, new LinkedList<ImageRecord>())).verify();
	}

	@Test
	public void testEqual() throws Exception {
		assertThat(cut, is(new Result(parentGroup2, new ImageRecord(PATH, HASH))));
	}
}
