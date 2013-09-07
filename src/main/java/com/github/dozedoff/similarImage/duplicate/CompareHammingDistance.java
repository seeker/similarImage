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

import org.everpeace.search.Distance;

import com.github.dozedoff.similarImage.db.ImageRecord;

public class CompareHammingDistance implements Distance<Bucket<Long, ImageRecord>> {
	protected static int getHammingDistance(long a, long b) {
		long xor = a ^ b;
		int distance = Long.bitCount(xor);
		return distance;
	}

	@Override
	public double eval(Bucket<Long, ImageRecord> e1, Bucket<Long, ImageRecord> e2) {
		long hashE1 = e1.getId();
		long hashE2 = e2.getId();
		int distance = getHammingDistance(hashE1, hashE2);
		return distance;
	}
}
