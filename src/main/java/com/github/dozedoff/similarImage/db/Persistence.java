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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.table.TableUtils;

public class Persistence {
	private static final Logger logger = LoggerFactory.getLogger(Persistence.class);
	private static Persistence instance = null;
	private final String dbUrl = "jdbc:sqlite:similarImage.db";

	Dao<ImageRecord, String> imageRecordDao;

	private Persistence() {
		try {
			ConnectionSource cs = new JdbcConnectionSource(dbUrl);
			setupDatabase(cs);
			setupDAO(cs);
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
	}

	private void setupDAO(ConnectionSource cs) throws SQLException {
		logger.info("Setting up DAO...");
		imageRecordDao = DaoManager.createDao(cs, ImageRecord.class);
	}

	public void addRecord(ImageRecord record) throws SQLException {
		imageRecordDao.createIfNotExists(record);
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
}
