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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.similarImage.db.PendingHashImage;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;

public class OrmlitePendingHashImageTest extends BaseOrmliteRepositoryTest {
	private static final String NEW_PATH = "foo";
	private static final String EXISTING_PATH = "bar";
	private static final int UNKNOWN_ID = 42;

	private PendingHashImage newEntry;
	private PendingHashImage existingEntry;

	private PendingHashImageRepository cut;
	private Dao<PendingHashImage, Integer> dao;

	@Before
	public void setUp() throws Exception {
		dao = DaoManager.createDao(db.getCs(), PendingHashImage.class);
		cut = new OrmlitePendingHashImage(DaoManager.createDao(db.getCs(), PendingHashImage.class));

		newEntry = new PendingHashImage(NEW_PATH);
		existingEntry = new PendingHashImage(EXISTING_PATH);

		dao.create(existingEntry);
	}

	@Test
	public void testStore() throws Exception {
		cut.store(newEntry);
		
		assertThat(dao.queryForMatchingArgs(newEntry), hasSize(1));
	}

	@Test(expected = RepositoryException.class)
	public void testStoreDuplicate() throws Exception {
		cut.store(existingEntry);
	}

	@Test
	public void testExistsById() throws Exception {
		assertThat(cut.exists(existingEntry), is(true));
	}

	@Test
	public void testExistsByPath() throws Exception {
		assertThat(cut.exists(new PendingHashImage(EXISTING_PATH)), is(true));
	}

	@Test
	public void testExistsNotFound() throws Exception {
		assertThat(cut.exists(newEntry), is(false));
	}

	@Test
	public void testRemoveByIdExisting() throws Exception {
		cut.removeById(1);

		assertThat(dao.queryForSameId(existingEntry), nullValue());
	}

	@Test
	public void testRemoveByIdNonExisting() throws Exception {
		cut.removeById(UNKNOWN_ID);
	}

	@Test
	public void testGetByIdExisting() throws Exception {
		assertThat(cut.getById(existingEntry.getId()), is(existingEntry));
	}
	
	@Test
	public void testGetByIdNotFOund() throws Exception {
		assertThat(cut.getById(UNKNOWN_ID), nullValue());
	}
	
	@Test
	public void testFirstIdNotZero() {
		assertThat(existingEntry.getId(), is(not(0)));
	}

	@Test
	public void testGetAll() throws Exception {
		dao.create(newEntry);

		assertThat(cut.getAll(), containsInAnyOrder(newEntry, existingEntry));
	}
}
