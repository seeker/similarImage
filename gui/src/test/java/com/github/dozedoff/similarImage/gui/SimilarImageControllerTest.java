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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;

import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.event.GuiEventBus;
import com.github.dozedoff.similarImage.event.GuiGroupEvent;
import com.github.dozedoff.similarImage.io.Statistics;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

@RunWith(MockitoJUnitRunner.class)
public class SimilarImageControllerTest {
	private static final long TEST_KEY = 42L;

	@Mock
	private ResultGroupView displayGroup;

	@Mock
	private ImageRepository imageRepository;

	@Mock
	private Statistics statistics;

	@Mock
	private ExecutorService threadPool;

	@Mock
	private SimilarImageView gui;

	@InjectMocks
	private SimilarImageController cut;

	private ImageRecord testRecord;

	@Before
	public void setUp() {
		cut.setGui(gui);
		testRecord = new ImageRecord("foo", TEST_KEY);
	}

	@Test
	public void testGroupUpdateEvent() {
		Multimap<Long, ImageRecord> groups = MultimapBuilder.hashKeys().hashSetValues().build();
		groups.put(TEST_KEY, testRecord);

		GuiEventBus.getInstance().post(new GuiGroupEvent(groups));

		assertThat(cut.getGroup(TEST_KEY), hasItem(testRecord));
	}
}
