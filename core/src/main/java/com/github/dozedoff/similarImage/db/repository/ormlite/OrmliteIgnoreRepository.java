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
package com.github.dozedoff.similarImage.db.repository.ormlite;

import java.nio.file.Path;
import java.sql.SQLException;

import com.github.dozedoff.similarImage.db.IgnoreRecord;
import com.github.dozedoff.similarImage.db.repository.IgnoreRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.SelectArg;

/**
 * Ignore repository using ORMlite DAOs to access databases.
 * 
 * @author Nicholas Wright
 *
 */
public class OrmliteIgnoreRepository implements IgnoreRepository {
	private static final String PATH_COLUMN = "imagePath";

	private final Dao<IgnoreRecord, String> ignoreDao;

	private final PreparedQuery<IgnoreRecord> pathQuery;
	private final SelectArg pathForQuery;
	
	/**
	 * Create a new {@link OrmliteIgnoreRepository} that can be use to access the database.
	 * 
	 * @param ignoreDao
	 *            dao for the ignored record table
	 * @throws RepositoryException
	 *             if the prepared query setup fails
	 */
	public OrmliteIgnoreRepository(Dao<IgnoreRecord, String> ignoreDao) throws RepositoryException {
		this.ignoreDao = ignoreDao;
		pathForQuery = new SelectArg();
		
		try {
			this.pathQuery = this.ignoreDao.queryBuilder().where().eq(PATH_COLUMN, pathForQuery).prepare();
		} catch (SQLException e) {
			throw new RepositoryException("Failed to setup prepared query", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void store(IgnoreRecord toStore) throws RepositoryException {
		try {
			ignoreDao.createIfNotExists(toStore);
		} catch (SQLException e) {
			throw new RepositoryException("Failed to store ignore", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void remove(IgnoreRecord toRemove) throws RepositoryException {
		try {
			ignoreDao.delete(toRemove);
		} catch (SQLException e) {
			throw new RepositoryException("Failed to delete ignore", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IgnoreRecord findByPath(Path path) throws RepositoryException {
		return findByPath(path.toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IgnoreRecord findByPath(String path) throws RepositoryException {
		pathForQuery.setValue(path);

		try {
			return ignoreDao.queryForFirst(pathQuery);
		} catch (SQLException e) {
			throw new RepositoryException("Failed to query for path", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isPathIgnored(String path) throws RepositoryException {
		return findByPath(path) != null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isPathIgnored(Path path) throws RepositoryException {
		return isPathIgnored(path.toString());
	}
}
