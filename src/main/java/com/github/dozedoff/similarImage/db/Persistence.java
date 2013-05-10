/*  Copyright (C) 2013  Nicholas Wright
    
    This file is part of similarImage - A similar image finder using pHash
    
    mmut is free software: you can redistribute it and/or modify
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
package com.github.dozedoff.similarImage.db;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.j256.ormlite.dao.CloseableWrappedIterable;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.table.TableUtils;

public class Persistence {
	private static final Logger logger = LoggerFactory.getLogger(Persistence.class);
	private static Persistence instance = null;
	private final String dbUrl = "jdbc:sqlite:similarImage.db";

	Dao<ImageRecord, String> imageRecordDao;
	Dao<FilterRecord, Long> filterRecordDao;

	private Persistence() {
		try {
			ConnectionSource cs = new JdbcConnectionSource(dbUrl);
			setupDatabase(cs);
			setupDAO(cs);
			long recordCount = imageRecordDao.countOf();
			long filterCount = filterRecordDao.countOf();
			logger.info("Loaded database with {} image and {} filter records", recordCount, filterCount);
		} catch (SQLException e) {
			logger.error("Failed to setup database {}", dbUrl, e);
			System.exit(1);
		}
	}
	
	public static synchronized Persistence getInstance() {
		if (instance == null) {
			instance = new Persistence();
		}

		return instance;
	}

	private void setupDatabase(ConnectionSource cs) throws SQLException {
		logger.info("Setting database config...");
		DatabaseConnection dbConn = cs.getReadWriteConnection();
		dbConn.executeStatement("PRAGMA page_size = 4096;", DatabaseConnection.DEFAULT_RESULT_FLAGS);
		dbConn.executeStatement("PRAGMA cache_size=10000;", DatabaseConnection.DEFAULT_RESULT_FLAGS);
		dbConn.executeStatement("PRAGMA locking_mode=EXCLUSIVE;", DatabaseConnection.DEFAULT_RESULT_FLAGS);
		dbConn.executeStatement("PRAGMA synchronous=NORMAL;", DatabaseConnection.DEFAULT_RESULT_FLAGS);
		dbConn.executeStatement("PRAGMA temp_store = MEMORY;", DatabaseConnection.DEFAULT_RESULT_FLAGS);
		dbConn.executeStatement("PRAGMA journal_mode=MEMORY;", DatabaseConnection.DEFAULT_RESULT_FLAGS);
		
		logger.info("Setting up database tables...");
		TableUtils.createTableIfNotExists(cs, ImageRecord.class);
		TableUtils.createTableIfNotExists(cs, FilterRecord.class);
	}

	private void setupDAO(ConnectionSource cs) throws SQLException {
		logger.info("Setting up DAO...");
		imageRecordDao = DaoManager.createDao(cs, ImageRecord.class);
		filterRecordDao = DaoManager.createDao(cs, FilterRecord.class);
	}

	public void addRecord(ImageRecord record) throws SQLException {
		imageRecordDao.createIfNotExists(record);
	}
	
	public void batchAddRecord(final List<ImageRecord> record) throws Exception {
		imageRecordDao.callBatchTasks(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				for(ImageRecord ir : record){
					imageRecordDao.createIfNotExists(ir);
				}
				return null;
			}
		});
	}
	
	public ImageRecord getRecord(Path path) throws SQLException {
		return imageRecordDao.queryForId(path.toString());
	}
	
	public List<ImageRecord> getRecords(long pHash) throws SQLException{
		ImageRecord searchRecord = new ImageRecord(null, pHash);
		return imageRecordDao.queryForMatching(searchRecord);
	}

	public void deleteRecord(ImageRecord record) throws SQLException {
		imageRecordDao.delete(record);
	}

	public boolean isPathRecorded(Path path) throws SQLException {
		String id = path.toString();
		ImageRecord record = imageRecordDao.queryForId(id);

		if (record == null) {
			return false;
		} else {
			return true;
		}
	}
	
	public CloseableWrappedIterable<ImageRecord> getImageRecordIterator() {
		return imageRecordDao.getWrappedIterable();
	}
	
	public List<ImageRecord> getAllRecords() throws SQLException {
		return imageRecordDao.queryForAll();
	}
	
	public void addFilter(FilterRecord filter) throws SQLException {
		filterRecordDao.createOrUpdate(filter);
	}

	public boolean filterExists(long pHash) throws SQLException {
		FilterRecord filter = filterRecordDao.queryForId(pHash);

		if (filter != null) {
			return true;
		} else {
			return false;
		}
	}
	
	public FilterRecord getFilter(long pHash) throws SQLException {
		return filterRecordDao.queryForId(pHash);
	}
	
	public List<FilterRecord> getAllFilters() throws SQLException {
		return filterRecordDao.queryForAll();
	}
	
	public List<String> getFilterReasons() {
		List<String> reasons = new LinkedList<String>();
		
		CloseableWrappedIterable<FilterRecord> iterator = filterRecordDao.getWrappedIterable();
		
		for(FilterRecord fr : iterator) {
			String reason = fr.getReason();
			
			if(!reasons.contains(reason)) {
				reasons.add(reason);
			}
		}
		
		try {
			iterator.close();
		} catch (SQLException e) {
			logger.warn("Failed to close iterator", e);
		}
		
		return reasons;
	}
	
	public List<ImageRecord> filterByPath(Path directory) throws SQLException {
		QueryBuilder<ImageRecord, String> qb = imageRecordDao.queryBuilder();
		PreparedQuery<ImageRecord> prep = qb.where().like("path", directory.toString() + "%").prepare();
		return imageRecordDao.query(prep);
	}
}
