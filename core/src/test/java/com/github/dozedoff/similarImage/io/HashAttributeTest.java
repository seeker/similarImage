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

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

import javax.management.InvalidAttributeValueException;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.similarImage.util.TestUtil;

public class HashAttributeTest {
	private static final String TEMP_FILE_PREFIX = "HashAttributeTest";
	private static final String TEST_HASH_NAME = "testhash";
	private static final long TIMESTAMP_TOLERANCE = 10;
	private static final long TEST_VALUE = 42;
	private static final int HEXADECIMAL_RADIX = 16;
	private static final String INVALID_FILE_PATH = "foo";

	private Path tempFile;
	private HashAttribute cut;

	private String testHashFullName;
	private String timestampFullName;

	@Before
	public void setUp() throws Exception {
		tempFile = TestUtil.getTempFileWithExtendedAttributeSupport(TEMP_FILE_PREFIX);
		cut = new HashAttribute(TEST_HASH_NAME);

		testHashFullName = cut.getHashFQN();
		timestampFullName = cut.getTimestampFQN();
	}

	@Test
	public void testWrittenHashValue() throws Exception {
		cut.writeHash(tempFile, TEST_VALUE);

		assertThat(Long.parseUnsignedLong(ExtendedAttribute.readExtendedAttributeAsString(tempFile, testHashFullName), HEXADECIMAL_RADIX),
				is(TEST_VALUE));
	}

	@Test
	public void testWrittenTimeStamp() throws Exception {
		cut.writeHash(tempFile, TEST_VALUE);

		long timestamp = Files.getLastModifiedTime(tempFile).toMillis();

		assertThat(Long.parseUnsignedLong(ExtendedAttribute.readExtendedAttributeAsString(tempFile, timestampFullName)),
				is(allOf(greaterThan(timestamp - TIMESTAMP_TOLERANCE), lessThan(timestamp + TIMESTAMP_TOLERANCE))));
	}

	@Test
	public void testAreAttributesValidNoHashOrTimestamp() throws Exception {
		assertThat(cut.areAttributesValid(tempFile), is(false));
	}

	@Test
	public void testAreAttributesValidHashButNoTimestamp() throws Exception {
		ExtendedAttribute.setExtendedAttribute(tempFile, testHashFullName, Long.toString(TEST_VALUE));

		assertThat(cut.areAttributesValid(tempFile), is(false));
	}

	@Test
	public void testAreAttributesValidHashAndModifiedFile() throws Exception {
		ExtendedAttribute.setExtendedAttribute(tempFile, testHashFullName, Long.toString(TEST_VALUE));
		long timestamp = Files.getLastModifiedTime(tempFile).toMillis();
		timestamp += TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);

		ExtendedAttribute.setExtendedAttribute(tempFile, timestampFullName, Long.toString(timestamp));

		assertThat(cut.areAttributesValid(tempFile), is(false));
	}

	@Test
	public void testAreAttributesValidHashAndTimestampBefore() throws Exception {
		ExtendedAttribute.setExtendedAttribute(tempFile, testHashFullName, Long.toString(TEST_VALUE));
		long timestamp = Files.getLastModifiedTime(tempFile).toMillis();
		timestamp -= TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);

		ExtendedAttribute.setExtendedAttribute(tempFile, timestampFullName, Long.toString(timestamp));

		assertThat(cut.areAttributesValid(tempFile), is(false));
	}

	@Test
	public void testAreAttributesValidHashAndTimestampOK() throws Exception {
		ExtendedAttribute.setExtendedAttribute(tempFile, testHashFullName, Long.toString(TEST_VALUE));
		long timestamp = Files.getLastModifiedTime(tempFile).toMillis();
		ExtendedAttribute.setExtendedAttribute(tempFile, timestampFullName, Long.toString(timestamp));

		assertThat(cut.areAttributesValid(tempFile), is(true));
	}

	@Test(expected = InvalidAttributeValueException.class)
	public void testReadHashNoAttributes() throws Exception {
		cut.writeHash(tempFile, TEST_VALUE);
		Files.setLastModifiedTime(tempFile, FileTime.fromMillis(1));

		cut.readHash(tempFile);
	}

	@Test(expected = InvalidAttributeValueException.class)
	public void testReadHashInvalidFile() throws Exception {
		cut.readHash(Paths.get(INVALID_FILE_PATH));
	}

	@Test
	public void testReadHashAllOK() throws Exception {
		cut.writeHash(tempFile, TEST_VALUE);

		assertThat(cut.readHash(tempFile), is(TEST_VALUE));
	}

	@Test
	public void testGetHashFQN() throws Exception {
		assertThat(cut.getHashFQN(), is(ExtendedAttribute.createName("hash", TEST_HASH_NAME)));
	}

	@Test
	public void testGetTimestampFQN() throws Exception {
		assertThat(cut.getTimestampFQN(), is(ExtendedAttribute.createName("timestamp", TEST_HASH_NAME)));
	}

	@Test
	public void testMarkCorrupted() throws Exception {
		cut.markCorrupted(tempFile);

		assertThat(ExtendedAttribute.isExtendedAttributeSet(tempFile, cut.getCorruptNameFQN()), is(true));
	}

	@Test
	public void testGetCorruptNameFQN() throws Exception {
		assertThat(cut.getCorruptNameFQN(), is(ExtendedAttribute.createName("corrupt")));
	}

	@Test
	public void testIsCorruptedSet() throws Exception {
		ExtendedAttribute.setExtendedAttribute(tempFile, cut.getCorruptNameFQN(), "");

		assertThat(cut.isCorrupted(tempFile), is(true));
	}

	@Test
	public void testIsCorruptedNotSet() throws Exception {
		assertThat(cut.isCorrupted(tempFile), is(false));
	}
}
