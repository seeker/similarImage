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
package com.github.dozedoff.similarImage.app;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

public class SettingsTest {
	@Before
	public void setup() {
	}

	@Test
	public void testGetpHashWorkersDefaultValue() throws Exception {
		int value = Settings.getInstance().getpHashWorkers();
		assertThat(value, is(2));
	}

	@Test
	public void testGetDataLoadersDefaultValue() throws Exception {
		int value = Settings.getInstance().getDataLoaders();
		assertThat(value, is(1));
	}

	@Test
	public void testGetDataLoaderPriorityDefaultValue() throws Exception {
		int value = Settings.getInstance().getDataLoaderPriority();
		assertThat(value, is(2));
	}

	@Test
	public void testGetThumbnailDimensionDefaultValue() throws Exception {
		int value = Settings.getInstance().getThumbnailDimension();
		assertThat(value, is(500));
	}

	@Test
	public void testGetLoaderOutQueueSizeDefaultValue() throws Exception {
		int value = Settings.getInstance().getLoaderOutQueueSize();
		assertThat(value, is(400));
	}

	@Test
	public void testValidateSettingsDefaultValue() throws Exception {
		boolean isValid = Settings.getInstance().validateSettings();
		assertThat(isValid, is(true));
	}
}
