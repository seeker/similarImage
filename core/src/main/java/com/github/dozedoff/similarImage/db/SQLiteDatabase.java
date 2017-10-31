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
package com.github.dozedoff.similarImage.db;

import java.nio.file.Path;
import java.sql.SQLException;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;

/**
 * Create/Open and configure a SQLite database
 * 
 * @author Nicholas Wright
 */
public class SQLiteDatabase implements Database {
	private static final Logger LOGGER = LoggerFactory.getLogger(SQLiteDatabase.class);
	private final static String DEFAULT_DB_PATH = "similarImage.db";
	private final static String DB_PREFIX = "jdbc:sqlite:";

	private final ConnectionSource connectionSource;

	/**
	 * Create or open a SQLite database in the working directory with the name {@value SQLiteDatabase#DEFAULT_DB_PATH}.
	 */
	public SQLiteDatabase() {
		this(DEFAULT_DB_PATH);
	}

	/**
	 * Create or open a SQLite database at the given path.
	 * 
	 * @param dbPath
	 *            path to the database file
	 */
	public SQLiteDatabase(Path dbPath) {
		this(dbPath.toString());
	}

	/**
	 * Create or open a SQLite database at the given path.
	 * 
	 * @param dbPath
	 *            path to the database file
	 */
	public SQLiteDatabase(String dbPath) {
		try {
			String fullDbPath = DB_PREFIX + dbPath;
			connectionSource = new JdbcConnectionSource(fullDbPath);
			migrateDatabase(fullDbPath);
			setupDatabase(connectionSource);

			LOGGER.info("Loaded database");
		} catch (SQLException e) {
			LOGGER.error("Failed to setup database {}", dbPath, e);
			throw new RuntimeException("Failed to setup database" + dbPath);
		}
	}

	private void setupDatabase(ConnectionSource cs) throws SQLException {
		LOGGER.info("Setting database config...");
		DatabaseConnection dbConn = cs.getReadWriteConnection();
		// FIXME Tests fail if journal_mode PRAGMA is set
		dbConn.executeStatement("PRAGMA page_size = 4096;", DatabaseConnection.DEFAULT_RESULT_FLAGS);
		dbConn.executeStatement("PRAGMA cache_size=10000;", DatabaseConnection.DEFAULT_RESULT_FLAGS);
		dbConn.executeStatement("PRAGMA locking_mode=EXCLUSIVE;", DatabaseConnection.DEFAULT_RESULT_FLAGS);
		dbConn.executeStatement("PRAGMA synchronous=NORMAL;", DatabaseConnection.DEFAULT_RESULT_FLAGS);
		dbConn.executeStatement("PRAGMA temp_store = MEMORY;", DatabaseConnection.DEFAULT_RESULT_FLAGS);
	}

	private void migrateDatabase(String fullDbPath) {
		Flyway flyway = new Flyway();
		flyway.setDataSource(fullDbPath, "", "");
		flyway.migrate();
	}

	/**
	 * Get a connection source for the SQLite database.
	 * 
	 * @return {@link ConnectionSource} for the database
	 */
	@Override
	public ConnectionSource getCs() {
		return connectionSource;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() {
		connectionSource.closeQuietly();
	}
}
