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
package com.github.dozedoff.similarImage.db.repository.ormlite;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.similarImage.db.IgnoreRecord;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public class OrmliteIgnoreRepositoryTest extends OrmliteRepositoryBaseTest {
	private OrmliteIgnoreRepository cut;
	private Dao<IgnoreRecord, String> dao;

	private static final String PATH_A = "foo";
	private static final String PATH_B = "bar";

	private IgnoreRecord existingIgnore;
	private IgnoreRecord newIgnore;

	@Before
	public void setUp() throws Exception {
		ConnectionSource cs = getConnectionSource();
		
		TableUtils.createTable(cs, IgnoreRecord.class);

		dao = DaoManager.createDao(cs, IgnoreRecord.class);
		cut = new OrmliteIgnoreRepository(dao);

		existingIgnore = new IgnoreRecord(new ImageRecord(PATH_A, 0));
		newIgnore = new IgnoreRecord(new ImageRecord(PATH_B, 0));

		dao.create(existingIgnore);
	}

	private Path toPath(String path) {
		return Paths.get(path);
	}

	@Test
	public void testStoreNew() throws Exception {
		cut.store(newIgnore);

		assertThat(dao.queryForAll(), hasItem(newIgnore));
	}
	
	@Test
	public void testStoreDuplicate() throws Exception {
		cut.store(existingIgnore);

		assertThat(dao.queryForAll(), hasSize(1));
	}

	@Test
	public void testStoreNewSize() throws Exception {
		cut.store(newIgnore);

		assertThat(dao.queryForAll(), hasSize(2));
	}

	@Test
	public void testRemove() throws Exception {
		cut.remove(existingIgnore);

		assertThat(dao.queryForAll(), not(hasItem(existingIgnore)));
	}

	@Test
	public void testRemoveSize() throws Exception {
		cut.remove(existingIgnore);

		assertThat(dao.queryForAll(), is(empty()));
	}

	@Test
	public void testFindByPathPath() throws Exception {
		assertThat(cut.findByPath(toPath(PATH_A)), is(existingIgnore));
	}

	@Test
	public void testFindByPathPathNotFound() throws Exception {
		assertThat(cut.findByPath(toPath(PATH_B)), is(nullValue()));
	}

	@Test
	public void testFindByPathString() throws Exception {
		assertThat(cut.findByPath(PATH_A), is(existingIgnore));
	}

	@Test
	public void testFindByPathStringNotFound() throws Exception {
		assertThat(cut.isPathIgnored(PATH_B), is(false));
	}

	@Test
	public void testIsPathIgnoredString() throws Exception {
		assertThat(cut.isPathIgnored(PATH_A), is(true));
	}

	@Test
	public void testIsPathIgnoredStringNope() throws Exception {
		assertThat(cut.isPathIgnored(PATH_B), is(false));
	}

	@Test
	public void testIsPathIgnoredPath() throws Exception {
		assertThat(cut.isPathIgnored(toPath(PATH_A)), is(true));
	}
}
