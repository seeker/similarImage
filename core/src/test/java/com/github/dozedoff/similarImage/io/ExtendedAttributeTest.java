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
import static org.junit.Assert.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.similarImage.util.TestUtil;

public class ExtendedAttributeTest {
	private static final String TEMP_FILE_PREFIX = "ExtendedAttributeTest";
	private static final String TEST_NAME = ExtendedAttribute.SIMILARIMAGE_NAMESPACE + ".junit";
	private static final String TEST_VALUE = "foobar";

	private Path tempFile;

	@Before
	public void setUp() throws Exception {
		tempFile = TestUtil.getTempFileWithExtendedAttributeSupport(TEMP_FILE_PREFIX);
	}

	@Test
	public void testSupportsExtendedAttributes() throws Exception {
		assertThat(ExtendedAttribute.supportsExtendedAttributes(tempFile), is(true));
	}

	@Test
	public void testSupportsExtendedAttributesNoFile() throws Exception {
		assertThat(ExtendedAttribute.supportsExtendedAttributes(Paths.get("foo")), is(false));
	}

	@Test
	public void testSetExtendedAttributePathStringString() throws Exception {
		ExtendedAttribute.setExtendedAttribute(tempFile, TEST_NAME, TEST_VALUE);
	}

	@Test
	public void testReadExtendedAttributeAsString() throws Exception {
		ExtendedAttribute.setExtendedAttribute(tempFile, TEST_NAME, TEST_VALUE);

		assertThat(ExtendedAttribute.readExtendedAttributeAsString(tempFile, TEST_NAME), is(TEST_VALUE));
	}

	@Test
	public void testExtendedAttributeIsNotSet() throws Exception {
		assertThat(ExtendedAttribute.isExtendedAttributeSet(tempFile, TEST_NAME), is(false));
	}

	@Test
	public void testExtendedAttributeIsSet() throws Exception {
		ExtendedAttribute.setExtendedAttribute(tempFile, TEST_NAME, TEST_VALUE);

		assertThat(ExtendedAttribute.isExtendedAttributeSet(tempFile, TEST_NAME), is(true));
	}

	@Test
	public void testCreateName() throws Exception {
		assertThat(ExtendedAttribute.createName("foo", "bar"), is(ExtendedAttribute.SIMILARIMAGE_NAMESPACE + ".foo.bar"));
	}
}
