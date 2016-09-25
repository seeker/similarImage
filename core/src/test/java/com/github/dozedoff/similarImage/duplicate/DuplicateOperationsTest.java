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
package com.github.dozedoff.similarImage.duplicate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.db.FilterRecord;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;

@RunWith(MockitoJUnitRunner.class)
public class DuplicateOperationsTest {
	private static final String GUARD_MSG = "Guard condition failed";

	@Mock
	private Persistence persistence;
	private DuplicateOperations dupOp;

	private Path tempDirectory = null;

	@Before
	public void setUp() throws Exception {
		dupOp = new DuplicateOperations(persistence);

		tempDirectory = Files.createTempDirectory("DuplicateOperationsTest");
	}

	@Ignore("Not implemented yet")
	@Test
	public void testMoveToDnw() throws Exception {
		List<Path> files = createTempTestFiles(1);
		Path file = files.get(0);

		dupOp.moveToDnw(file);

		assertThat(Files.exists(file), is(true));

		verify(persistence).deleteRecord(new ImageRecord(file.toString(), 42));
		verify(persistence).addFilter(new FilterRecord(anyLong(), "DNW"));
	}

	@Test
	public void testDeleteAll() throws Exception {
		List<Path> files = createTempTestFiles(10);
		LinkedList<ImageRecord> records = new LinkedList<>();

		for (Path p : files) {
			records.add(new ImageRecord(p.toString(), 0));
		}

		dupOp.deleteAll(records);

		assertFilesDoNotExist(files);
		verify(persistence, times(10)).deleteRecord(any(ImageRecord.class));
	}

	@Test
	public void testDeleteFile() throws Exception {
		List<Path> files = createTempTestFiles(1);
		Path file = files.get(0);

		assertThat(GUARD_MSG, Files.exists(file), is(true));

		dupOp.deleteFile(file);

		assertThat(Files.exists(file), is(false));
		verify(persistence).deleteRecord(new ImageRecord(file.toString(), 0));
	}

	@Test
	public void testDeleteFileNull() throws Exception {
		dupOp.deleteFile(null);

		verify(persistence, never()).deleteRecord(any(ImageRecord.class));
	}

	@Test
	public void testDeleteFileDirectory() throws Exception {
		createTempTestFiles(1);
		dupOp.deleteFile(tempDirectory);

		verify(persistence, never()).deleteRecord(any(ImageRecord.class));
	}

	@Test
	public void testDeleteFileDirectoryNonExisistantFile() throws Exception {
		createTempTestFiles(1);
		dupOp.deleteFile(tempDirectory.resolve("foobar"));

		verify(persistence).deleteRecord(any(ImageRecord.class));
	}

	@Test
	public void testDeleteFileDbError() throws Exception {
		List<Path> files = createTempTestFiles(1);
		Path file = files.get(0);

		Mockito.doThrow(new SQLException("This is a test")).when(persistence).deleteRecord(new ImageRecord(file.toString(), 0));

		assertThat(GUARD_MSG, Files.exists(file), is(true));

		dupOp.deleteFile(file);

		assertThat(Files.exists(file), is(true));
		verify(persistence).deleteRecord(new ImageRecord(file.toString(), 0));
	}

	@Test
	public void testMarkDnwAndDelete() throws Exception {
		List<Path> files = createTempTestFiles(15);
		LinkedList<ImageRecord> records = new LinkedList<>();

		for (Path p : files) {
			records.add(new ImageRecord(p.toString(), 0));
		}

		dupOp.markDnwAndDelete(records);

		assertFilesDoNotExist(files);

		verify(persistence, times(15)).addFilter(new FilterRecord(anyLong(), "DNW"));
		verify(persistence, times(15)).deleteRecord(any(ImageRecord.class));
	}

	@Test
	public void testMarkDnwAndDeleteDBerror() throws Exception {
		List<Path> files = createTempTestFiles(15);
		LinkedList<ImageRecord> records = new LinkedList<>();

		Mockito.doThrow(new SQLException("This is a test")).when(persistence).addFilter(any(FilterRecord.class));

		for (Path p : files) {
			records.add(new ImageRecord(p.toString(), 0));
		}

		dupOp.markDnwAndDelete(records);

		guardFilesExist(files);

		verify(persistence, times(15)).addFilter(new FilterRecord(anyLong(), "DNW"));
		verify(persistence, never()).deleteRecord(any(ImageRecord.class));
	}

	@Test
	public void testMarkAsNotInDb() throws Exception {
		List<Path> files = createTempTestFiles(1);
		Path file = files.get(0);

		dupOp.markAs(file, "foo");

		verify(persistence).getRecord(any(Path.class));
		verify(persistence, never()).addFilter(any(FilterRecord.class));
	}

	@Test
	public void testMarkAsNoFilterExists() throws Exception {
		List<Path> files = createTempTestFiles(1);
		Path file = files.get(0);

		when(persistence.getRecord(file)).thenReturn(new ImageRecord(file.toString(), 42));

		dupOp.markAs(file, "foo");

		verify(persistence).getFilter(42);
		verify(persistence).addFilter(new FilterRecord(42, "foo"));
	}

	@Test
	public void testMarkAsDBerror() throws Exception {
		List<Path> files = createTempTestFiles(1);
		Path file = files.get(0);

		when(persistence.getRecord(file)).thenThrow(new SQLException("This is a test"));

		dupOp.markAs(file, "foo");

		verify(persistence, never()).getFilter(anyLong());
		verify(persistence, never()).addFilter(any(FilterRecord.class));
	}

	@Test
	public void testMarkAsFilterExists() throws Exception {
		List<Path> files = createTempTestFiles(1);
		Path file = files.get(0);

		when(persistence.getRecord(file)).thenReturn(new ImageRecord(file.toString(), 42));
		when(persistence.getFilter(42)).thenReturn(new FilterRecord(42, "bar"));

		dupOp.markAs(file, "foo");

		verify(persistence).addFilter(new FilterRecord(42, "foo"));
	}

	@Test
	public void testMarkDirectory() throws Exception {
		createTempTestFiles(3);

		dupOp.markDirectoryAs(tempDirectory, "foo");

		verify(persistence, times(3)).getRecord(any(Path.class));
		verify(persistence, never()).addFilter(any(FilterRecord.class));
	}

	@Test
	public void testMarkDirectoryNotAdirectory() throws Exception {
		List<Path> files = createTempTestFiles(3);
		Path file = files.get(0);

		dupOp.markDirectoryAs(file, "foo");

		verify(persistence, never()).getRecord(any(Path.class));
		verify(persistence, never()).addFilter(any(FilterRecord.class));
	}

	@Test
	public void testMarkDirectoryDirectoryIsNull() throws Exception {
		createTempTestFiles(3);

		dupOp.markDirectoryAs(null, "foo");

		verify(persistence, never()).getRecord(any(Path.class));
		verify(persistence, never()).addFilter(any(FilterRecord.class));
	}

	@Test
	public void testMarkDirectoryDirectoryNonExistantDirectory() throws Exception {
		createTempTestFiles(3);

		dupOp.markDirectoryAs(tempDirectory.resolve("foobar"), "foo");

		verify(persistence, never()).getRecord(any(Path.class));
		verify(persistence, never()).addFilter(any(FilterRecord.class));
	}

	@Test
	public void testMarkAll() throws Exception {
		LinkedList<ImageRecord> records = new LinkedList<ImageRecord>();
		records.add(new ImageRecord("foo", 0));
		records.add(new ImageRecord("bar", 1));

		String testTag = "test";

		dupOp.markAll(records, testTag);

		verify(persistence).addFilter(new FilterRecord(0, testTag));
		verify(persistence).addFilter(new FilterRecord(1, testTag));
	}

	private LinkedList<Path> createTempTestFiles(int amount) throws IOException {
		LinkedList<Path> tempFiles = new LinkedList<>();
		tempDirectory = Files.createTempDirectory("DuplicateOperationsTest");

		for (int i = 0; i < amount; i++) {
			Path testFile = Files.createTempFile(tempDirectory, "testFile", null);
			tempFiles.add(testFile);
		}

		guardFilesExist(tempFiles);
		return tempFiles;
	}

	private void guardFilesExist(List<Path> files) {
		for (Path p : files) {
			assertThat(GUARD_MSG, Files.exists(p), is(true));
		}
	}

	private void assertFilesDoNotExist(List<Path> files) {
		for (Path p : files) {
			assertThat(GUARD_MSG + " for " + p.toString(), Files.exists(p), is(false));
		}
	}

	@Test
	public void testFindMissingFiles() throws Exception {
		Path testFile = Files.createTempFile(tempDirectory, "findmissingfilestest", null);
		ImageRecord missingRecord = new ImageRecord(testFile.getParent().resolve("foo").toString(), 0);
		when(persistence.filterByPath(tempDirectory)).thenReturn(Arrays.asList(new ImageRecord(testFile.toString(), 0), missingRecord));

		List<ImageRecord> missing = dupOp.findMissingFiles(tempDirectory);

		assertThat(missing, containsInAnyOrder(missingRecord));
	}
}
