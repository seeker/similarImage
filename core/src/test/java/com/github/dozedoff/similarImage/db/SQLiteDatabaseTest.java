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
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.j256.ormlite.support.ConnectionSource;

public class SQLiteDatabaseTest {
	private static final String EMPTY_STRING = "";

	private Path databaseFile;
	private SQLiteDatabase cut;

	@Before
	public void setUp() throws Exception {
		databaseFile = Files.createTempFile(SQLiteDatabaseTest.class.getSimpleName(), ".db");
		cut = new SQLiteDatabase(databaseFile);
	}

	@After
	public void tearDown() throws Exception {
		if (cut != null) {
			cut.close();
			cut = null;
		}
	}

	@Test(expected = RuntimeException.class)
	public void testSQLiteDatabaseInvalidPath() throws Exception {
		new SQLiteDatabase(databaseFile.resolve("invalid"));
	}

	@Test
	public void testGetCs() throws Exception {
		assertThat(cut.getCs().isOpen(EMPTY_STRING), is(true));

	}

	@Ignore("Closing a closed connection when using pooled connections results in NPE")
	@Test
	public void testClose() throws Exception {
		ConnectionSource cs = cut.getCs();

		assertThat(cs.isOpen(EMPTY_STRING), is(true)); // guard assert

		cut.close();

		assertThat(cs.isOpen(EMPTY_STRING), is(false));
	}
}
