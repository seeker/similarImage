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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import javax.annotation.concurrent.Immutable;

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
	@DatabaseField(id = true, canBeNull = false)
	private String imagePath;

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
	 * @param path
	 *            to ignore
	 */
	public IgnoreRecord(String path) {
		this.imagePath = path;
	}

	/**
	 * Create a new {@link IgnoreRecord} for the given path.
	 * 
	 * @param path
	 *            to ignore
	 */
	public IgnoreRecord(Path path) {
		this(path.toString());
	}

	/**
	 * Get the path for the ignored image.
	 * 
	 * @return the image path
	 */
	public Path getImagePath() {
		return Paths.get(imagePath);
	}

	/**
	 * Get the path for the ignored image.
	 * 
	 * @return the image path
	 */
	public String getImagePathAsString() {
		return imagePath;
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

			return Objects.equals(this.imagePath, other.imagePath);
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
		return Objects.hash(imagePath);
	}
}
