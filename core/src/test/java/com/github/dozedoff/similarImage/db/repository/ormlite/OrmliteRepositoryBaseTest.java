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

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;

public class OrmliteRepositoryBaseTest {

	private ConnectionSource connectionSource;
	private String databaseName = "OrmliteRepositoryTest";

	@Before
	public void baseSetUp() throws Exception {
		setUpDatabase(getDatabaseName());
	}

	@After
	public void baseTearDown() throws Exception {
		connectionSource.close();
	}

	private void setUpDatabase(String dbName) throws Exception {
		Path tempDbPath = Files.createTempFile(dbName, ".db");
		String fullDbPath = "jdbc:sqlite:" + tempDbPath.toString();
		this.connectionSource = new JdbcConnectionSource(fullDbPath);
	}

	/**
	 * Get the database {@link ConnectionSource} for this test.
	 * 
	 * @return {@link ConnectionSource} for the test
	 */
	protected ConnectionSource getConnectionSource() {
		return connectionSource;
	}

	/**
	 * Get the database name to be used for the test.
	 * 
	 * @return databaseName to use
	 */
	protected String getDatabaseName() {
		return this.databaseName;
	}
}
