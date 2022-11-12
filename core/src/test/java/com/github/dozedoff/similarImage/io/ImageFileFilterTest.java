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
package com.github.dozedoff.similarImage.io;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.jimfs.Jimfs;

public class ImageFileFilterTest {
	private ImageFileFilter cut;
	private FileSystem fs;

	@Before
	public void setUp() throws Exception {
		fs = Jimfs.newFileSystem();
		cut = new ImageFileFilter();
	}

	@After
	public void tearDown() throws Exception {
		fs.close();
	}

	private Path createFile(String fileName) throws IOException {
		Path path = fs.getPath(fileName);
		Files.createFile(path);
		return path;
	}

	@Test
	public void testJpg() throws Exception {
		assertThat(cut.accept(createFile("foo.jpg")), is(true));
	}

	@Test
	public void testJpeg() throws Exception {
		assertThat(cut.accept(createFile("foo.jpeg")), is(true));
	}

	@Test
	public void testPng() throws Exception {
		assertThat(cut.accept(createFile("foo.png")), is(true));
	}

	@Test
	public void testGif() throws Exception {
		assertThat(cut.accept(createFile("foo.gif")), is(true));
	}

	@Test
	public void testNonLowerCase() throws Exception {
		assertThat(cut.accept(createFile("foo.Jpg")), is(true));
	}

	@Test
	public void testNonImageExtension() throws Exception {
		assertThat(cut.accept(createFile("foo.txt")), is(false));
	}
}
