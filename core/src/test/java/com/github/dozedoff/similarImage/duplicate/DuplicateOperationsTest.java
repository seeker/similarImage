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
import com.github.dozedoff.similarImage.db.Tag;
import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.db.repository.TagRepository;

@RunWith(MockitoJUnitRunner.class)
public class DuplicateOperationsTest {
	private static final String GUARD_MSG = "Guard condition failed";

	private static final long TEST_HASH = 42L;
	private static final int RECORD_NUMBER = 15;

	private static final Tag TAG_FOO = new Tag("foo");
	private static final Tag TAG_BAR = new Tag("bar");
	private static final Tag TAG_DNW = new Tag("DNW");
	private static final Tag TAG_ALL = new Tag("all");

	@Mock
	private FilterRepository filterRepository;

	@Mock
	private TagRepository tagRepository;

	@Mock
	private ImageRepository imageRepository;

	private DuplicateOperations dupOp;

	private Path tempDirectory = null;

	private FilterRecord fooFilter;

	@Before
	public void setUp() throws Exception {
		dupOp = new DuplicateOperations(filterRepository, tagRepository, imageRepository);

		tempDirectory = Files.createTempDirectory("DuplicateOperationsTest");
		
		fooFilter = new FilterRecord(TEST_HASH, TAG_FOO);
	}

	@Ignore("Not implemented yet")
	@Test
	public void testMoveToDnw() throws Exception {
		List<Path> files = createTempTestFiles(1);
		Path file = files.get(0);

		dupOp.moveToDnw(file);

		assertThat(Files.exists(file), is(true));

		verify(imageRepository).remove(new ImageRecord(file.toString(), TEST_HASH));
		verify(filterRepository).store(new FilterRecord(anyLong(), TAG_DNW));
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
		verify(imageRepository, times(10)).remove(any(ImageRecord.class));
	}

	@Test
	public void testDeleteFile() throws Exception {
		List<Path> files = createTempTestFiles(1);
		Path file = files.get(0);

		assertThat(GUARD_MSG, Files.exists(file), is(true));

		dupOp.deleteFile(file);

		assertThat(Files.exists(file), is(false));
		verify(imageRepository).remove(new ImageRecord(file.toString(), 0));
	}

	@Test
	public void testDeleteFileNull() throws Exception {
		dupOp.deleteFile(null);

		verify(imageRepository, never()).remove(any(ImageRecord.class));
	}

	@Test
	public void testDeleteFileDirectory() throws Exception {
		createTempTestFiles(1);
		dupOp.deleteFile(tempDirectory);

		verify(imageRepository, never()).remove(any(ImageRecord.class));
	}

	@Test
	public void testDeleteFileDirectoryNonExisistantFile() throws Exception {
		createTempTestFiles(1);
		dupOp.deleteFile(tempDirectory.resolve("foobar"));

		verify(imageRepository).remove(any(ImageRecord.class));
	}

	@Test
	public void testDeleteFileDbError() throws Exception {
		List<Path> files = createTempTestFiles(1);
		Path file = files.get(0);

		Mockito.doThrow(new RepositoryException("This is a test")).when(imageRepository).remove(new ImageRecord(file.toString(), 0));

		assertThat(GUARD_MSG, Files.exists(file), is(true));

		dupOp.deleteFile(file);

		assertThat(Files.exists(file), is(true));
		verify(imageRepository).remove(new ImageRecord(file.toString(), 0));
	}

	@Test
	public void testMarkDnwAndDelete() throws Exception {
		List<Path> files = createTempTestFiles(RECORD_NUMBER);
		LinkedList<ImageRecord> records = new LinkedList<>();

		for (Path p : files) {
			records.add(new ImageRecord(p.toString(), 0));
		}

		dupOp.markDnwAndDelete(records);

		assertFilesDoNotExist(files);

		verify(filterRepository, times(RECORD_NUMBER)).store(new FilterRecord(anyLong(), TAG_DNW));
		verify(imageRepository, times(RECORD_NUMBER)).remove(any(ImageRecord.class));
	}

	@Test
	public void testMarkDnwAndDeleteDBerror() throws Exception {
		List<Path> files = createTempTestFiles(RECORD_NUMBER);
		LinkedList<ImageRecord> records = new LinkedList<>();

		Mockito.doThrow(new RepositoryException("This is a test")).when(filterRepository)
				.store(any(FilterRecord.class));

		for (Path p : files) {
			records.add(new ImageRecord(p.toString(), 0));
		}

		dupOp.markDnwAndDelete(records);

		guardFilesExist(files);

		verify(filterRepository, times(RECORD_NUMBER)).store(new FilterRecord(anyLong(), TAG_DNW));
		verify(imageRepository, never()).remove(any(ImageRecord.class));
	}

	@Test
	public void testMarkAsNotInDb() throws Exception {
		List<Path> files = createTempTestFiles(1);
		Path file = files.get(0);

		dupOp.markAs(file, "foo");

		verify(imageRepository).getByPath(any(Path.class));
		verify(filterRepository, never()).store(any(FilterRecord.class));
	}

	@Test
	public void testMarkAsNoFilterExists() throws Exception {
		List<Path> files = createTempTestFiles(1);
		Path file = files.get(0);

		when(imageRepository.getByPath(file)).thenReturn(new ImageRecord(file.toString(), TEST_HASH));

		dupOp.markAs(file, "foo");

		verify(filterRepository).store(fooFilter);
	}

	@Test
	public void testMarkAsDBerror() throws Exception {
		List<Path> files = createTempTestFiles(1);
		Path file = files.get(0);

		when(imageRepository.getByPath(file)).thenThrow(new RepositoryException("This is a test"));

		dupOp.markAs(file, "foo");

		verify(filterRepository, never()).store(any(FilterRecord.class));
	}

	@Test
	public void testMarkAsFilterExists() throws Exception {
		List<Path> files = createTempTestFiles(1);
		Path file = files.get(0);

		when(imageRepository.getByPath(file)).thenReturn(new ImageRecord(file.toString(), 42));

		dupOp.markAs(file, "foo");

		verify(filterRepository).store(new FilterRecord(42, TAG_FOO));
	}

	@Test
	public void testMarkDirectory() throws Exception {
		createTempTestFiles(3);

		dupOp.markDirectoryAs(tempDirectory, "foo");

		verify(imageRepository, times(3)).getByPath(any(Path.class));
		verify(filterRepository, never()).store(any(FilterRecord.class));
	}

	@Test
	public void testMarkDirectoryNotAdirectory() throws Exception {
		List<Path> files = createTempTestFiles(3);
		Path file = files.get(0);

		dupOp.markDirectoryAs(file, "foo");

		verify(imageRepository, never()).getByPath(any(Path.class));
		verify(filterRepository, never()).store(any(FilterRecord.class));
	}

	@Test
	public void testMarkDirectoryDirectoryIsNull() throws Exception {
		createTempTestFiles(3);

		dupOp.markDirectoryAs(null, "foo");

		verify(imageRepository, never()).getByPath(any(Path.class));
		verify(filterRepository, never()).store(any(FilterRecord.class));
	}

	@Test
	public void testMarkDirectoryDirectoryNonExistantDirectory() throws Exception {
		createTempTestFiles(3);

		dupOp.markDirectoryAs(tempDirectory.resolve("foobar"), "foo");

		verify(imageRepository, never()).getByPath(any(Path.class));
		verify(filterRepository, never()).store(any(FilterRecord.class));
	}

	@Test
	public void testMarkAll() throws Exception {
		LinkedList<ImageRecord> records = new LinkedList<ImageRecord>();
		records.add(new ImageRecord(TAG_FOO.getTag(), 0));
		records.add(new ImageRecord(TAG_BAR.getTag(), 1));

		dupOp.markAll(records, TAG_ALL.getTag());

		verify(filterRepository).store(new FilterRecord(0, TAG_ALL));
		verify(filterRepository).store(new FilterRecord(1, TAG_ALL));
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
		when(imageRepository.startsWithPath(tempDirectory))
				.thenReturn(Arrays.asList(new ImageRecord(testFile.toString(), 0), missingRecord));

		List<ImageRecord> missing = dupOp.findMissingFiles(tempDirectory);

		assertThat(missing, containsInAnyOrder(missingRecord));
	}
}
