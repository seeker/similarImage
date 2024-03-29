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

import org.junit.Rule;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.quality.Strictness;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.google.common.collect.Multimap;

public class ImageQueryPipelineTest {
	public @Rule MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

	@Mock
	private Function<Path, List<ImageRecord>> imageQueryStage;

	@Mock
	private GroupImagesStage grouper;

	@Mock
	private Function<Multimap<Long, ImageRecord>, Multimap<Long, ImageRecord>> postProcessingStageA;
	@Mock
	private Function<Multimap<Long, ImageRecord>, Multimap<Long, ImageRecord>> postProcessingStageB;

	@Mock
	private List<ImageRecord> images;

	@Mock
	private Multimap<Long, ImageRecord> groups;

	private ImageQueryPipeline cut;

	@Before
	public void setUp() throws Exception {
		when(imageQueryStage.apply(any())).thenReturn(images);
		when(grouper.apply(images)).thenReturn(groups);
		when(postProcessingStageA.apply(groups)).thenReturn(groups);
		when(postProcessingStageB.apply(groups)).thenReturn(groups);

		cut = new ImageQueryPipeline(imageQueryStage, grouper,
				Arrays.asList(postProcessingStageA, postProcessingStageB));

		cut.apply(null);
	}

	@Test
	public void testQueryExecuted() throws Exception {
		verify(imageQueryStage).apply(any());
	}

	@Test
	public void testGroupingExecuted() throws Exception {
		verify(grouper).apply(images);
	}
	
	@Test
	public void testPostprocessingAExecuted() throws Exception {
		verify(postProcessingStageA).apply(groups);
	}

	@Test
	public void testPostprocessingBExecuted() throws Exception {
		verify(postProcessingStageB).apply(groups);
	}

	@Test
	public void testPipelineReturnsGroups() throws Exception {
		assertThat(cut.apply(null), is(groups));
	}

	@Test
	public void testGrouperInstance() throws Exception {
		assertThat(cut.getImageGrouper(), is(instanceOf(GroupImagesStage.class)));
	}

	@Test
	public void testGetPostProcessingStages() throws Exception {
		assertThat(cut.getPostProcessingStages(), hasSize(2));
	}
}
