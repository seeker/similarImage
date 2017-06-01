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
package com.github.dozedoff.similarImage.module;

import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.github.dozedoff.similarImage.component.MainScope;
import com.github.dozedoff.similarImage.io.Statistics;

import dagger.Module;
import dagger.Provides;

@Module
public class StatisticsModule {
	public static final String METRICS_LOGGER_NAME = "similarImage.metrics";
	
	@MainScope
	@Provides
	public Statistics provideStatistics() {
		return new Statistics();
	}
	
	@MainScope
	@Provides
	public MetricRegistry provideMetricRegistry() {
		return new MetricRegistry();
	}

	@Provides
	public Slf4jReporter provideReporter(MetricRegistry metrics) {
		return Slf4jReporter.forRegistry(metrics).outputTo(LoggerFactory.getLogger(METRICS_LOGGER_NAME))
				.convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build();
	}
}
