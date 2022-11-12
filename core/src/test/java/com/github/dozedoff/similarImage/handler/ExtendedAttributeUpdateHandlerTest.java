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
package com.github.dozedoff.similarImage.handler;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.io.HashAttribute;
import com.google.common.jimfs.Jimfs;

@RunWith(MockitoJUnitRunner.class)
public class ExtendedAttributeUpdateHandlerTest {
	private static final String TEST_FILE_PREFIX = ExtendedAttributeUpdateHandlerTest.class.getSimpleName();

	@Mock
	private HashAttribute hashAttribute;
	@Mock
	private ImagePHash hasher;

	private Path testFile;

	@InjectMocks
	private ExtendedAttributeUpdateHandler cut;

	private FileSystem fs;

	@Before
	public void setUp() throws Exception {
		fs = Jimfs.newFileSystem();

		testFile = fs.getPath(TEST_FILE_PREFIX);
		Files.createFile(testFile);
	}

	@After
	public void tearDown() throws Exception {
		fs.close();
	}

	@Test
	public void testHandleValidExtendedAttribute() throws Exception {
		when(hashAttribute.areAttributesValid(testFile)).thenReturn(true);

		assertThat(cut.handle(testFile), is(true));
	}

	@Test
	public void testHandleInvalidExtendedAttribute() throws Exception {
		when(hashAttribute.areAttributesValid(testFile)).thenReturn(false);

		assertThat(cut.handle(testFile), is(true));
	}

	@Test
	public void testHandleIOError() throws Exception {
		when(hashAttribute.areAttributesValid(testFile)).thenReturn(false);
		when(hasher.getLongHash(any(InputStream.class))).thenThrow(new IOException());

		assertThat(cut.handle(testFile), is(false));
	}
}
