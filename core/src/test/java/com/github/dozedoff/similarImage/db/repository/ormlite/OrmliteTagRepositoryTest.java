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
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.similarImage.db.Tag;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.db.repository.TagRepository;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public class OrmliteTagRepositoryTest extends OrmliteRepositoryBaseTest {
	private static final String TAG_EXSTING = "existing";
	private static final String TAG_NEW = "new";
	private static final String TAG_CONTEXT = "context";

	private Tag tagNew;
	private Tag tagExsting;
	private Tag tagContext;

	private TagRepository cut;

	private Dao<Tag, Long> tagDao;
	private ConnectionSource cs;
	@Before
	public void setUp() throws Exception {
		cs = getConnectionSource();
		tagDao = DaoManager.createDao(cs, Tag.class);
		
		setUpDatabase();

		cut = new OrmliteTagRepository(tagDao);
	}

	private void setUpDatabase() throws Exception {
		TableUtils.createTable(cs, Tag.class);
		
		tagNew = new Tag(TAG_NEW);
		tagExsting = new Tag(TAG_EXSTING);
		tagContext = new Tag(TAG_CONTEXT, true);

		tagDao.create(tagExsting);
		tagDao.create(tagContext);
	}

	@Test
	public void testGetByName() throws Exception {
		Tag result = cut.getByName(TAG_EXSTING);
		
		assertThat(result, is(tagExsting));
	}

	@Test
	public void testStore() throws Exception {
		cut.store(tagNew);
		Tag result = cut.getByName(TAG_NEW);

		assertThat(result, is(tagNew));
	}

	@Test
	public void testStoreUpdated() throws Exception {
		tagExsting.setContextMenu(true);
		cut.store(tagExsting);

		Tag result = cut.getByName(TAG_EXSTING);

		assertThat(result.isContextMenu(), is(true));
	}

	@Test(expected = RepositoryException.class)
	public void testStoreDuplicate() throws Exception {
		cut.store(tagNew);

		cut.store(new Tag(TAG_NEW));
	}

	@Test
	public void testRemove() throws Exception {
		cut.remove(tagExsting);

		Tag result = cut.getByName(TAG_EXSTING);

		assertThat(result, is(nullValue()));
	}

	@Test
	public void testGetAll() throws Exception {
		List<Tag> results = cut.getAll();

		assertThat(results, containsInAnyOrder(tagContext, tagExsting));
	}

	@Test
	public void testGetWithContext() throws Exception {
		List<Tag> results = cut.getWithContext();

		assertThat(results, containsInAnyOrder(tagContext));
	}

}
