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
package com.github.dozedoff.similarImage.thread.pipeline;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.db.Tag;
import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;

@RunWith(MockitoJUnitRunner.class)
public class ImageQueryPipelineBuilderTest {
	private static final int DISTANCE = 42;

	@Mock
	private ImageRepository imageRepository;

	@Mock
	private FilterRepository filterRepository;

	@InjectMocks
	private ImageQueryPipelineBuilder imageQueryPipelineBuilder;

	private ImageQueryPipelineBuilder cut;

	@Before
	public void setUp() throws Exception {
		cut = new ImageQueryPipelineBuilder(imageRepository);
	}

	@Test
	public void testImagesWithIgnore() throws Exception {
		cut.build().apply(null);

		verify(imageRepository).getAll();
	}

	@Test
	public void testImagesLimitedByScopeWithIgnore() throws Exception {
		cut.excludeIgnored().build().apply(null);

		verify(imageRepository).getAllWithoutIgnored();
	}

	@Test
	public void testRemoveSingleImageGroups() throws Exception {
		assertThat(cut.removeSingleImageGroups().build().getPostProcessingStages(),
				hasItem(instanceOf(RemoveSingleImageSetStage.class)));
	}

	@Test
	public void testRemoveDuplicateGroups() throws Exception {
		assertThat(cut.removeDuplicateGroups().build().getPostProcessingStages(),
				hasItem(instanceOf(RemoveDuplicateSetStage.class)));
	}

	@Test
	public void testNewBuilder() throws Exception {
		assertThat(ImageQueryPipelineBuilder.newBuilder(imageRepository),
				is(instanceOf(ImageQueryPipelineBuilder.class)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDistanceNegative() throws Exception {
		cut.distance(-1).build();
	}

	@Test
	public void testDistanceSet() throws Exception {
		ImageQueryPipeline pipeline = cut.distance(DISTANCE).build();
		GroupImagesStage grouper = (GroupImagesStage) pipeline.getImageGrouper();

		assertThat(grouper.getHammingDistance(), is(DISTANCE));
	}

	@Test
	public void testGroupByTagGrouper() throws Exception {
		ImageQueryPipeline pipeline = cut.groupByTag(filterRepository, new Tag("")).build();

		assertThat(pipeline.getImageGrouper(), is(instanceOf(GroupByTagStage.class)));
	}
}
