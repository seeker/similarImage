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

/**
 * Tag defined by the user, used to group {@link FilterRecord}.
 * 
 * @author Nicholas Wright
 *
 */
public class Tag {
	/**
	 * This field is for DAO internal use only.
	 */
	@DatabaseField(generatedId = true)
	private long userTagId;
	@DatabaseField(canBeNull = false)
	private String tag;

	/**
	 * @deprecated For DAO use only.
	 */
	@Deprecated
	public Tag() {
	}

	/**
	 * Create a new tag.
	 * 
	 * @param tag
	 *            name of the tag
	 */
	public Tag(String tag) {
		this.tag = tag;
	}

	/**
	 * Get the tag.
	 * 
	 * @return tag
	 */
	public final String getTag() {
		return tag;
	}

	/**
	 * Set the tag.
	 * 
	 * @param tag
	 *            to set
	 */
	public final void setTag(String tag) {
		this.tag = tag;
	}

	/**
	 * Uses the tag as the display string.
	 * 
	 * @return the tag represented by this class
	 */
	@Override
	public String toString() {
		return this.tag;
	}
}
