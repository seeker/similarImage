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
package com.github.dozedoff.similarImage.gui;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.result.Result;

public class ThumbnailCacheLoaderTest {
	private ThumbnailCacheLoader cut;
	private Result key;
	private Result invalidImagePath;

	private Path image;

	@Before
	public void setUp() throws Exception {
		image = Paths.get(Thread.currentThread().getContextClassLoader().getResource("testImage.jpg").toURI());
		
		key = new Result(null, new ImageRecord(image.toString(), 0));
		invalidImagePath = new Result(null, new ImageRecord("foo", 0));
		
		cut = new ThumbnailCacheLoader();
	}

	@Test
	public void testLoadImage() throws Exception {
		assertThat(cut.load(key), is(notNullValue()));
	}

	@Test(expected = NoSuchFileException.class)
	public void testLoadInvalidPath() throws Exception {
		assertThat(cut.load(invalidImagePath), is(notNullValue()));
	}
}
