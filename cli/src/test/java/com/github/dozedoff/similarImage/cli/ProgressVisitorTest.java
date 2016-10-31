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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.io.HashAttribute;
import com.github.dozedoff.similarImage.io.Statistics;


@RunWith(MockitoJUnitRunner.class)
public class ProgressVisitorTest {
	private static final Path PATH = Paths.get("foo");

	@Mock
	private HashAttribute hashAttribute;

	private Statistics statistics;

	private ProgressVisitor cut;

	@Before
	public void setUp() throws Exception {
		setCorrupt(false);
		setValid(true);
		
		statistics = new Statistics();

		cut = new ProgressVisitor(statistics, hashAttribute);
	}

	private void setValid(boolean isValid) {
		when(hashAttribute.areAttributesValid(any(Path.class))).thenReturn(isValid);
	}

	private void setCorrupt(boolean isCorrupt) throws IOException {
		when(hashAttribute.isCorrupted(any(Path.class))).thenReturn(isCorrupt);
	}

	private void visitTestFile() throws IOException {
		cut.visitFile(PATH, null);
	}

	@Test
	public void testProcessedFileProcessedCount() throws Exception {
		visitTestFile();

		assertThat(statistics.getProcessedFiles(), is(1));
	}

	@Test
	public void testProcessedFileFoundCount() throws Exception {
		visitTestFile();

		assertThat(statistics.getFoundFiles(), is(1));
	}

	@Test
	public void testProcessedFileFailedCount() throws Exception {
		visitTestFile();

		assertThat(statistics.getFailedFiles(), is(0));
	}

	@Test
	public void testCorruptFileProcessedCount() throws Exception {
		setCorrupt(true);

		visitTestFile();

		assertThat(statistics.getProcessedFiles(), is(0));
	}

	@Test
	public void testCorruptFileFailedCount() throws Exception {
		setCorrupt(true);

		visitTestFile();

		assertThat(statistics.getFailedFiles(), is(1));
	}

	@Test
	public void testCorruptFileFoundCount() throws Exception {
		setCorrupt(true);

		visitTestFile();

		assertThat(statistics.getFoundFiles(), is(1));
	}

	@Test
	public void testUnprocessedFileFoundCount() throws Exception {
		setValid(false);

		visitTestFile();

		assertThat(statistics.getFoundFiles(), is(1));
	}

	@Test
	public void testUnprocessedFileFailedCount() throws Exception {
		setValid(false);

		visitTestFile();

		assertThat(statistics.getFailedFiles(), is(0));
	}

	@Test
	public void testUnprocessedFileProcessedCount() throws Exception {
		setValid(false);

		visitTestFile();

		assertThat(statistics.getProcessedFiles(), is(0));
	}
}
