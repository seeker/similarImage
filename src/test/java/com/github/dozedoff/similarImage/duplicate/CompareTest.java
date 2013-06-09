/*  Copyright (C) 2013  Nicholas Wright
    
    This file is part of similarImage - A similar image finder using pHash
    
    mmut is free software: you can redistribute it and/or modify
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

import org.junit.Test;

public class CompareTest {
	@Test
	public void testGetHammingDistanceZero() {
		int distance = CompareHammingDistance.getHammingDistance(0, 0);
		assertThat(distance, is(0));
	}

	public void testGetHammingDistanceMax() {
		int distance = CompareHammingDistance.getHammingDistance(Long.MIN_VALUE, Long.MIN_VALUE);
		assertThat(distance, is(0));
	}

	public void testGetHammingDistance() {
		int distance = CompareHammingDistance.getHammingDistance(5, 6);
		assertThat(distance, is(2));
	}
}
