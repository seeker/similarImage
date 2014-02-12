/*  Copyright (C) 2014  Nicholas Wright
    
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

import java.awt.Dimension;
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

import com.github.dozedoff.similarImage.duplicate.ImageInfo;

@RunWith(MockitoJUnitRunner.class)
public class DuplicateEntryControllerTest {
	@Mock
	private ImageInfo imageInfo;

	@Mock
	private OperationsMenu opMenu;

	@Mock
	private Dimension thumbDimension;

	@Mock
	private DuplicateEntryView view;

	private DuplicateEntryController duplicateEntryController;
	private static Path testImage;

	@BeforeClass
	public static void setUpClass() throws Exception {
		testImage = Paths.get(Thread.currentThread().getContextClassLoader().getResource("testImage.jpg").toURI());
	}

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		when(view.getView()).thenReturn(new JPanel());

		when(opMenu.getMenu()).thenReturn(new JPopupMenu());

		when(imageInfo.getDimension()).thenReturn(new Dimension(5, 20));
		when(imageInfo.getPath()).thenReturn(testImage);
		when(imageInfo.getSize()).thenReturn(1024L);
		when(imageInfo.getpHash()).thenReturn(42L);
		when(imageInfo.getSizePerPixel()).thenReturn(10.24);

		duplicateEntryController = new DuplicateEntryController(imageInfo, thumbDimension);
		duplicateEntryController.setView(view);

	}

	@Test
	public void testGetImagePath() throws Exception {
		when(imageInfo.getPath()).thenReturn(Paths.get("foo"));

		assertThat(duplicateEntryController.getImagePath(), is(Paths.get("foo")));
	}

	@Test
	public void testGetImageInfo() throws Exception {
		assertThat(duplicateEntryController.getImageInfo(), is(imageInfo));
	}

	@Test
	public void testSetView() throws Exception {
		// verify(view).setImage(any(JLabel.class)); // FIXME ImageIO fails for unknown reason

		verify(view).createLable(eq("Path: " + testImage.toString()));
		verify(view).createLable(eq("Size: 1 kb"));
		verify(view).createLable(eq("Dimension: 5x20"));
		verify(view).createLable(eq("pHash: 42"));
		verify(view).createLable(eq("Size per Pixel: 10.24"));
	}

	@Test
	public void testGetView() throws Exception {
		assertThat(duplicateEntryController.getView(), is(view.getView()));
	}

	@Test
	public void testDisplayFullImage() throws Exception {
		duplicateEntryController.displayFullImage();

		verify(view).displayFullImage(any(JLabel.class), eq(testImage));
	}

}
