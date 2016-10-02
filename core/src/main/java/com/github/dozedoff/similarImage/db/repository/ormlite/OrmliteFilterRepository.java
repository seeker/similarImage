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

import com.github.dozedoff.similarImage.db.FilterRecord;
import com.github.dozedoff.similarImage.db.Tag;
import com.github.dozedoff.similarImage.db.Thumbnail;
import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.SelectArg;

/**
 * Filter repository using ORMlite DAOs to access databases.
 * 
 * @author Nicholas Wright
 *
 */
public class OrmliteFilterRepository implements FilterRepository {
	private static final String THUMB_HASH_COLUMN_NAME = "uniqueHash";

	private static final String STORE_FILTER_ERROR_MSG = "Failed to store Filter";

	private Dao<FilterRecord, Integer> filterDao;
	private Dao<Thumbnail, Integer> thumbnailDao;

	private PreparedQuery<Thumbnail> thumbnailHashQuery;
	private SelectArg thumbnailHashQueryArg;

	/**
	 * Create a repository using ORMlite DAO to access the database.
	 * 
	 * @param filterDao
	 *            dao for the filter table
	 * @param thumbnailDao
	 *            dao for the thumbnail table
	 * @throws SQLException
	 *             if the prepared query setup fails
	 */
	public OrmliteFilterRepository(Dao<FilterRecord, Integer> filterDao, Dao<Thumbnail, Integer> thumbnailDao)
			throws SQLException {
		this.filterDao = filterDao;
		this.thumbnailDao = thumbnailDao;
		
		thumbnailHashQueryArg = new SelectArg();
		thumbnailHashQuery = thumbnailDao.queryBuilder().where().eq(THUMB_HASH_COLUMN_NAME, thumbnailHashQueryArg)
				.prepare();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<FilterRecord> getByHash(long hash) throws RepositoryException {
		try {
			return filterDao.queryForMatching(new FilterRecord(hash, null, null));
		} catch (SQLException e) {
			throw new RepositoryException("Failed to query by hash", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<FilterRecord> getByTag(Tag tag) throws RepositoryException {
		try {
			return filterDao.queryForMatchingArgs(new FilterRecord(0, tag, null));
		} catch (SQLException e) {
			throw new RepositoryException("Failed to query by tag", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<FilterRecord> getAll() throws RepositoryException {
		try {
			return filterDao.queryForAll();
		} catch (SQLException e) {
			throw new RepositoryException("Failed to get all filters", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void store(FilterRecord toStore) throws RepositoryException {
		if (toStore.hasThumbnail()) {
			checkAndCreateThumbnail(toStore);
		}

		try {
			if (filterDao.queryForMatchingArgs(toStore).isEmpty()) {
				filterDao.create(toStore);
			}
		} catch (SQLException e) {
			throw new RepositoryException(STORE_FILTER_ERROR_MSG, e);
		}
	}

	private void checkAndCreateThumbnail(FilterRecord toStore) throws RepositoryException {
		Thumbnail thumbnail = toStore.getThumbnail();

		try {
			if (thumbnailDao.refresh(thumbnail) == 0) {
				thumbnailHashQueryArg.setValue(thumbnail.getUniqueHash());

				Thumbnail existingThumbnail = thumbnailDao.queryForFirst(thumbnailHashQuery);

				if (existingThumbnail == null) {
					thumbnailDao.create(thumbnail);
				} else {
					toStore.setThumbnail(existingThumbnail);
				}
			}
		} catch (SQLException e) {
			throw new RepositoryException(STORE_FILTER_ERROR_MSG, e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void remove(FilterRecord filter) throws RepositoryException {
		try {
			filterDao.delete(filter);
		} catch (SQLException e) {
			throw new RepositoryException("Failed to remove Filter", e);
		}
	}
}
