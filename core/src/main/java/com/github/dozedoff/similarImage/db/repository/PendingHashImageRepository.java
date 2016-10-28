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
package com.github.dozedoff.similarImage.db.repository;

import java.util.List;
import java.util.UUID;

import com.github.dozedoff.similarImage.db.PendingHashImage;

public interface PendingHashImageRepository {

	/**
	 * Store the {@link PendingHashImage} in the repository, duplicate paths are not allowed.
	 * 
	 * @param image
	 *            to store
	 * @return true if the entry was stored, false if it was a duplicate
	 * @throws RepositoryException
	 *             if there is an error accessing the datasource
	 */
	boolean store(PendingHashImage image) throws RepositoryException;

	/**
	 * Check if the given record exists
	 * 
	 * @param image
	 *            to look for
	 * @return true if the image is present
	 * @throws RepositoryException
	 *             if there is an error accessing the datasource
	 */
	boolean exists(PendingHashImage image) throws RepositoryException;

	/**
	 * Query a {@link PendingHashImage} by {@link UUID}
	 * 
	 * @param most
	 *            most significant bits
	 * @param least
	 *            least significant bits
	 * @return {@link PendingHashImage}, if found otherwise null.
	 * @throws RepositoryException
	 *             if there is an error accessing the datasource
	 */
	PendingHashImage getByUUID(long most, long least) throws RepositoryException;

	/**
	 * Remove a image from the datasource.
	 * 
	 * @param image
	 *            to remove
	 * @throws RepositoryException
	 *             if there is an error accessing the datasource
	 */
	void remove(PendingHashImage image) throws RepositoryException;

	/**
	 * Get all {@link PendingHashImage} entries
	 * 
	 * @return a {@link List} containing all entries
	 * @throws RepositoryException
	 *             if there is an error accessing the datasource
	 */
	List<PendingHashImage> getAll() throws RepositoryException;
}
