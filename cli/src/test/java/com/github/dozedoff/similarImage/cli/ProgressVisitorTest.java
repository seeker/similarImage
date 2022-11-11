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
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.github.dozedoff.similarImage.io.HashAttribute;
import com.google.common.jimfs.Jimfs;


@RunWith(MockitoJUnitRunner.class)
public class ProgressVisitorTest {
	private static Path path;
	private static Path pathNonImage;
	private static final String TEST_DIRECTORY = "baz";

	@Mock
	private HashAttribute hashAttribute;

	private MetricRegistry metrics;

	private ProgressVisitor cut;

	private static FileSystem fs;

	private Counter foundFiles;
	private Counter processedFiles;
	private Counter failedFiles;
	private Meter filesPerSecond;

	@BeforeClass
	public static void beforeClass() throws Exception {
		fs = Jimfs.newFileSystem();
		Files.createDirectory(fs.getPath(TEST_DIRECTORY));

		path = fs.getPath(TEST_DIRECTORY, "foo.jpg");
		Files.createFile(path);

		pathNonImage = fs.getPath(TEST_DIRECTORY, "bar.txt");
		Files.createFile(pathNonImage);

	}

	@AfterClass
	public static void afterClass() throws Exception {
		fs.close();
	}

	@Before
	public void setUp() throws Exception {
		setCorrupt(false);
		setValid(true);
		
		metrics = new MetricRegistry();

		cut = new ProgressVisitor(metrics, hashAttribute);
		foundFiles = metrics.counter(ProgressCalc.METRIC_NAME_FOUND);
		processedFiles = metrics.counter(ProgressCalc.METRIC_NAME_PROCESSED);
		failedFiles = metrics.counter(ProgressCalc.METRIC_NAME_FAILED);
		filesPerSecond = metrics.meter(ProgressCalc.METRIC_NAME_FILES_PER_SECOND);

	}

	private void setValid(boolean isValid) {
		when(hashAttribute.areAttributesValid(any(Path.class))).thenReturn(isValid);
	}

	private void setCorrupt(boolean isCorrupt) throws IOException {
		when(hashAttribute.isCorrupted(any(Path.class))).thenReturn(isCorrupt);
	}

	private void visitTestFile() throws IOException {
		cut.visitFile(path, null);
	}

	@Test
	public void testProcessedFileProcessedCount() throws Exception {
		visitTestFile();

		assertThat(processedFiles.getCount(), is(1L));
	}

	@Test
	public void testProcessedFileFoundCount() throws Exception {
		visitTestFile();

		assertThat(foundFiles.getCount(), is(1L));
	}

	@Test
	public void testProcessedFileFoundCountForNonImage() throws Exception {
		cut.visitFile(pathNonImage, null);

		assertThat(foundFiles.getCount(), is(0L));
	}

	@Test
	public void testProcessedFileFailedCount() throws Exception {
		visitTestFile();

		assertThat(failedFiles.getCount(), is(0L));
	}

	@Test
	public void testCorruptFileProcessedCount() throws Exception {
		setCorrupt(true);

		visitTestFile();

		assertThat(processedFiles.getCount(), is(0L));
	}

	@Test
	public void testCorruptFileFailedCount() throws Exception {
		setCorrupt(true);

		visitTestFile();

		assertThat(failedFiles.getCount(), is(1L));
	}

	@Test
	public void testCorruptFileFoundCount() throws Exception {
		setCorrupt(true);

		visitTestFile();

		assertThat(foundFiles.getCount(), is(1L));
	}

	@Test
	public void testUnprocessedFileFoundCount() throws Exception {
		setValid(false);

		visitTestFile();

		assertThat(foundFiles.getCount(), is(1L));
	}

	@Test
	public void testUnprocessedFileFailedCount() throws Exception {
		setValid(false);

		visitTestFile();

		assertThat(failedFiles.getCount(), is(0L));
	}

	@Test
	public void testUnprocessedFileProcessedCount() throws Exception {
		setValid(false);

		visitTestFile();

		assertThat(processedFiles.getCount(), is(0L));
	}

	@Test
	public void testProcessedFilesPerSecond() throws Exception {
		setValid(true);

		visitTestFile();

		assertThat(filesPerSecond.getMeanRate(), is(not(0.0)));
	}
}
