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
public final class FilterRecord {
	@DatabaseField(generatedId = true)
	private int id;
	@DatabaseField(canBeNull = false, index = true, uniqueCombo = true)
	private long pHash;
	@DatabaseField(canBeNull = false, index = true, uniqueCombo = true)
	private String tag;
	@DatabaseField(foreign = true, foreignAutoRefresh = true)
	private Thumbnail thumbnail;

	/**
	 * Intended for DAO only
	 * 
	 * @deprecated Use the constructor with arguments instead
	 */
	@Deprecated
	public FilterRecord() {
	}

	/**
	 * Create a new {@link FilterRecord} without a {@link Thumbnail}.
	 * 
	 * @param hash
	 *            the similarity hash for the image
	 * @param tag
	 *            for the image
	 */
	public FilterRecord(long hash, String tag) {
		this.pHash = hash;
		this.tag = tag;
		this.thumbnail = null;
	}

	/**
	 * Create a new {@link FilterRecord} with a {@link Thumbnail}.
	 * 
	 * @param pHash
	 *            the similarity hash for the image
	 * @param tag
	 *            for the image
	 * @param thumbnail
	 *            thumbnail image for the record, can be null
	 */
	public FilterRecord(long pHash, String tag, Thumbnail thumbnail) {
		super();
		this.pHash = pHash;
		this.tag = tag;
		this.thumbnail = thumbnail;
	}

	public long getpHash() {
		return pHash;
	}

	public void setpHash(long pHash) {
		this.pHash = pHash;
	}

	/**
	 * @deprecated Replaced by {@link FilterRecord#getTag()}
	 * @return The reason for this filter
	 */
	@Deprecated
	public String getReason() {
		return tag;
	}

	public String getTag() {
		return tag;
	}

	/**
	 * @deprecated Replaced by {@link FilterRecord#setTag(String)}
	 * @param reason
	 *            for this filter
	 */
	@Deprecated
	public void setReason(String reason) {
		this.tag = reason;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public Thumbnail getThumbnail() {
		return thumbnail;
	}

	public final void setThumbnail(Thumbnail thumbnail) {
		this.thumbnail = thumbnail;
	}

	/**
	 * Checks if a thumbnail is set for this {@link FilterRecord}.
	 * 
	 * @return true if a thumbnail is set
	 */
	public boolean hasThumbnail() {
		return thumbnail != null;
	}

	/**
	 * Create a hashcode using hash, tag and thumbnail
	 * 
	 * @return generated hashcode
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (pHash ^ (pHash >>> 32));
		result = prime * result + ((tag == null) ? 0 : tag.hashCode());
		result = prime * result + ((thumbnail == null) ? 0 : thumbnail.hashCode());
		return result;
	}

	/**
	 * Compare an object to check if it is equal. Hash, Tag and thumbnail will be used for comparison.
	 * 
	 * @param obj
	 *            object to compare
	 * @return true if the object is identical
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		FilterRecord other = (FilterRecord) obj;
		if (pHash != other.pHash) {
			return false;
		}
		if (tag == null) {
			if (other.tag != null) {
				return false;
			}
		} else if (!tag.equals(other.tag)) {
			return false;
		}
		if (thumbnail == null) {
			if (other.thumbnail != null) {
				return false;
			}
		} else if (!thumbnail.equals(other.thumbnail)) {
			return false;
		}
		return true;
	}
}
