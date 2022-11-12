/*  Copyright (C) 2017  Nicholas Wright
    
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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.Before;
import org.junit.Test;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;

public class FlyWayMigrationTest {
	private static final String PATH_1 = "path1";
	private static final String PATH_2 = "path2";
	private static final String PATH_3 = "path3";

	private static final String VERSION_2_1 = "2.1";

	private Flyway flyway;
	private FluentConfiguration flywayConfig;
	private Path databaseFile;
	private ConnectionSource cs;
	
	@Before
	public void setUp() throws Exception {
		databaseFile = Files.createTempFile(SQLiteDatabaseTest.class.getSimpleName(), ".db");
		String fulldbPath = "jdbc:sqlite:" + databaseFile;
		flywayConfig = Flyway.configure().dataSource(fulldbPath, "", "").target(VERSION_2_1);
		flyway = flywayConfig.load();
		
		cs = new JdbcConnectionSource(fulldbPath);
	}

	@Test
	public void testMigrationTo2v2tags() throws Exception {
		flyway.migrate();

		Dao<Tag, Integer> dao = DaoManager.createDao(cs, Tag.class);
		List<Tag> tags = dao.queryForAll();

		assertThat(tags, hasItem(new Tag("foo", false)));
		assertThat(tags, hasSize(1));
	}

	@Test
	public void testMigrationTo2v2filter() throws Exception {
		flyway.migrate();

		Dao<Tag, Integer> tag = DaoManager.createDao(cs, Tag.class);
		Dao<FilterRecord, Integer> filter = DaoManager.createDao(cs, FilterRecord.class);
		List<FilterRecord> tags = filter.queryForAll();

		Tag testTag = tag.queryForId(1);
		assertThat(tags, hasSize(3));
		assertThat(tags,
				hasItems(new FilterRecord(1, testTag), new FilterRecord(2, testTag), new FilterRecord(3, testTag)));
	}

	@Test
	public void testMigrationTo3v0() throws Exception
	{
		flywayConfig.target("3.0").load().migrate();

		Dao<ImageRecord, String> image = DaoManager.createDao(cs, ImageRecord.class);
		Dao<IgnoreRecord, String> ignore = DaoManager.createDao(cs, IgnoreRecord.class);
		List<IgnoreRecord> ignored = ignore.queryForAll();

		ignored.stream().forEach(record -> {
			try {
				image.refresh(record.getImage());
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});

		assertThat(ignored, hasItems(new IgnoreRecord(new ImageRecord(PATH_1, 1)),
				new IgnoreRecord(new ImageRecord(PATH_2, 1)), new IgnoreRecord(new ImageRecord(PATH_3, 2))));
	}
}
