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
package com.github.dozedoff.similarImage.db.repository.ormlite;

import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.db.repository.TagRepository;

/**
 * Factory for creating Repositories.
 * 
 * @author Nicholas Wright
 *
 */
public interface RepositoryFactory {

	/**
	 * Create a new {@link FilterRepository}
	 * 
	 * @return an initialized {@link FilterRepository}
	 * @throws RepositoryException
	 *             if there was an error with the datasource
	 */
	FilterRepository buildFilterRepository() throws RepositoryException;

	/**
	 * Create a new {@link ImageRepository}
	 * 
	 * @return an initialized {@link ImageRepository}
	 * @throws RepositoryException
	 *             if there was an error with the datasource
	 */
	ImageRepository buildImageRepository() throws RepositoryException;

	/**
	 * Create a new {@link TagRepository}
	 * 
	 * @return an initialized {@link TagRepository}
	 * @throws RepositoryException
	 *             if there was an error with the datasource
	 */
	TagRepository buildTagRepository() throws RepositoryException;

	/**
	 * Create a new {@link PendingHashImageRepository}
	 * 
	 * @return an initialized {@link PendingHashImageRepository}
	 * @throws RepositoryException
	 *             if there was an error with the datasource
	 */
	PendingHashImageRepository buildPendingHashImageRepository() throws RepositoryException;

}
