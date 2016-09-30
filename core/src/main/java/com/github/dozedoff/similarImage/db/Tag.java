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
	public static final String NAME_FIELD_NAME = "name";
	
	/**
	 * This field is for DAO internal use only.
	 */
	@DatabaseField(generatedId = true)
	private long userTagId;
	@DatabaseField(canBeNull = false, unique = true, columnName=NAME_FIELD_NAME)
	private String tag;

	@DatabaseField(canBeNull = false)
	private boolean contextMenu;

	/**
	 * @deprecated For DAO use only.
	 */
	@Deprecated
	public Tag() {
	}

	/**
	 * Create a new tag which will not be displayed in the context menu.
	 * 
	 * @param tag
	 *            name of the tag
	 */
	public Tag(String tag) {
		this.tag = tag;
		this.contextMenu = false;
	}

	/**
	 * Create a new tag which can be displayed in the context menu.
	 * 
	 * @param tag
	 *            name of the tag
	 * 
	 * @param contextMenu
	 *            true if the tag should be displayed in the context menu
	 */
	public Tag(String tag, boolean contextMenu) {
		this.tag = tag;
		this.contextMenu = contextMenu;
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
	 * Get whether the tag shows up in the context menu.
	 * 
	 * @return true if the tag is shown in the context menu
	 */
	public final boolean isContextMenu() {
		return contextMenu;
	}

	/**
	 * Set whether the tag shows up in the context menu.
	 * 
	 * @param contextMenu
	 *            true if the tag should be shown in the context menu
	 */
	public final void setContextMenu(boolean contextMenu) {
		this.contextMenu = contextMenu;
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

	/**
	 * Create hashcode using tag and contextMenu flag.
	 * 
	 * @return generated hashcode
	 */
	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (contextMenu ? 1231 : 1237);
		result = prime * result + ((tag == null) ? 0 : tag.hashCode());
		return result;
	}

	/**
	 * Compare class to this one. Only tag and contextMenu flag are used for comparison, the id is ignored.
	 * 
	 * @param obj
	 *            object to compare
	 * @return true if the object is identical
	 */
	@Override
	public final boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Tag)) {
			return false;
		}
		Tag other = (Tag) obj;
		if (contextMenu != other.contextMenu) {
			return false;
		}
		if (tag == null) {
			if (other.tag != null) {
				return false;
			}
		} else if (!tag.equals(other.tag)) {
			return false;
		}
		return true;
	}
}
