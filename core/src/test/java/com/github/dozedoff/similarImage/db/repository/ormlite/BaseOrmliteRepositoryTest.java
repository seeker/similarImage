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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;

import com.github.dozedoff.similarImage.db.Database;
import com.github.dozedoff.similarImage.db.SQLiteDatabase;

public abstract class BaseOrmliteRepositoryTest {
	private Path path;
	protected Database db;

	@Before
	public void databaseSetup() throws IOException {
		path = Files.createTempFile("OrmliteRepositoryTest", ".db");
		db = new SQLiteDatabase(path);
	}

	@After
	public void databaseTearDown() throws IOException {
		db.close();
	}
}
