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

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.SelectArg;

public class OrmliteImageRepository implements ImageRepository {
	private final Dao<ImageRecord, String> imageDao;

	private PreparedQuery<ImageRecord> queryStartsWithPath;
	private SelectArg argStartsWithPath;

	/**
	 * Create a repository using ORMlite DAO to access the database.
	 * 
	 * @param imageDao
	 *            for the image table
	 * @throws RepositoryException
	 *             if there is an error setting up prepared queries
	 */
	public OrmliteImageRepository(Dao<ImageRecord, String> imageDao) throws RepositoryException {
		this.imageDao = imageDao;
		
		argStartsWithPath = new SelectArg();

		try {
			queryStartsWithPath = imageDao.queryBuilder().where().like(ImageRecord.PATH_COLUMN_NAME, argStartsWithPath)
					.prepare();
		} catch (SQLException e) {
			throw new RepositoryException("Failed to setup prepared statements", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void store(ImageRecord image) throws RepositoryException {
		try {
			imageDao.createOrUpdate(image);
		} catch (SQLException e) {
			throw new RepositoryException("Failed to store image", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ImageRecord> getByHash(long hash) throws RepositoryException {
		try {
			return imageDao.queryForMatching(new ImageRecord(null, hash));
		} catch (SQLException e) {
			throw new RepositoryException("Failed to query by hash", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ImageRecord getByPath(Path path) throws RepositoryException {
		try {
			return imageDao.queryForId(path.toString());
		} catch (SQLException e) {
			throw new RepositoryException("Failed to query for path", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ImageRecord> startsWithPath(Path directory) throws RepositoryException {
		argStartsWithPath.setValue(directory.toString() + "%");

		try {
			return imageDao.query(queryStartsWithPath);
		} catch (SQLException e) {
			throw new RepositoryException("Failed to query for starts with path", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void remove(ImageRecord image) throws RepositoryException {
		try {
			imageDao.delete(image);
		} catch (SQLException e) {
			throw new RepositoryException("Failed to remove image", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void remove(Collection<ImageRecord> images) throws RepositoryException {
		try {
			imageDao.delete(images);
		} catch (SQLException e) {
			throw new RepositoryException("Failed to remove some or all images", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ImageRecord> getAll() throws RepositoryException {
		try {
			return imageDao.queryForAll();
		} catch (SQLException e) {
			throw new RepositoryException("Failed to query all", e);
		}
	}
}
