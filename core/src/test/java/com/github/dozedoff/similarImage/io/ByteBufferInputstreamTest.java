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

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

public class ByteBufferInputstreamTest {
	private static final byte[] TEST_DATA = { 2, 4, 6, 8, 7, 4, 89, 43, 12, 90 };
	private static final int BUFFER_CAPACITY = 50;
	private ByteBuffer buffer;
	private InputStream input;

	@Before
	public void setUp() throws Exception {
		buffer = ByteBuffer.allocateDirect(BUFFER_CAPACITY);
		buffer.put(TEST_DATA);
		buffer.flip();

		input = new ByteBufferInputstream(buffer);
	}

	@Test
	public void testReadFirstByte() throws Exception {
		assertThat(input.read(), is(2));
	}

	@Test
	public void testReadAllData() throws Exception {
		byte[] read = new byte[TEST_DATA.length];

		input.read(read);

		assertThat(read, is(TEST_DATA));
	}

	@Test
	public void testReadAllDataByteRead() throws Exception {
		byte[] read  = new byte[BUFFER_CAPACITY *2];

		int actualRead = input.read(read);

		assertThat(actualRead, is(TEST_DATA.length));
	}

	@Test
	public void testAvailable() throws Exception {
		assertThat(input.available(), is(TEST_DATA.length));
	}

	@Test
	public void testReadAllDataBuffered() throws Exception {
		byte[] read = new byte[TEST_DATA.length];

		BufferedInputStream bis = new BufferedInputStream(input);
		bis.read(read);

		assertThat(read, is(TEST_DATA));
	}
}
