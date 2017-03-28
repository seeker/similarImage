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

import com.github.dozedoff.similarImage.db.Tag;

public interface TagRepository extends Repository {
	/**
	 * Get the tag with the matching name.
	 * 
	 * @param name
	 *            of the {@link Tag} to get from the datasource
	 * @return the tag or null if it is not found
	 * @throws RepositoryException
	 *             if there is an error accessing the datasource
	 */
	Tag getByName(String name) throws RepositoryException;

	/**
	 * Store a tag in the repository. If the tag already exists, it will be updated.
	 * 
	 * @param tag
	 *            to store
	 * @throws RepositoryException
	 *             if there is an error accessing the datasource
	 */
	void store(Tag tag) throws RepositoryException;

	/**
	 * Remove a tag from the repository. If the tag already exists, it will be updated.
	 * 
	 * @param tag
	 *            to store
	 * @throws RepositoryException
	 *             if there is an error accessing the datasource
	 */
	void remove(Tag tag) throws RepositoryException;

	/**
	 * Get all stored {@link Tag}s.
	 * 
	 * @return A list of all stored {@link Tag}s
	 * @throws RepositoryException
	 *             if there is an error accessing the datasource
	 */
	List<Tag> getAll() throws RepositoryException;

	/**
	 * Get all {@link Tag}s that are set to be shown in the context menu.
	 * 
	 * @return A list of stored {@link Tag}s with the context flag set
	 * @throws RepositoryException
	 *             if there is an error accessing the datasource
	 */
	List<Tag> getWithContext() throws RepositoryException;
}
