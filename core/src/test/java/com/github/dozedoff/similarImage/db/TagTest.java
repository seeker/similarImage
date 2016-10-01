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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.similarImage.util.StringUtil;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class TagTest {
	private static final String TEST_TAG = "test";

	private Tag cut;

	@Before
	public void setUp() {
		cut = new Tag(TEST_TAG);
	}

	@Test
	public void testEquals() throws Exception {
		EqualsVerifier<Tag> ev = EqualsVerifier.forClass(Tag.class).allFieldsShouldBeUsedExcept("userTagId")
				.suppress(Warning.NONFINAL_FIELDS);

		ev.verify();
	}

	@Test
	public void testTagStringName() throws Exception {
		assertThat(cut.getTag(), is(TEST_TAG));
	}

	@Test
	public void testTagStringContextMenu() throws Exception {
		assertThat(cut.isContextMenu(), is(false));
	}

	@Test
	public void testIsMatchAll() throws Exception {
		assertThat(cut.isMatchAll(), is(false));
	}

	@Test
	public void testIsMatchAllAsterisk() throws Exception {
		cut = new Tag(StringUtil.MATCH_ALL_TAGS);

		assertThat(cut.isMatchAll(), is(true));
	}
}
