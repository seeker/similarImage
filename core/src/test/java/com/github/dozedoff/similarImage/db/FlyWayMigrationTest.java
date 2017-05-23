/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.similarImage.db;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.Before;
import org.junit.Test;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;

public class FlyWayMigrationTest {
	private Flyway flyway;
	private Path databaseFile;
	private ConnectionSource cs;
	
	
	@Before
	public void setUp() throws Exception {
		databaseFile = Files.createTempFile(SQLiteDatabaseTest.class.getSimpleName(), ".db");
		String fulldbPath = "jdbc:sqlite:" + databaseFile;
		flyway = new Flyway();
		flyway.setDataSource(fulldbPath, "", "");

		cs = new JdbcConnectionSource(fulldbPath);
	}

	@Test
	public void testMigrationTo2_2_tags() throws Exception {
		flyway.setTargetAsString("2.2");
		flyway.migrate();

		Dao<Tag, Integer> dao = DaoManager.createDao(cs, Tag.class);
		List<Tag> tags = dao.queryForAll();

		assertThat(tags, hasItem(new Tag("foo", false)));
		assertThat(tags, hasSize(1));
	}

	@Test
	public void testMigrationTo2_2_filter() throws Exception {
		flyway.setTargetAsString("2.2");
		flyway.migrate();

		Dao<FilterRecord, Integer> filter = DaoManager.createDao(cs, FilterRecord.class);
		List<FilterRecord> tags = filter.queryForAll();

		assertThat(tags, hasSize(3));
	}
}
