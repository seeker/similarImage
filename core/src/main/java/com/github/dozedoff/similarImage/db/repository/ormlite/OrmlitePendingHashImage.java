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

import java.sql.SQLException;
import java.util.List;

import com.github.dozedoff.similarImage.db.PendingHashImage;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.j256.ormlite.dao.Dao;

/**
 * Repository for accessing {@link PendingHashImage} database table via ORMlite.
 * 
 * @author Nicholas Wright
 *
 */
public class OrmlitePendingHashImage implements PendingHashImageRepository {
	private final Dao<PendingHashImage, Integer> pendingDao;

	/**
	 * Create a repository using ORMlite DAO to access the database.
	 * 
	 * @param pendingDao
	 *            for the {@link PendingHashImage} table
	 * @throws RepositoryException
	 *             if there is an error setting up prepared queries
	 */
	public OrmlitePendingHashImage(Dao<PendingHashImage, Integer> pendingDao) throws RepositoryException {
		this.pendingDao = pendingDao;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void store(PendingHashImage image) throws RepositoryException {
		try {
			pendingDao.create(image);
		} catch (SQLException e) {
			throw new RepositoryException("Failed to store entry", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean exists(PendingHashImage image) throws RepositoryException {
		try {
			if (pendingDao.refresh(image) == 0) {
				return !pendingDao.queryForMatching(image).isEmpty();
			} else {
				return true;
			}
		} catch (SQLException e) {
			throw new RepositoryException("Failed to lookup entry", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeById(int id) throws RepositoryException {
		try {
			pendingDao.deleteById(id);
		} catch (SQLException e) {
			throw new RepositoryException("Failed to delete entry", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PendingHashImage getById(int id) throws RepositoryException {
		try {
			return pendingDao.queryForId(id);
		} catch (SQLException e) {
			throw new RepositoryException("Failed to get entry by id", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<PendingHashImage> getAll() throws RepositoryException {
		try {
			return pendingDao.queryForAll();
		} catch (SQLException e) {
			throw new RepositoryException("Failed to query for all entries", e);
		}
	}
}
