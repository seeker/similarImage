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

import java.util.Arrays;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Database entry for a thumbnail image. Contains the image data and a hash to uniquely identify the original image.
 *
 */
@DatabaseTable
public class Thumbnail {
	public static final int THUMBNAIL_SIZE = 100;
	@DatabaseField(generatedId = true)
	private int id;
	@DatabaseField(dataType = DataType.BYTE_ARRAY, canBeNull = false, index = true, unique = true)
	private byte[] uniqueHash;
	@DatabaseField(dataType = DataType.BYTE_ARRAY, canBeNull = false)
	private byte[] imageData;

	/**
	 * Intended for DAO only
	 * 
	 * @deprecated Use the constructor with arguments instead
	 */
	@Deprecated
	public Thumbnail() {
	}

	/**
	 * Create a new record for a thumbnail image.
	 * 
	 * @param uniqueHash
	 *            for the original image
	 * @param imageData
	 *            of the thumbnail
	 */
	public Thumbnail(byte[] uniqueHash, byte[] imageData) {
		this.uniqueHash = uniqueHash;
		this.imageData = imageData;
	}

	/**
	 * Hash that uniquely identifies the original image
	 * 
	 * @return hash value
	 */
	public byte[] getUniqueHash() {
		return uniqueHash;
	}

	/**
	 * Image data for the thumbnail.
	 * 
	 * @return raw data
	 */
	public byte[] getImageData() {
		return imageData;
	}

	/**
	 * Hashcode for the thumbnail. Hash value and image data are used, the id is ignored.
	 * 
	 * @return computed hashcode
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(imageData);
		result = prime * result + Arrays.hashCode(uniqueHash);
		return result;
	}

	/**
	 * Checks if two objects are equal. Only Hash value and image data are compared, the id is ignored.
	 * 
	 * @param obj
	 *            object to compare
	 * @return true, if objects are equal
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
		Thumbnail other = (Thumbnail) obj;
		if (!Arrays.equals(imageData, other.imageData)) {
			return false;
		}
		if (!Arrays.equals(uniqueHash, other.uniqueHash)) {
			return false;
		}
		return true;
	}
}
