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

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import com.github.dozedoff.similarImage.db.ImageRecord;

public interface ImageRepository extends Repository {
	/**
	 * Store a {@link ImageRecord} in the repository. If it already exists, it
	 * will be updated.
	 * 
	 * @param image
	 *            to store
	 */
	void store(ImageRecord image) throws RepositoryException;

	/**
	 * Return all {@link ImageRecord} that match the given hash.
	 * 
	 * @param hash
	 *            to search for
	 * @return a list containing {@link ImageRecord} with given hash
	 * @throws RepositoryException
	 *             if there is a error accessing the datasource
	 */
	List<ImageRecord> getByHash(long hash) throws RepositoryException;

	/**
	 * Get the {@link ImageRecord} that exactly matches the path, if any.
	 * 
	 * @param path
	 *            to search for
	 * @return the matching {@link ImageRecord} or null if there is no match
	 * @throws RepositoryException
	 *             if there is a error accessing the datasource
	 */
	ImageRecord getByPath(Path path) throws RepositoryException;

	/**
	 * Get all {@link ImageRecord} that start with the given path.
	 * 
	 * @param directory
	 *            path that the paths should start with
	 * @return {@link ImageRecord} that start with the given path
	 * @throws RepositoryException
	 *             if there is a error accessing the datasource
	 */
	List<ImageRecord> startsWithPath(Path directory) throws RepositoryException;

	/**
	 * Remove the {@link ImageRecord} from the datasource.
	 * 
	 * @param image
	 *            to remove
	 * @throws RepositoryException
	 *             if there is a error accessing the datasource
	 */
	void remove(ImageRecord image) throws RepositoryException;

	/**
	 * Remove all {@link ImageRecord} in the {@link Collection} from the
	 * datasource.
	 * 
	 * @param images
	 *            to remove
	 * @throws RepositoryException
	 *             if there is a error accessing the datasource
	 */
	void remove(Collection<ImageRecord> images) throws RepositoryException;

	/**
	 * Get all {@link ImageRecord} stored in the datasource
	 * 
	 * @return all found {@link ImageRecord}
	 * @throws RepositoryException
	 *             if there is a error accessing the datasource
	 */
	List<ImageRecord> getAll() throws RepositoryException;

	/**
	 * Get all {@link ImageRecord} stored in the datasource who are not ignored.
	 * 
	 * @return all non-ignored {@link ImageRecord}
	 * @throws RepositoryException
	 *             if there is a error accessing the datasource
	 */
	List<ImageRecord> getAllWithoutIgnored() throws RepositoryException;

	/**
	 * Get all {@link ImageRecord} stored in the datasource who are not ignored.
	 * 
	 * @param directory
	 *            only include images from the directory and it's sub-directories
	 * @return all non-ignored {@link ImageRecord}
	 * @throws RepositoryException
	 *             if there is a error accessing the datasource
	 */
	List<ImageRecord> getAllWithoutIgnored(Path directory) throws RepositoryException;
}
