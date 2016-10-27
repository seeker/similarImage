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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;

import javax.management.InvalidAttributeValueException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.io.ExtendedAttribute;
import com.github.dozedoff.similarImage.io.ExtendedAttributeQuery;
import com.github.dozedoff.similarImage.io.HashAttribute;
import com.github.dozedoff.similarImage.util.TestUtil;

@RunWith(MockitoJUnitRunner.class)
public class ExtendedAttributeHandlerTest {
	@Mock
	private HashAttribute hashAttribute;

	@Mock
	private ExtendedAttributeQuery eaQuery;

	@Mock
	private ImageRepository imageRepository;

	@InjectMocks
	private ExtendedAttributeHandler cut;

	private Path testFile;

	@Before
	public void setUp() throws Exception {
		testFile = TestUtil.getTempFileWithExtendedAttributeSupport(ExtendedAttributeHandlerTest.class.getSimpleName());

		when(hashAttribute.areAttributesValid(testFile)).thenReturn(true);
		when(eaQuery.isEaSupported(any(Path.class))).thenReturn(true);
	}

	@Test
	public void testHandleFileHasHash() throws Exception {
		assertThat(cut.handle(testFile), is(true));
	}

	@Test
	public void testHandleFileHasNoHash() throws Exception {
		when(hashAttribute.areAttributesValid(testFile)).thenReturn(false);

		assertThat(cut.handle(testFile), is(false));
	}

	@Test
	public void testHandleDbError() throws Exception {
		Mockito.doThrow(RepositoryException.class).when(imageRepository).store(any(ImageRecord.class));

		assertThat(cut.handle(testFile), is(false));
	}

	@Test
	public void testHandleFileReadError() throws Exception {
		when(hashAttribute.readHash(testFile)).thenThrow(new IOException());

		assertThat(cut.handle(testFile), is(false));
	}

	@Test
	public void testHandleAttributeError() throws Exception {
		when(hashAttribute.readHash(testFile)).thenThrow(new InvalidAttributeValueException());

		assertThat(cut.handle(testFile), is(false));
	}

	@Test
	public void testHandleCorruptFileIsHandled() throws Exception {
		when(eaQuery.isEaSupported(testFile)).thenReturn(true);
		when(hashAttribute.areAttributesValid(testFile)).thenReturn(true);
		ExtendedAttribute.setExtendedAttribute(testFile, ExtendedAttributeHandler.CORRUPT_EA_NAMESPACE, "");

		assertThat(cut.handle(testFile), is(true));
	}

	@Test
	public void testHandleCorruptFileNotStored() throws Exception {
		when(eaQuery.isEaSupported(testFile)).thenReturn(true);
		when(hashAttribute.areAttributesValid(testFile)).thenReturn(true);
		ExtendedAttribute.setExtendedAttribute(testFile, ExtendedAttributeHandler.CORRUPT_EA_NAMESPACE, "");

		verify(imageRepository, never()).store(any(ImageRecord.class));
	}
}
