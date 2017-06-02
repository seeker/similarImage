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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.similarImage.db.IgnoreRecord;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;

public class OrmliteImageRepositoryTest extends OrmliteRepositoryBaseTest {
	private Dao<ImageRecord, String> imageDao;
	private Dao<IgnoreRecord, String> ignoreDao;

	private static final long HASH_EXISTING_RECORD = 1;
	private static final long HASH_NEW_RECORD = 2;

	private String pathExisting;
	private String pathNew;

	private ImageRecord imageExisting;
	private ImageRecord imageNew;

	private OrmliteImageRepository cut;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getDatabaseName() {
		return this.getClass().getCanonicalName();
	}

	@Before
	public void setUp() throws Exception {
		imageDao = DaoManager.createDao(getConnectionSource(), ImageRecord.class);
		ignoreDao = DaoManager.createDao(getConnectionSource(), IgnoreRecord.class);

		TableUtils.createTable(getConnectionSource(), ImageRecord.class);
		TableUtils.createTable(getConnectionSource(), IgnoreRecord.class);

		pathExisting = "existing";
		pathNew = "new";

		imageExisting = new ImageRecord(pathExisting, HASH_EXISTING_RECORD);
		imageNew = new ImageRecord(pathNew, HASH_NEW_RECORD);

		imageDao.create(imageExisting);
		ignoreDao.create(new IgnoreRecord(imageExisting));

		cut = new OrmliteImageRepository(imageDao, ignoreDao);
	}

	@Test
	public void testStore() throws Exception {
		cut.store(imageNew);

		assertThat(imageDao.queryForId(pathNew), is(imageNew));
	}

	@Test
	public void testGetByHashExists() throws Exception {
		assertThat(cut.getByHash(HASH_EXISTING_RECORD), containsInAnyOrder(imageExisting));
	}

	@Test
	public void testGetByHashNotFound() throws Exception {
		assertThat(cut.getByHash(HASH_NEW_RECORD), hasSize(0));
	}

	@Test
	public void testGetByPathExists() throws Exception {
		assertThat(cut.getByPath(Paths.get(pathExisting)), is(imageExisting));
	}

	@Test
	public void testGetByPathNotFound() throws Exception {
		assertThat(cut.getByPath(Paths.get(pathNew)), is(nullValue()));
	}

	@Test
	public void testStartsWithPath() throws Exception {
		assertThat(cut.startsWithPath(Paths.get("exi")), containsInAnyOrder(imageExisting));
	}

	@Test
	public void testRemoveImageRecord() throws Exception {
		cut.remove(imageExisting);

		assertThat(imageDao.queryForMatching(imageExisting), hasSize(0));
	}

	@Test
	public void testRemoveImageRecordCollection() throws Exception {
		imageDao.create(imageNew);

		List<ImageRecord> toRemove = new LinkedList<ImageRecord>();
		toRemove.add(imageExisting);
		toRemove.add(imageNew);

		assertThat(imageDao.queryForAll(), hasSize(2)); // guard assert

		cut.remove(toRemove);

		assertThat(imageDao.queryForMatching(imageExisting), hasSize(0));
	}

	@Test
	public void testGetAll() throws Exception {
		imageDao.create(imageNew);

		assertThat(cut.getAll(), containsInAnyOrder(imageExisting, imageNew));
	}

	@Test
	public void testGetAllWithoutIgnored() throws Exception {
		imageDao.create(imageNew);

		List<ImageRecord> result = cut.getAllWithoutIgnored();

		assertThat(result, hasItem(imageNew));
		assertThat(result, hasSize(1));
	}
}
