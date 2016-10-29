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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.similarImage.db.PendingHashImage;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;

public class OrmlitePendingHashImageTest extends BaseOrmliteRepositoryTest {
	private static final String NEW_PATH = "foo";
	private static final String EXISTING_PATH = "bar";
	private static final UUID UUID_NEW = UUID.fromString("0e7156c1-bff3-4954-9693-63a3136bf885");
	private static final UUID UUID_EXISTING = UUID.fromString("4dc1a7ad-0d52-4606-a77d-ca3e7fcd227c");

	private PendingHashImage newEntry;
	private PendingHashImage existingEntry;

	private PendingHashImageRepository cut;
	private Dao<PendingHashImage, Integer> dao;

	@Before
	public void setUp() throws Exception {
		dao = DaoManager.createDao(db.getCs(), PendingHashImage.class);
		cut = new OrmlitePendingHashImage(DaoManager.createDao(db.getCs(), PendingHashImage.class));

		newEntry = new PendingHashImage(NEW_PATH, UUID_NEW.getMostSignificantBits(), UUID_NEW.getLeastSignificantBits());
		existingEntry = new PendingHashImage(EXISTING_PATH, UUID_EXISTING.getMostSignificantBits(),
				UUID_EXISTING.getLeastSignificantBits());

		dao.create(existingEntry);
	}

	@Test
	public void testStoreQueryDatabase() throws Exception {
		cut.store(newEntry);

		assertThat(dao.queryForMatchingArgs(newEntry), hasSize(1));
	}

	@Test
	public void testStoreReturnValue() throws Exception {
		assertThat(cut.store(newEntry), is(true));
	}

	@Test
	public void testStoreDuplicate() throws Exception {
		assertThat(cut.store(existingEntry), is(false));
	}

	@Test
	public void testExistsById() throws Exception {
		assertThat(cut.exists(existingEntry), is(true));
	}

	@Test
	public void testExistsByPath() throws Exception {
		assertThat(cut.exists(new PendingHashImage(EXISTING_PATH, UUID_EXISTING)), is(true));
	}

	@Test
	public void testExistsNotFound() throws Exception {
		assertThat(cut.exists(newEntry), is(false));
	}

	@Test
	public void testGetAll() throws Exception {
		dao.create(newEntry);

		assertThat(cut.getAll(), containsInAnyOrder(newEntry, existingEntry));
	}

	@Test
	public void testGetByUuidNonExisting() throws Exception {
		assertThat(cut.getByUUID(UUID_NEW.getMostSignificantBits(), UUID_NEW.getLeastSignificantBits()), is(nullValue()));
	}

	@Test
	public void testGetByUuidExisting() throws Exception {
		assertThat(cut.getByUUID(UUID_EXISTING.getMostSignificantBits(), UUID_EXISTING.getLeastSignificantBits()), is(existingEntry));
	}

	@Test
	public void testRemove() throws Exception {
		assertThat(dao.queryForSameId(existingEntry), is(notNullValue())); // guard assert

		cut.remove(existingEntry);

		assertThat(dao.queryForSameId(existingEntry), is(nullValue()));
	}
}
