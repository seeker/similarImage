/*  Copyright (C) 2017  Nicholas Wright
    
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
package com.github.dozedoff.similarImage.gui;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.db.IgnoreRecord;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.repository.IgnoreRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;


@RunWith(MockitoJUnitRunner.class)
public class IgnoredImagePresenterTest {
	private static final int CONCURRENT_TIMEOUT = 2000;

	@Mock
	private IgnoreRepository ignoreRepository;
	@Mock
	private IgnoredImageView ignoredImageView;

	private IgnoreRecord ignoreA;
	private IgnoreRecord ignoreB;

	private IgnoredImagePresenter cut;

	@Before
	public void setUp() throws Exception {
		cut = new IgnoredImagePresenter(ignoreRepository);

		ImageRecord imageA = new ImageRecord("", 0);
		ignoreA = new IgnoreRecord(imageA);
		ignoreB = new IgnoreRecord(imageA);

		when(ignoreRepository.getAll()).thenReturn(Arrays.asList(ignoreA, ignoreB));
	}

	@Test
	public void testSetView() throws Exception {
		cut.setView(ignoredImageView);

		verify(ignoredImageView, timeout(CONCURRENT_TIMEOUT)).pack();
	}

	@Test
	public void testGetModel() throws Exception {
		assertThat(cut.getModel(), is(not(nullValue())));
	}

	@Test
	public void testRefreshList() throws Exception {
		cut.setView(ignoredImageView);
		cut.refreshList();

		Awaitility.await().atMost(CONCURRENT_TIMEOUT, TimeUnit.MILLISECONDS).until(() -> cut.getModel().getSize(),
				is(2));
	}

	@Test
	public void testRefreshListViewIsPacked() throws Exception {
		cut.setView(ignoredImageView);
		cut.refreshList();

		verify(ignoredImageView, timeout(CONCURRENT_TIMEOUT).times(2)).pack();
	}

	@Test
	public void testRefreshListWithRepositoryError() throws Exception {
		when(ignoreRepository.getAll()).thenThrow(new RepositoryException("test"));

		cut.setView(ignoredImageView);
		cut.refreshList();

		verify(ignoredImageView, timeout(CONCURRENT_TIMEOUT).times(2)).pack();
	}

	@Test
	public void testRemoveIgnoredImages() throws Exception {
		cut.removeIgnoredImages(Arrays.asList(ignoreA));

		verify(ignoreRepository).remove(ignoreA);
	}

	@Test
	public void testRemoveIgnoredImagesWithRepositoryError() throws Exception {
		Mockito.doThrow(new RepositoryException("")).when(ignoreRepository).remove(ignoreA);

		cut.removeIgnoredImages(Arrays.asList(ignoreA, ignoreB));

		verify(ignoreRepository, times(2)).remove(any());
	}
}
