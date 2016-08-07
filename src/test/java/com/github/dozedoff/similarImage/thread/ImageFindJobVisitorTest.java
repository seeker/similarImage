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
package com.github.dozedoff.similarImage.thread;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.handler.HashHandler;
import com.github.dozedoff.similarImage.io.Statistics;

@RunWith(MockitoJUnitRunner.class)
public class ImageFindJobVisitorTest {
	@Mock
	private Filter<Path> fileFilter;

	@Mock
	private HashHandler handler;

	private Collection<HashHandler> handlers;

	private Statistics statistics;

	@Mock
	private Path path;

	@Mock
	private BasicFileAttributes attrs;

	private ImageFindJobVisitor cut;


	@Before
	public void setUp() throws Exception {
		when(fileFilter.accept(any())).thenReturn(true);
		when(handler.handle(path)).thenReturn(true);

		handlers = new LinkedList<HashHandler>();
		handlers.add(handler);

		statistics = new Statistics();

		cut = new ImageFindJobVisitor(fileFilter, handlers, statistics);
	}

	@Test
	public void testVisitFileAccepted() throws Exception {
		cut.visitFile(path, attrs);
		
		assertThat(cut.getFileCount(), is(1));
	}

	@Test
	public void testVisitFileNotAccepted() throws Exception {
		when(fileFilter.accept(any())).thenReturn(false);

		cut.visitFile(path, attrs);

		assertThat(cut.getFileCount(), is(0));
	}

	@Test
	public void testVisitFileOkProcessedFiles() throws Exception {
		cut.visitFile(path, attrs);

		assertThat(statistics.getProcessedFiles(), is(1));
	}

	@Test
	public void testVisitFileOkFailedFiles() throws Exception {
		cut.visitFile(path, attrs);

		assertThat(statistics.getFailedFiles(), is(0));
	}

	@Test
	public void testVisitNotHandled() throws Exception {
		when(handler.handle(path)).thenReturn(false);

		cut.visitFile(path, attrs);

		assertThat(statistics.getFailedFiles(), is(1));
	}

	@Test
	public void testGetFileCount() throws Exception {
		cut.visitFile(path, attrs);

		assertThat(cut.getFileCount(), is(1));
	}
}
