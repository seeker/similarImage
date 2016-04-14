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

public class ImageRecordComperator implements Comparator<ImageRecord>, Serializable {
	private static final long serialVersionUID = -407477449233333356L;

	// TODO null should be treated smaller as a record
	@Override
	public int compare(ImageRecord o1, ImageRecord o2) {
		long l1 = o1.getpHash();
		long l2 = o2.getpHash();

		if (l1 < l2) {
			return -1;
		} else if (l1 == l2) {
			return 0;
		} else {
			return 1;
		}
	}
}
