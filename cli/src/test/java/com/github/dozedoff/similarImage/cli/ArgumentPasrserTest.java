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
package com.github.dozedoff.similarImage.cli;

import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.thread.ImageFindJobVisitor;

import net.sourceforge.argparse4j.inf.ArgumentParserException;


@RunWith(MockitoJUnitRunner.class)
public class ArgumentPasrserTest {
	private static final String LOCAL_SUBCOMMAND = "local";
	private static final String NODE_SUBCOMMAND = "node";

	@Mock
	private ImageFindJobVisitor visitor;

	private ArgumentPasrser cut;

	@Before
	public void setUp() {
		cut = new ArgumentPasrser(new DummyVisitor());
	}

	@Test(expected = ArgumentParserException.class)
	public void testParseArgsInvalidOption() throws Exception {
		cut.parseArgs(new String[] { LOCAL_SUBCOMMAND, "--foo" });
	}

	@Test
	public void testParseArgsUpdateOption() throws Exception {
		cut.parseArgs(new String[] { LOCAL_SUBCOMMAND, "--update", "foo" });
	}

	@Test
	public void testParseArgsNodeOption() throws Exception {
		cut.parseArgs(new String[] { NODE_SUBCOMMAND, "--port", "123" });
	}

	class DummyVisitor extends SimpleFileVisitor<Path> {
	}
}
