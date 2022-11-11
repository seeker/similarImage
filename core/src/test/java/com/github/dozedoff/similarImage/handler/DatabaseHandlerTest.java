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
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.io.Statistics;

@RunWith(MockitoJUnitRunner.class)
public class DatabaseHandlerTest {
	@Mock
	private ImageRepository imageRepository;

	@Mock
	private Statistics statistics;

	@InjectMocks
	private DatabaseHandler cut;

	private Path testFile;

	private ImageRecord existingImage;

	@Before
	public void setUp() throws Exception {
		testFile = Paths.get("foo");
		existingImage = new ImageRecord(testFile.toString(), 0);
	}

	@Test
	public void testHandleFileFoundGood() throws Exception {
		when(imageRepository.getByPath(testFile)).thenReturn(existingImage);

		assertThat(cut.handle(testFile), is(true));
	}

	@Test
	public void testHandleFileNotFound() throws Exception {
		assertThat(cut.handle(testFile), is(false));
	}

	@Test
	public void testHandleDatabaseError() throws Exception {
		when(imageRepository.getByPath(testFile)).thenThrow(new RepositoryException("test"));

		assertThat(cut.handle(testFile), is(false));
	}
}
