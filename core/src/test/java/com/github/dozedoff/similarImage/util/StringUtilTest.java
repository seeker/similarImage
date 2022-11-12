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
package com.github.dozedoff.similarImage.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class StringUtilTest {
	private static final String TEST_TAG = "foo";

	@Test
	public void testSanitizeTag() throws Exception {
		assertThat(StringUtil.sanitizeTag(TEST_TAG), is(TEST_TAG));
	}

	@Test
	public void testSanitizeTagNull() throws Exception {
		assertThat(StringUtil.sanitizeTag(null), is(StringUtil.MATCH_ALL_TAGS));
	}

	@Test
	public void testSanitizeTagEmpty() throws Exception {
		assertThat(StringUtil.sanitizeTag(""), is(StringUtil.MATCH_ALL_TAGS));
	}
}
