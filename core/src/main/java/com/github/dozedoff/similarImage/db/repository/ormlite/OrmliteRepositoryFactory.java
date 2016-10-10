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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.BadFileRecord;
import com.github.dozedoff.similarImage.db.Database;
import com.github.dozedoff.similarImage.db.FilterRecord;
import com.github.dozedoff.similarImage.db.IgnoreRecord;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Tag;
import com.github.dozedoff.similarImage.db.Thumbnail;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.LruObjectCache;
import com.j256.ormlite.support.ConnectionSource;

/**
 * Creates OrmLite based repositories using a database.
 * 
 * @author Nicholas Wright
 */
public class OrmliteRepositoryFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(OrmliteRepositoryFactory.class);
	
	private static final int LARGE_CACHE_SIZE = 5000;
	private static final int DEFAULT_CACHE_SIZE = 5000;

	private Dao<ImageRecord, String> imageRecordDao;
	private Dao<FilterRecord, Integer> filterRecordDao;
	private Dao<BadFileRecord, String> badFileRecordDao;
	private Dao<IgnoreRecord, Long> ignoreRecordDao;
	private Dao<Thumbnail, Integer> thumbnailDao;
	private Dao<Tag, Long> tagDao;

	/**
	 * Create a new Repository Factory using the given database instance.
	 * 
	 * @param database
	 *            to use for creating repositories
	 */
	public OrmliteRepositoryFactory(Database database) {
		try {
			setupDAO(database.getCs());
		} catch (SQLException e) {
			throw new RuntimeException("Failed to setup DAO: " + e.toString(), e);
		}
	}

	private void setupDAO(ConnectionSource cs) throws SQLException {
		LOGGER.info("Setting up DAO...");

		imageRecordDao = DaoManager.createDao(cs, ImageRecord.class);
		filterRecordDao = DaoManager.createDao(cs, FilterRecord.class);
		badFileRecordDao = DaoManager.createDao(cs, BadFileRecord.class);
		ignoreRecordDao = DaoManager.createDao(cs, IgnoreRecord.class);
		thumbnailDao = DaoManager.createDao(cs, Thumbnail.class);
		tagDao = DaoManager.createDao(cs, Tag.class);

		imageRecordDao.setObjectCache(new LruObjectCache(LARGE_CACHE_SIZE));
		filterRecordDao.setObjectCache(new LruObjectCache(DEFAULT_CACHE_SIZE));
		badFileRecordDao.setObjectCache(new LruObjectCache(DEFAULT_CACHE_SIZE));
		ignoreRecordDao.setObjectCache(new LruObjectCache(DEFAULT_CACHE_SIZE));
	}

	/**
	 * Create a new {@link OrmliteFilterRepository}
	 * 
	 * @return an initialized {@link OrmliteFilterRepository}
	 * @throws RepositoryException
	 *             if there was an error with the DAO or database
	 */
	public OrmliteFilterRepository buildFilterRepository() throws RepositoryException {
		try {
			return new OrmliteFilterRepository(filterRecordDao, thumbnailDao);
		} catch (SQLException e) {
			throw new RepositoryException("Failed to create filter repository", e);
		}
	}

	/**
	 * Create a new {@link OrmliteImageRepository}
	 * 
	 * @return an initialized {@link OrmliteImageRepository}
	 * @throws RepositoryException
	 *             if there was an error with the DAO or database
	 */
	public OrmliteImageRepository buildImageRepository() throws RepositoryException {
		return new OrmliteImageRepository(imageRecordDao);
	}

	/**
	 * Create a new {@link OrmliteTagRepository}
	 * 
	 * @return an initialized {@link OrmliteTagRepository}
	 * @throws RepositoryException
	 *             if there was an error with the DAO or database
	 */
	public OrmliteTagRepository buildTagRepository() throws RepositoryException {
		return new OrmliteTagRepository(tagDao);
	}
}
