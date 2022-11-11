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
package com.github.dozedoff.similarImage.handler;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.io.HashAttribute;
import com.github.dozedoff.similarImage.io.Statistics;
import com.github.dozedoff.similarImage.thread.ImageHashJob;

@RunWith(MockitoJUnitRunner.class)
public class HashingHandlerTest {
	@Mock
	private HashAttribute hashAttribute;

	@Mock
	private ImagePHash hasher;

	@Mock
	private ImageRepository imageRepository;

	@Mock
	private Statistics statistics;

	@Mock
	private ExecutorService threadPool;

	@InjectMocks
	private HashingHandler cut;

	private Path testPath;

	@Before
	public void setUp() throws Exception {
		testPath = Paths.get("foo");
	}

	@Test
	public void testHandleAlwaysTrue() throws Exception {
		assertThat(cut.handle(testPath), is(true));
	}

	@Test
	public void testHandleJobIsExecuted() throws Exception {
		cut.handle(testPath);

		verify(threadPool).execute(any(ImageHashJob.class));
	}
}
