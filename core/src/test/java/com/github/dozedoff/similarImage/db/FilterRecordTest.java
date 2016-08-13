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

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class FilterRecordTest {
	private static final String GUARD_MSG = "Guard condition failed";
	private FilterRecord filterRecord;

	@Before
	public void setUp() throws Exception {
		filterRecord = new FilterRecord(42, "dontPanic");
	}

	@Test
	public void testGetpHash() throws Exception {
		assertThat(filterRecord.getpHash(), is(42L));
	}

	@Test
	public void testSetpHash() throws Exception {
		assertThat(GUARD_MSG, filterRecord.getpHash(), is(42L));

		filterRecord.setpHash(7L);

		assertThat(filterRecord.getpHash(), is(7L));
	}

	@Test
	public void testGetReason() throws Exception {
		assertThat(filterRecord.getReason(), is("dontPanic"));
	}

	@Test
	public void testSetReason() throws Exception {
		assertThat(GUARD_MSG, filterRecord.getReason(), is("dontPanic"));

		filterRecord.setReason("towel");

		assertThat(filterRecord.getReason(), is("towel"));
	}

	@Test
	public void testEqualsIsEqual() throws Exception {
		FilterRecord other = new FilterRecord(42, "dontPanic");
		assertThat(filterRecord.equals(other), is(true));
	}

	@Test
	public void testEquals() throws Exception {
		EqualsVerifier.forClass(FilterRecord.class).suppress(Warning.NONFINAL_FIELDS).verify();
	}
}
