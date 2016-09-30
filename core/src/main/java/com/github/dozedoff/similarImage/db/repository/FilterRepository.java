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

import com.github.dozedoff.similarImage.db.FilterRecord;
import com.github.dozedoff.similarImage.db.Thumbnail;

/**
 * Repository for retrieving and storing {@link FilterRecord} and the associated {@link Thumbnail} (if any).
 * 
 * @author Nicholas Wright
 */
public interface FilterRepository {
	/**
	 * Get a list of {@link FilterRecord} that match the given hash.
	 * 
	 * @param hash
	 *            to search for
	 * @return a list of found filters
	 * @throws RepositoryException
	 *             if the repository encounters an error processing the request
	 */
	List<FilterRecord> getByHash(long hash) throws RepositoryException;

	/**
	 * Get a list of {@link FilterRecord} that match the given tag.
	 * 
	 * @param tag
	 *            to search for
	 * @return a list of found filters
	 * @throws RepositoryException
	 *             if the repository encounters an error processing the request
	 */
	List<FilterRecord> getByTag(String tag) throws RepositoryException;

	/**
	 * Get a list of all {@link FilterRecord}
	 * 
	 * @return a list of all filters
	 * @throws RepositoryException
	 *             if the repository encounters an error processing the request
	 */
	List<FilterRecord> getAll() throws RepositoryException;

	/**
	 * Persist a {@link FilterRecord}.
	 * 
	 * @param toStore
	 *            the {@link FilterRecord} to persist
	 * @throws RepositoryException
	 *             if the repository encounters an error processing the request
	 */
	void store(FilterRecord toStore) throws RepositoryException;
}
