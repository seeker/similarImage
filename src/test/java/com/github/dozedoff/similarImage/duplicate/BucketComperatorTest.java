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
package com.github.dozedoff.similarImage.duplicate;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.similarImage.db.ImageRecord;

public class BucketComperatorTest {
	BucketComperator bc;
	Bucket<Long, ImageRecord> a, b, c;

	@Before
	public void setUp() throws Exception {
		bc = new BucketComperator();

		a = new Bucket<Long, ImageRecord>(1L);
		b = new Bucket<Long, ImageRecord>(2L);
		c = new Bucket<Long, ImageRecord>(42L);
	}

	@Test
	public void testCompareEqual() throws Exception {
		assertThat(bc.compare(a, new Bucket<Long, ImageRecord>(1L)), is(0));
	}

	@Test
	public void testCompareLess() throws Exception {
		assertThat(bc.compare(a, b), is(-1));
	}

	@Test
	public void testCompareLessLargerNumber() throws Exception {
		assertThat(bc.compare(a, c), is(-1));
	}

	@Test
	public void testCompareGreater() throws Exception {
		assertThat(bc.compare(b, a), is(1));
	}

	@Test
	public void testCompareGreaterLargerNumber() throws Exception {
		assertThat(bc.compare(c, a), is(1));
	}

	@Test(expected = NullPointerException.class)
	public void testCompareFirstNull() throws Exception {
		bc.compare(null, b);
	}

	@Test(expected = NullPointerException.class)
	public void testCompareSecondNull() throws Exception {
		bc.compare(a, null);
	}

	@Test(expected = NullPointerException.class)
	public void testCompareBothNull() throws Exception {
		bc.compare(null, null);
	}
}
