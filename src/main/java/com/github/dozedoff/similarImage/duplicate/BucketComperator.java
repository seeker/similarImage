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

import java.io.Serializable;
import java.util.Comparator;

import com.github.dozedoff.similarImage.db.ImageRecord;

public class BucketComperator implements Comparator<Bucket<Long, ImageRecord>>, Serializable {
	private static final long serialVersionUID = 5900233220884625603L;

	// TODO null should be treated smaller as a record
	@Override
	public int compare(Bucket<Long, ImageRecord> o1, Bucket<Long, ImageRecord> o2) {
		long l1 = o1.getId();
		long l2 = o2.getId();

		if (l1 < l2) {
			return -1;
		} else if (l1 == l2) {
			return 0;
		} else {
			return 1;
		}
	}
}