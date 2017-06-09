/*  Copyright (C) 2017  Nicholas Wright
    
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

import java.nio.file.Path;
import java.util.List;

import com.github.dozedoff.similarImage.db.IgnoreRecord;

/**
 * Interface for repositories that handle ignored images.
 * 
 * @author Nicholas Wright
 *
 */
public interface IgnoreRepository extends Repository {

	/**
	 * Store the record in the repository, duplicate entries are ignored.
	 * 
	 * @param toStore
	 *            the {@link IgnoreRecord} to persist
	 * @throws RepositoryException
	 *             if there is an error accessing the repository
	 */
	void store(IgnoreRecord toStore) throws RepositoryException;

	/**
	 * Remove the record from the repository.
	 * 
	 * @param toRemove
	 *            the {@link IgnoreRecord} to remove
	 * @throws RepositoryException
	 *             if there is an error accessing the repository
	 */
	void remove(IgnoreRecord toRemove) throws RepositoryException;

	/**
	 * Query the repository for the given path.
	 * 
	 * @param path
	 *            to query
	 * @return the path if found, otherwise null
	 * @throws RepositoryException
	 *             if there is an error accessing the repository
	 */
	IgnoreRecord findByPath(Path path) throws RepositoryException;

	/**
	 * Query the repository for the given path.
	 * 
	 * @param path
	 *            to query
	 * @return the path if found, otherwise null
	 * @throws RepositoryException
	 *             if there is an error accessing the repository
	 */
	IgnoreRecord findByPath(String path) throws RepositoryException;

	/**
	 * Query the repository if the path is ignored.
	 * 
	 * @param path
	 *            to query
	 * @return true if the path is ignored, otherwise false
	 * @throws RepositoryException
	 *             if there is an error accessing the repository
	 */
	boolean isPathIgnored(String path) throws RepositoryException;

	/**
	 * Query the repository if the path is ignored.
	 * 
	 * @param path
	 *            to query
	 * @return true if the path is ignored, otherwise false
	 * @throws RepositoryException
	 *             if there is an error accessing the repository
	 */
	boolean isPathIgnored(Path path) throws RepositoryException;

	/**
	 * Get all ignored images.
	 * 
	 * @return a list of ignored images
	 * @throws RepositoryException
	 *             if there is an error accessing the repository
	 */
	List<IgnoreRecord> getAll() throws RepositoryException;
}
