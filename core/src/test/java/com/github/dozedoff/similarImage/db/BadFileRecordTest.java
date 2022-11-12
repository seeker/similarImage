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
package com.github.dozedoff.similarImage.db;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

public class BadFileRecordTest {
	private BadFileRecord badFileRecord;

	@Before
	public void setUp() throws Exception {
		badFileRecord = new BadFileRecord(Paths.get("bad"));
	}

	@Test
	public void testGetPath() throws Exception {
		assertThat(badFileRecord.getPath(), is("bad"));
	}
}
