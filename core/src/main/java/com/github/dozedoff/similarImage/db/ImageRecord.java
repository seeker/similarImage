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
package com.github.dozedoff.similarImage.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
public final class ImageRecord implements Comparable<ImageRecord> {
	public static final String PATH_COLUMN_NAME = "path";

	@DatabaseField(id = true, canBeNull = false, columnName = PATH_COLUMN_NAME)
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (pHash ^ (pHash >>> 32));
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ImageRecord other = (ImageRecord) obj;
		if (pHash != other.pHash)
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
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

	/**
	 * Display the class name and data in a human readable form.
	 * 
	 * @return Class information and state
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("ImageRecord");
		sb.append("{");
		sb.append("path:");
		sb.append(path);
		sb.append(",");
		sb.append("hash:");
		sb.append(pHash);
		sb.append("}");

		return sb.toString();
	}
}
