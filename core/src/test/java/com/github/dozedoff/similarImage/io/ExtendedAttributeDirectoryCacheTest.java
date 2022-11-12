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

import org.junit.Rule;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.quality.Strictness;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ExtendedAttributeDirectoryCacheTest {
	public @Rule MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

	private static final String PATH = "/foo/bar";
	private static final String PATH_ROOT = "/baz";

	@Mock
	private ExtendedAttributeQuery eaQuery;

	private ExtendedAttributeDirectoryCache cut;

	private Path subDirectory;
	private Path rootDirectory;
	private Path root;
	private Path relative;

	@Before
	public void setUp() throws Exception {
		lenient().when(eaQuery.isEaSupported(any(Path.class))).thenReturn(true);

		subDirectory = Paths.get(PATH);
		rootDirectory = Paths.get(PATH_ROOT);
		root = Paths.get("/");
		relative = Paths.get("foo");

		cut = new ExtendedAttributeDirectoryCache(eaQuery);
	}

	@Test
	public void testIsEaSupportedUseCache() throws Exception {
		assertThat(cut.isEaSupported(subDirectory), is(true));
		assertThat(cut.isEaSupported(subDirectory), is(true));
		
		verify(eaQuery).isEaSupported(any());
	}

	@Test
	public void testIsEaSupportedExpireCache() throws Exception {
		cut = new ExtendedAttributeDirectoryCache(eaQuery, 1, TimeUnit.MICROSECONDS);
		assertThat(cut.isEaSupported(subDirectory), is(true));

		when(eaQuery.isEaSupported(any(Path.class))).thenReturn(false);

		assertThat(cut.isEaSupported(subDirectory), is(false));
	}

	@Test
	public void testIsEaSupportedRootParent() throws Exception {
		assertThat(cut.isEaSupported(rootDirectory), is(true));
	}

	@Test
	public void testIsEaSupportedRoot() throws Exception {
		assertThat(cut.isEaSupported(root), is(true));
	}

	@Test
	public void testIsEaSupportedNoParent() throws Exception {
		assertThat(cut.isEaSupported(relative), is(false));
	}
}
