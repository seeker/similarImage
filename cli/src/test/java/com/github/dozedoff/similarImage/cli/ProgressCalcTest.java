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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.github.dozedoff.similarImage.io.Statistics;

//FIXME Silent runner is just a band-aid to get the tests to run
@RunWith(MockitoJUnitRunner.Silent.class)
public class ProgressCalcTest {
	private static final int TOTAL_FILE_COUNT = 210;
	private static final int PROCESSED_FILE_COUNT = 30;
	private static final int FAILED_FILE_COUNT = 20;

	private static final double TOTAL_PROGRESS_PERCENT = 23.809;
	private static final double CORRUPT_PERCENT = 9.523;

	private static final double ALLOWED_COMAPRE_ERROR = 0.0009;

	@Mock
	private Statistics statistics;

	private MetricRegistry metrics;

	private ProgressCalc cut;

	@Before
	public void setUp() throws Exception {
		when(statistics.getFoundFiles()).thenReturn(TOTAL_FILE_COUNT);
		when(statistics.getProcessedFiles()).thenReturn(PROCESSED_FILE_COUNT);
		when(statistics.getFailedFiles()).thenReturn(FAILED_FILE_COUNT);

		metrics = new MetricRegistry();

		cut = new ProgressCalc(metrics);

		setCounter(ProgressCalc.METRIC_NAME_FOUND, TOTAL_FILE_COUNT);
		setCounter(ProgressCalc.METRIC_NAME_PROCESSED, PROCESSED_FILE_COUNT);
		setCounter(ProgressCalc.METRIC_NAME_FAILED, FAILED_FILE_COUNT);
	}

	private void setCounter(String name, long value) {
		Counter counter = metrics.getCounters().get(name);

		counter.dec(counter.getCount());
		counter.inc(value);
	}

	@Test
	public void testTotalProgressPercent() throws Exception {
		assertThat(cut.totalProgressPercent(), closeTo(TOTAL_PROGRESS_PERCENT, ALLOWED_COMAPRE_ERROR));
	}

	@Test
	public void testTotalProgressPercentWithNoFiles() throws Exception {
		setCounter(ProgressCalc.METRIC_NAME_FOUND, 0);
		setCounter(ProgressCalc.METRIC_NAME_PROCESSED, 0);

		assertThat(cut.totalProgressPercent(), is(0.0));
	}

	@Test
	public void testCorruptPercent() throws Exception {
		assertThat(cut.corruptPercent(), closeTo(CORRUPT_PERCENT, ALLOWED_COMAPRE_ERROR));
	}

	@Test
	public void testCorruptPercentWithNoFiles() throws Exception {
		setCounter(ProgressCalc.METRIC_NAME_FOUND, 0);
		setCounter(ProgressCalc.METRIC_NAME_FAILED, 0);

		assertThat(cut.corruptPercent(), is(0.0));
	}

	@Test
	public void testToString() throws Exception {
		Locale.setDefault(Locale.US);
		assertThat(cut.toString(), is("Total progress: 23.81%, corrupt images: 9.52%, files per second processed: 0.00"));
	}
}
