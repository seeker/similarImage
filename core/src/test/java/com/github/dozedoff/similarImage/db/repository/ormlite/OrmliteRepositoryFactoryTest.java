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

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.dozedoff.similarImage.db.FilterRecord;
import com.github.dozedoff.similarImage.db.IgnoreRecord;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.PendingHashImage;
import com.github.dozedoff.similarImage.db.SQLiteDatabase;
import com.github.dozedoff.similarImage.db.Tag;
import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.db.repository.IgnoreRepository;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.github.dozedoff.similarImage.db.repository.TagRepository;

public class OrmliteRepositoryFactoryTest {
	private static final String TEST_STRING = "Foo";

	private static OrmliteRepositoryFactory cut;
	private static Path testPath;
	private static SQLiteDatabase db;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		testPath = Files.createTempFile(OrmliteRepositoryFactoryTest.class.getSimpleName(), ".db");
		db = new SQLiteDatabase(testPath);
		cut = new OrmliteRepositoryFactory(db);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		db.close();
		Files.deleteIfExists(testPath);
	}

	@Test
	public void testBuildFilterRepository() throws Exception {
		FilterRepository fr = cut.buildFilterRepository();
		fr.store(new FilterRecord(0, new Tag(TEST_STRING)));
	}

	@Test
	public void testBuildImageRepository() throws Exception {
		ImageRepository ir = cut.buildImageRepository();
		ir.store(new ImageRecord(TEST_STRING, 0));
	}

	@Test
	public void testBuildTagRepository() throws Exception {
		TagRepository tr = cut.buildTagRepository();
		tr.store(new Tag(TEST_STRING));
	}

	@Test
	public void testBuildPendingImageRepository() throws Exception {
		PendingHashImageRepository phir = cut.buildPendingHashImageRepository();
		phir.store(new PendingHashImage(TEST_STRING, 0, 0));
	}

	@Test
	public void testBuildIgnoreRepository() throws Exception {
		IgnoreRepository ir = cut.buildIgnoreRepository();

		ir.store(new IgnoreRecord(new ImageRecord(TEST_STRING, 0)));
	}
}
