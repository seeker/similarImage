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
package com.github.dozedoff.similarImage.thread;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

public class NamedThreadFactoryTest {
	private NamedThreadFactory ntf;

	@Before
	public void setUp() throws Exception {
		ntf = new NamedThreadFactory("test");
	}

	@Test
	public void testNewThread() throws Exception {
		Thread t = ntf.newThread(null);

		assertThat(t.getName(), is("test thread 0"));
	}

	@Test
	public void testNewThreadTwo() throws Exception {
		ntf.newThread(null);
		Thread t2 = ntf.newThread(null);

		assertThat(t2.getName(), is("test thread 1"));
	}

}
