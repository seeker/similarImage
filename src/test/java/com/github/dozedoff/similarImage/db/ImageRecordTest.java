/*  Copyright (C) 2014  Nicholas Wright
    
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
package com.github.dozedoff.similarImage.db;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class ImageRecordTest {
	private ImageRecord imageRecord;

	@Before
	public void setUp() throws Exception {
		imageRecord = new ImageRecord("foo", 42);
	}

	@Test
	public void testGetPath() throws Exception {
		assertThat(imageRecord.getPath(), is("foo"));
	}

	@Test
	public void testGetpHash() throws Exception {
		assertThat(imageRecord.getpHash(), is(42L));
	}

	@Test
	public void testCompareToSelf() throws Exception {
		assertThat(imageRecord.compareTo(imageRecord), is(0));
	}

	@Test(expected = NullPointerException.class)
	public void testCompareToNull() throws Exception {
		imageRecord.compareTo(null);
	}

	@Test
	public void testCompareToBigger() throws Exception {
		assertThat(imageRecord.compareTo(new ImageRecord("bar", 55)), is(-1));
	}

	@Test
	public void testCompareToSmaller() throws Exception {
		assertThat(imageRecord.compareTo(new ImageRecord("bar", 7)), is(1));
	}

	@Test
	public void testCompareToEqual() throws Exception {
		assertThat(imageRecord.compareTo(new ImageRecord("bar", 42)), is(0));
	}

	@Test
	public void testEquals() throws Exception {
		EqualsVerifier.forClass(ImageRecord.class).suppress(Warning.NONFINAL_FIELDS).verify();
	}
}
