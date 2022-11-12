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
package com.github.dozedoff.similarImage.app;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Before;
import org.junit.Test;

import net.sourceforge.argparse4j.inf.Namespace;

public class SimilarImageTest {
	private static final String WORKERS_ARG_NAME = "no_workers";

	private Namespace noWorkersArgs;
	private Namespace localModeArgs;

	@Before
	public void setUp() throws Exception {
		noWorkersArgs = SimilarImage.parseArgs(new String[] { "--no-workers" });
		localModeArgs = SimilarImage.parseArgs(new String[] {});
	}

	@Test
	public void testBrokerOnlyModeSet() throws Exception {
		assertThat(noWorkersArgs.getBoolean(WORKERS_ARG_NAME), is(true));
	}

	@Test
	public void testLocalModeOnly() throws Exception {
		assertThat(localModeArgs.getBoolean(WORKERS_ARG_NAME), is(false));
	}

	@Test
	public void testIsNoWorkersMode() throws Exception {
		assertThat(SimilarImage.isNoWorkersMode(noWorkersArgs), is(true));
	}

	@Test
	public void testIsLocalMode() throws Exception {
		assertThat(SimilarImage.isNoWorkersMode(localModeArgs), is(false));
	}
}
