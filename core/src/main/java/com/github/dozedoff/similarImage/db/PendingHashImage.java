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

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Stores paths and identity of images that have been resized and are pending hashing.
 * 
 * @author Nicholas Wright
 *
 */
@DatabaseTable
public class PendingHashImage {
	@DatabaseField(generatedId=true)
	private int id;
	
	@DatabaseField(unique = true)
	private String path;

	/**
	 * Intended for DAO only
	 * 
	 * @deprecated Use the constructor with arguments instead
	 */
	@Deprecated
	public PendingHashImage() {
	}

	/**
	 * Create a new {@link PendingHashImage} record with the given path.
	 * 
	 * @param path
	 *            to set
	 */
	public PendingHashImage(String path) {
		this.path = path;
	}

	/**
	 * Create a new {@link PendingHashImage} record with the given path.
	 * 
	 * @param path
	 *            to set
	 */
	public PendingHashImage(Path path) {
		this.path = path.toString();
	}

	/**
	 * Get the stored path.
	 * 
	 * @return stored path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Get the stored path.
	 * 
	 * @return stored path
	 */
	public Path getPathAsPath() {
		return Paths.get(path);
	}

	/**
	 * Get the id of this entry. This is also the the tracking ID of the message.
	 * 
	 * @return the id of this entry
	 */
	public int getId() {
		return id;
	}

	/**
	 * Check if the instance has been stored in the repository and was assigned a unique id.
	 * 
	 * @return true if a unique id was assigned
	 */
	public boolean isIdValid() {
		return id != 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof PendingHashImage)) {
			return false;
		}
		PendingHashImage other = (PendingHashImage) obj;
		if (path == null) {
			if (other.path != null) {
				return false;
			}
		} else if (!path.equals(other.path)) {
			return false;
		}
		return true;
	}
}
