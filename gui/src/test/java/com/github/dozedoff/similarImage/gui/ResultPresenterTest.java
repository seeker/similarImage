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
package com.github.dozedoff.similarImage.gui;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.result.Result;
import com.github.dozedoff.similarImage.result.ResultGroup;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;

@RunWith(MockitoJUnitRunner.class)
public class ResultPresenterTest {
	private static final long HASH = 42L;

	@Mock
	private OperationsMenu opMenu;

	@Mock
	private ResultView view;

	private ResultPresenter duplicateEntryController;
	private static Path testImage;
	private Result result;
	private LoadingCache<Result, BufferedImage> thumbnailCache;

	@Mock
	private ResultGroup resultGroup;

	@BeforeClass
	public static void setUpClass() throws Exception {
		testImage = Paths.get(Thread.currentThread().getContextClassLoader().getResource("testImage.jpg").toURI());
	}

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		result = new Result(resultGroup, new ImageRecord(testImage.toString(), HASH));

		when(view.getView()).thenReturn(new JPanel());

		when(opMenu.getMenu()).thenReturn(new JPopupMenu());

		thumbnailCache = CacheBuilder.newBuilder().softValues().build(new ThumbnailCacheLoader());
		duplicateEntryController = new ResultPresenter(result, thumbnailCache);
		duplicateEntryController.setView(view);

	}

	@Test
	public void testGetImagePath() throws Exception {
		assertThat(duplicateEntryController.getImagePath(), is(Paths.get(testImage.toString())));
	}

	@Test
	public void testSetView() throws Exception {
		verify(view).setImage(any(JLabel.class));

		verify(view).createLable(eq("Path: " + testImage.toString()));
		verify(view).createLable(eq("Size: 1 kb"));
		verify(view).createLable(eq("Dimension: 40x40"));
		verify(view).createLable(eq("pHash: 42"));
		verify(view).createLable(eq("Size per Pixel: 1.11375"));
	}

	@Test
	public void testDisplayFullImage() throws Exception {
		duplicateEntryController.displayFullImage();

		verify(view).displayFullImage(any(JLabel.class), eq(testImage));
	}

}
