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
package com.github.dozedoff.similarImage.learning;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Comparator;
import java.util.Set;

import org.everpeace.search.BKTree;
import org.junit.BeforeClass;
import org.junit.Test;

public class BKTreeLearning {
	private static final int SAMPLE_COUNT = 11;
	private static final int NUMBER_OF_CHILDREN = 9;
	private static final int LAST_INSERT = SAMPLE_COUNT - 1;

	private static BKTree<Integer> bkTree;
	private static int last = -1;

	@BeforeClass
	public static void setUpBeforeClass() {
		bkTree = new BKTree<Integer>(new Comparator<Integer>() {

			@Override
			public int compare(Integer o1, Integer o2) {
				return Math.abs(01 - o2);
			}
		}, 0);

		for (int i = 0; i < SAMPLE_COUNT; i++) {
			bkTree.insert(i);
			last = i;
		}
	}

	@Test
	public void childrenTest() {
		assertThat(bkTree.numOfChildren(), is(NUMBER_OF_CHILDREN));
	}

	@Test
	public void lastInsert() {
		assertThat(last, is(LAST_INSERT));
	}

	@Test
	public void existsTest() {
		Set<Integer> results = bkTree.searchWithin(2, 1d);

		assertThat(results.contains(2), is(true));
	}
}
