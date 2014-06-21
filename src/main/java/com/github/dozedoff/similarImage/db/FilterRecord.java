/*  Copyright (C) 2013  Nicholas Wright
    
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
public class FilterRecord {
	@DatabaseField(id = true, canBeNull = false)
	private long pHash;
	@DatabaseField(canBeNull = false)
	private String reason;

	/**
	 * Intended for DAO only
	 */
	@Deprecated
	public FilterRecord() {
	}

	public FilterRecord(long pHash, String reason) {
		super();
		this.pHash = pHash;
		this.reason = reason;
	}

	public long getpHash() {
		return pHash;
	}

	public void setpHash(long pHash) {
		this.pHash = pHash;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof FilterRecord)) {
			return false;
		}

		FilterRecord fr = (FilterRecord) obj;

		return (this.pHash == fr.pHash) && (this.reason.equals(fr.getReason()));
	}
}
