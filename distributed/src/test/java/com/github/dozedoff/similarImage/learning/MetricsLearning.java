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
package com.github.dozedoff.similarImage.learning;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

public class MetricsLearning {
	private static final int WORKER_COUNT = 10;

	private MetricRegistry metrics;
	private String meterName;

	@Before
	public void setUp() {
		metrics = new MetricRegistry();
		meterName = MetricRegistry.name(MetricsLearning.class, "test");
	}

	@Test
	public void testMultipleIdenticalMeterCount() throws Exception {
		for (int i = 0; i < WORKER_COUNT; i++) {
			metrics.meter(meterName);
		}

		assertThat(metrics.getMeters().size(), is(1));
	}

	@Test
	public void testMultipleIdenticalMeterMark() throws Exception {
		for (int i = 0; i < WORKER_COUNT; i++) {
			Meter meter = metrics.meter(meterName);
			meter.mark();
		}

		assertThat(metrics.getMeters().get(meterName).getCount(), is((long) WORKER_COUNT));
	}
}
