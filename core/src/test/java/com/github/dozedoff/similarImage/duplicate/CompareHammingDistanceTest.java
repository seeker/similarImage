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
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;

public class CompareHammingDistanceTest {

	private static Long a, b, c, d;
	private static CompareHammingDistance chd;

	@BeforeClass
	public static void setUpBeforeClass() {
		a = 2L;
		b = 3L;
		c = 2L;
		d = 4L;

		chd = new CompareHammingDistance();
	}

	@Test
	public void testGetHammingDistance() throws Exception {
		assertThat(CompareHammingDistance.getHammingDistance(2L, 3L), is(1));
	}

	@Test
	public void testGetHammingDistance2() throws Exception {
		assertThat(CompareHammingDistance.getHammingDistance(2L, 4L), is(2));
	}

	@Test
	public void testGetHammingDistance3() throws Exception {
		assertThat(CompareHammingDistance.getHammingDistance(3L, 5L), is(2));
	}

	@Test
	public void testEvalDistanceAxiom1() throws Exception {
		assertThat(chd.eval(a, c), is(0.0));
		assertThat(a, is(c));
	}

	@Test
	public void testEvalDistanceAxiom2() throws Exception {
		double ab, ba;

		ab = chd.eval(a, b);
		ba = chd.eval(b, a);

		assertThat(ab, is(ba));
	}

	@Test
	public void testEvalDistanceAxiom3() throws Exception {
		double ad, ab, bd;

		ab = chd.eval(a, b);
		ad = chd.eval(a, d);
		bd = chd.eval(b, d);

		assertThat(ad, is(lessThanOrEqualTo(ab + bd)));
	}
}
