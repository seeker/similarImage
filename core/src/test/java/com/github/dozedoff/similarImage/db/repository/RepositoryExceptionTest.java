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
package com.github.dozedoff.similarImage.db.repository;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

public class RepositoryExceptionTest {
	private static final String TEST_MESSAGE = "just testing";
	private Throwable testException;

	private RepositoryException cut;

	@Before
	public void setUp() throws Exception {
		testException = new IllegalArgumentException("test");
	}

	@Test
	public void testRepositoryExceptionStringThrowableMessage() throws Exception {
		cut = new RepositoryException(TEST_MESSAGE, testException);
		
		assertThat(cut.getMessage(), is(TEST_MESSAGE));
	}

	@Test
	public void testRepositoryExceptionStringThrowableCause() throws Exception {
		cut = new RepositoryException(TEST_MESSAGE, testException);

		assertThat(cut.getCause(), is(testException));
	}

	@Test
	public void testRepositoryExceptionStringMessage() throws Exception {
		cut = new RepositoryException(TEST_MESSAGE);

		assertThat(cut.getMessage(), is(TEST_MESSAGE));
	}

	@Test
	public void testRepositoryExceptionStringCause() throws Exception {
		cut = new RepositoryException(TEST_MESSAGE);

		assertThat(cut.getCause(), nullValue());
	}
}
