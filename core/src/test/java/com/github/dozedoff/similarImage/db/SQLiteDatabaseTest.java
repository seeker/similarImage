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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.j256.ormlite.support.ConnectionSource;

public class SQLiteDatabaseTest {
	private static Path testDirectory;
	private Path databaseFile;
	private Path invalidPath;

	@BeforeClass
	public static void setUpClass() throws Exception {
		testDirectory = Files.createTempDirectory("SQLiteDatabaseTest");
	}

	@Before
	public void setUp() throws Exception {
		databaseFile = Files.createTempFile(testDirectory, "test", ".db");
		invalidPath = testDirectory.resolve("invalid").resolve("foo.db");

	}

	@Test
	public void testSQLiteDatabasePath() throws Exception {
		new SQLiteDatabase(databaseFile);
	}

	@Test(expected = RuntimeException.class)
	public void testSQLiteDatabaseInvalidPath() throws Exception {
		new SQLiteDatabase(invalidPath);
	}

	@Test
	public void testGetCs() throws Exception {
		assertThat(new SQLiteDatabase(databaseFile).getCs().isOpen(), is(true));

	}

	@Test
	public void testClose() throws Exception {
		Database db = new SQLiteDatabase(databaseFile);
		ConnectionSource cs = db.getCs();

		assertThat(cs.isOpen(), is(true)); // guard assert

		db.close();

		assertThat(cs.isOpen(), is(false));

	}
}
