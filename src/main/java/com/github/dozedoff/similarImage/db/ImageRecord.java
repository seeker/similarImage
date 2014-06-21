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
package com.github.dozedoff.similarImage.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
public class ImageRecord implements Comparable<ImageRecord> {
	@DatabaseField(id = true, canBeNull = false)
	String path;
	@DatabaseField(canBeNull = false)
	long pHash;

	/**
	 * Intended for DAO
	 */
	@Deprecated
	public ImageRecord() {
	}

	public ImageRecord(String path, long pHash) {
		this.path = path;
		this.pHash = pHash;
	}

	public String getPath() {
		return path;
	}

	public long getpHash() {
		return pHash;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ImageRecord)) {
			return false;
		}

		ImageRecord toComapre = (ImageRecord) obj;

		if ((this.pHash == toComapre.getpHash()) && samePath(toComapre)) {
			return true;
		} else {
			return false;
		}
	}

	private boolean samePath(ImageRecord rec) {
		return this.getPath().equals(rec.getPath());
	}

	@Override
	public int compareTo(ImageRecord o) {
		if (o == null) {
			throw new NullPointerException();
		}

		if (this.pHash == o.getpHash()) {
			return 0;
		} else if (this.getpHash() > o.pHash) {
			return 1;
		} else {
			return -1;
		}
	}
}
