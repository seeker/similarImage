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

import java.util.Objects;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.MoreObjects;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Repository record for ignoring images.
 * 
 * @author Nicholas Wright
 *
 */
@Immutable
@DatabaseTable
public final class IgnoreRecord {
	public static final String IMAGEPATH_FIELD_NAME = "path";
	
	@DatabaseField(generatedId = true, canBeNull = false)
	private int id;

	@DatabaseField(canBeNull = false, foreign = true, columnName = IMAGEPATH_FIELD_NAME)
	private ImageRecord image;

	/**
	 * Intended for DAO only
	 * 
	 * @deprecated DAO only
	 */
	@Deprecated
	public IgnoreRecord() {
	}

	/**
	 * Create a new {@link IgnoreRecord} for the given path.
	 * 
	 * @param image
	 *            to ignore
	 */
	public IgnoreRecord(ImageRecord image) {
		this.image = image;
	}

	/**
	 * Get the ignored image.
	 * 
	 * @return image
	 */
	public ImageRecord getImage() {
		return image;
	}

	/**
	 * Compare if the objects are equal.
	 * 
	 * @param obj
	 *            instance to compare
	 * @return true if the object is of the type {@link IgnoreRecord} and the image path matches.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IgnoreRecord) {
			IgnoreRecord other = (IgnoreRecord) obj;

			return Objects.equals(this.image, other.image);
		}

		return false;
	}

	/**
	 * Get the hashcode for this object. Based on the image path.
	 * 
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return Objects.hash(image);
	}

	/**
	 * Returns information about the ignored {@link ImageRecord}.
	 * 
	 * @return image field formatted as a string
	 */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(IgnoreRecord.class).add("image", image).toString();
	}
}
