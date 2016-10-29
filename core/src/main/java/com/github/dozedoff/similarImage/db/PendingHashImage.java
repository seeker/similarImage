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
import java.util.UUID;

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
	public static final String MOST_SIGN_COL_NAME = "most";
	public static final String LEAST_SIGN_COL_NAME = "least";

	@DatabaseField(generatedId=true)
	private int id;
	
	@DatabaseField(unique = true, index = true)
	private String path;

	@DatabaseField(uniqueCombo = true, columnName = MOST_SIGN_COL_NAME, index = true)
	private long mostSignificant;

	@DatabaseField(uniqueCombo = true, columnName = LEAST_SIGN_COL_NAME, index = true)
	private long leastSignificant;

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
	 *            to track
	 * @param most
	 *            most significant bits of the {@link UUID}
	 * @param least
	 *            least significant bits of the {@link UUID}
	 */
	public PendingHashImage(String path, long most, long least) {
		this.path = path;
		this.mostSignificant = most;
		this.leastSignificant = least;
	}

	/**
	 * Create a new {@link PendingHashImage} record with the given path and UUID.
	 * 
	 * @param path
	 *            to track
	 * @param uuid
	 *            {@link UUID} that corresponds to the path
	 */
	public PendingHashImage(String path, UUID uuid) {
		this.path = path;
		this.mostSignificant = uuid.getMostSignificantBits();
		this.leastSignificant = uuid.getLeastSignificantBits();
	}

	/**
	 * Create a new {@link PendingHashImage} record with the given path and UUID.
	 * 
	 * @param path
	 *            to track
	 * @param uuid
	 *            {@link UUID} that corresponds to the path
	 */
	public PendingHashImage(Path path, UUID uuid) {
		this(path.toString(), uuid);
	}

	/**
	 * Convenience method that builds a {@link UUID} from the bits.
	 * 
	 * @return the {@link UUID} representing the path
	 */
	public UUID getUuid() {
		return new UUID(mostSignificant, leastSignificant);
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
	 * Get the most significant bits of the {@link UUID}
	 * 
	 * @return most significant bits of the {@link UUID}
	 */
	public long getMostSignificant() {
		return mostSignificant;
	}

	/**
	 * Get the least significant bits of the {@link UUID}
	 * 
	 * @return least significant bits of the {@link UUID}
	 */
	public long getLeastSignificant() {
		return leastSignificant;
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
