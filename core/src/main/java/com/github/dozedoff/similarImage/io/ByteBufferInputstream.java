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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteBufferInputstream extends InputStream {
	private final ByteBuffer buffer;

	/**
	 * Create an {@link InputStream} backed by the given {@link ByteBuffer}.
	 * 
	 * @param buffer
	 *            to read
	 */
	public ByteBufferInputstream(ByteBuffer buffer) {
		super();
		this.buffer = buffer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read() throws IOException {
		if (buffer.hasRemaining()) {
		return buffer.get();
		} else {
			return -1;
		}
	}

	/**
	 * Returns {@link ByteBuffer#remaining()} of the underlying buffer.
	 * 
	 * @exception IOException
	 *                will never be thrown.
	 * @return number of bytes that can still be read.
	 */
	@Override
	public int available() throws IOException {
		return buffer.remaining();
	}
}
