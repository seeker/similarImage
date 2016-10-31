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
package com.github.dozedoff.similarImage.cli;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.io.Statistics;

@RunWith(MockitoJUnitRunner.class)
public class ProgressCalcTest {
	private static final int TOTAL_FILE_COUNT = 210;
	private static final int PROCESSED_FILE_COUNT = 30;
	private static final int FAILED_FILE_COUNT = 20;

	private static final double TOTAL_PROGRESS_PERCENT = 23.809;
	private static final double CORRUPT_PERCENT = 9.523;

	private static final double ALLOWED_COMAPRE_ERROR = 0.0009;

	@Mock
	private Statistics statistics;
	@InjectMocks
	private ProgressCalc cut;

	@Before
	public void setUp() throws Exception {
		when(statistics.getFoundFiles()).thenReturn(TOTAL_FILE_COUNT);
		when(statistics.getProcessedFiles()).thenReturn(PROCESSED_FILE_COUNT);
		when(statistics.getFailedFiles()).thenReturn(FAILED_FILE_COUNT);
	}

	@Test
	public void testTotalProgressPercent() throws Exception {
		assertThat(cut.totalProgressPercent(), closeTo(TOTAL_PROGRESS_PERCENT, ALLOWED_COMAPRE_ERROR));
	}

	@Test
	public void testTotalProgressPercentWithNoFiles() throws Exception {
		when(statistics.getFoundFiles()).thenReturn(0);
		when(statistics.getProcessedFiles()).thenReturn(0);

		assertThat(cut.totalProgressPercent(), is(0.0));
	}

	@Test
	public void testCorruptPercent() throws Exception {
		assertThat(cut.corruptPercent(), closeTo(CORRUPT_PERCENT, ALLOWED_COMAPRE_ERROR));
	}

	@Test
	public void testCorruptPercentWithNoFiles() throws Exception {
		when(statistics.getFoundFiles()).thenReturn(0);
		when(statistics.getFailedFiles()).thenReturn(0);

		assertThat(cut.corruptPercent(), is(0.0));
	}

	@Test
	public void testToString() throws Exception {
		assertThat(cut.toString(), is("Total progress: 23.81%, corrupt images: 9.52%"));
	}
}
