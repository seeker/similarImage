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

import com.github.dozedoff.similarImage.db.PendingHashImage;

public interface PendingHashImageRepository {

	/**
	 * Store the {@link PendingHashImage} in the repository, duplicate paths are not allowed.
	 * 
	 * @param image
	 *            to store
	 * @throws RepositoryException
	 *             if there is an error accessing the datasource, or a duplicate was stored
	 */
	void store(PendingHashImage image) throws RepositoryException;

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
	 * Remove an image via id.
	 * 
	 * @param id
	 *            of the image to remove
	 * @throws RepositoryException
	 *             if there is an error accessing the datasource
	 */
	void removeById(int id) throws RepositoryException;

	/**
	 * Query a {@link PendingHashImage} by id.
	 * 
	 * @param id
	 *            to query
	 * @return {@link PendingHashImage}, if found otherwise null.
	 * @throws RepositoryException
	 *             if there is an error accessing the datasource
	 */
	PendingHashImage getById(int id) throws RepositoryException;

	/**
	 * Get all {@link PendingHashImage} entries
	 * 
	 * @return a {@link List} containing all entries
	 * @throws RepositoryException
	 *             if there is an error accessing the datasource
	 */
	List<PendingHashImage> getAll() throws RepositoryException;
}
