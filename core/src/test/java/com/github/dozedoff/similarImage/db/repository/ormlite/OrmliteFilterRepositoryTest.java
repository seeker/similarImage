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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.similarImage.db.FilterRecord;
import com.github.dozedoff.similarImage.db.Thumbnail;
import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public class OrmliteFilterRepositoryTest extends OrmliteRepositoryBaseTest {
	private static final long HASH_ONE = 1L;
	private static final long HASH_TWO = 2L;
	private static final long HASH_THREE = 3L;
	private static final long HASH_NEW_THUMBNAIL = 4L;
	private static final long HASH_EXISTING_THUMBNAIL = 5L;

	private static final String TAG_ONE = "foo";
	private static final String TAG_TWO = "bar";

	private static final byte[] THUMB_HASH_EXISTING = { 1, 2, 3 };
	private static final byte[] THUMB_HASH_NEW = { 9, 8, 7 };

	private FilterRepository cut;

	private Dao<FilterRecord, Integer> filterDao;
	private Dao<Thumbnail, Integer> thumbnailDao;

	private Thumbnail exsitingThumbnail;
	private Thumbnail newThumbnail;

	private List<FilterRecord> allFilters;

	private ConnectionSource cs;

	@Before
	public void setUp() throws Exception {
		cs = getConnectionSource();

		TableUtils.createTable(cs, FilterRecord.class);
		TableUtils.createTable(cs, Thumbnail.class);

		filterDao = DaoManager.createDao(cs, FilterRecord.class);
		thumbnailDao = DaoManager.createDao(cs, Thumbnail.class);

		cut = new OrmliteFilterRepository(filterDao, thumbnailDao);

		allFilters = new LinkedList<FilterRecord>();

		createRecords();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getDatabaseName() {
		return "OrmliteFilterRepository";
	}

	private void createRecords() throws Exception {
		exsitingThumbnail = new Thumbnail(THUMB_HASH_EXISTING, new byte[] {});
		newThumbnail = new Thumbnail(THUMB_HASH_NEW, new byte[] {});

		thumbnailDao.create(exsitingThumbnail);

		createRecord(HASH_ONE, TAG_ONE, null);
		createRecord(HASH_TWO, TAG_TWO, null);
		createRecord(HASH_ONE, TAG_TWO, null);
		createRecord(HASH_THREE, TAG_TWO, exsitingThumbnail);
	}

	private void createRecord(long hash, String tag, Thumbnail thumb) throws Exception {
		FilterRecord filter = new FilterRecord(hash, tag, thumb);

		filterDao.create(filter);
		allFilters.add(filter);
	}

	/**
	 * Delete the {@link FilterRecord} table to provoke errors.
	 */
	private void deleteFilterTable() throws Exception {
		TableUtils.dropTable(cs, FilterRecord.class, false);
	}

	@Test
		public void testGetByHash() throws Exception {
			List<FilterRecord> filters = cut.getByHash(HASH_TWO);
			
			assertThat(filters, containsInAnyOrder(new FilterRecord(HASH_TWO, TAG_TWO, null)));
		}

	@Test(expected = RepositoryException.class)
		public void testGetByHashException() throws Exception {
			deleteFilterTable();
	
			cut.getByHash(HASH_TWO);
		}

	@Test
		public void testGetByTag() throws Exception {
			List<FilterRecord> filters = cut.getByTag(TAG_ONE);
			
			assertThat(filters, containsInAnyOrder(new FilterRecord(HASH_ONE, TAG_ONE, null)));
		}

	@Test(expected = RepositoryException.class)
		public void testGetByTagException() throws Exception {
			deleteFilterTable();
	
			cut.getByTag(TAG_ONE);
		}

	@Test
		public void testGetAll() throws Exception {
			List<FilterRecord> filters = cut.getAll();
	
			assertThat(filters, containsInAnyOrder(allFilters.toArray(new FilterRecord[0])));
		}

	@Test(expected = RepositoryException.class)
		public void testGetAllException() throws Exception {
			deleteFilterTable();
	
			cut.getAll();
		}

	@Test
		public void testStoreWithExistingThumb() throws Exception {
			cut.store(new FilterRecord(HASH_NEW_THUMBNAIL, TAG_ONE, newThumbnail));
	
			List<FilterRecord> filters = cut.getByHash(HASH_NEW_THUMBNAIL);
	
			assertThat(filters, containsInAnyOrder(new FilterRecord(HASH_NEW_THUMBNAIL, TAG_ONE, newThumbnail)));
		}

	@Test(expected = RepositoryException.class)
		public void testStoreThumbnailException() throws Exception {
			TableUtils.dropTable(cs, Thumbnail.class, false);
	
			cut.store(new FilterRecord(HASH_NEW_THUMBNAIL, TAG_ONE, newThumbnail));
		}

	@Test(expected = RepositoryException.class)
		public void testStoreFilterException() throws Exception {
			TableUtils.dropTable(cs, FilterRecord.class, false);
	
			cut.store(new FilterRecord(HASH_NEW_THUMBNAIL, TAG_ONE, null));
		}

	@Test
		public void testStoreWithNewThumb() throws Exception {
			cut.store(new FilterRecord(HASH_EXISTING_THUMBNAIL, TAG_ONE, exsitingThumbnail));
	
			List<FilterRecord> filters = cut.getByHash(HASH_EXISTING_THUMBNAIL);
	
			assertThat(filters, containsInAnyOrder(new FilterRecord(HASH_EXISTING_THUMBNAIL, TAG_ONE, exsitingThumbnail)));
		}

	@Test
		public void testStoreWithThumbMatchingHash() throws Exception {
			Thumbnail matchingHashThumb = new Thumbnail(THUMB_HASH_EXISTING, new byte[] {});
			cut.store(new FilterRecord(HASH_NEW_THUMBNAIL, TAG_ONE, matchingHashThumb));
	
			List<FilterRecord> filters = cut.getByHash(HASH_NEW_THUMBNAIL);
	
			assertThat(filters, containsInAnyOrder(new FilterRecord(HASH_NEW_THUMBNAIL, TAG_ONE, exsitingThumbnail)));
		}

	@Test
		public void testStoreWithNoThumb() throws Exception {
			cut.store(new FilterRecord(HASH_NEW_THUMBNAIL, TAG_ONE, null));
	
			List<FilterRecord> filters = cut.getByHash(HASH_NEW_THUMBNAIL);
	
			assertThat(filters, containsInAnyOrder(new FilterRecord(HASH_NEW_THUMBNAIL, TAG_ONE, null)));
		}

	@Test
		public void testStoreDuplicate() throws Exception {
			cut.store(new FilterRecord(HASH_NEW_THUMBNAIL, TAG_ONE, null));
			cut.store(new FilterRecord(HASH_NEW_THUMBNAIL, TAG_ONE, null));
	
			List<FilterRecord> filters = cut.getByHash(HASH_NEW_THUMBNAIL);
	
			assertThat(filters, containsInAnyOrder(new FilterRecord(HASH_NEW_THUMBNAIL, TAG_ONE, null)));
		}
}
