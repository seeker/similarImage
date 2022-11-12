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
package com.github.dozedoff.similarImage.thread;

import org.junit.Rule;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.DefaultListModel;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.github.dozedoff.similarImage.result.GroupList;
import com.github.dozedoff.similarImage.result.ResultGroup;
import com.google.common.collect.Lists;

public class GroupListPopulatorTest {
	public @Rule MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

	GroupListPopulator glp;
	private static final int GROUP_COUNT = 5;
	private GroupList grouplist;
	private List<ResultGroup> results;

	@Mock
	DefaultListModel<ResultGroup> dlm;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		grouplist = new GroupList();
		results = new LinkedList<ResultGroup>();

		for (int i = GROUP_COUNT + 1; i > 0; i--) {
			results.add(new ResultGroup(grouplist, i, Collections.emptyList()));
		}

		grouplist.populateList(results);

		glp = new GroupListPopulator(grouplist, dlm);
	}

	@Test
	public void testElementsAddedInOrder() {
		glp.run();

		List<ResultGroup> testList = Lists.reverse(results);
		InOrder inOrder = inOrder(dlm);

		for (ResultGroup rg : testList) {
			inOrder.verify(dlm).addElement(rg);
		}
	}

	@Test
	public void testElementsAdded() {
		glp.run();

		for (ResultGroup rg : results) {
			verify(dlm).addElement(rg);
		}
	}
}
