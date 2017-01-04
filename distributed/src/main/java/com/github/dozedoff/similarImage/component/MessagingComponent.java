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
package com.github.dozedoff.similarImage.component;

import com.codahale.metrics.MetricRegistry;
import com.github.dozedoff.similarImage.io.Statistics;
import com.github.dozedoff.similarImage.messaging.ArtemisEmbeddedServer;
import com.github.dozedoff.similarImage.messaging.ArtemisSession;
import com.github.dozedoff.similarImage.module.ServerConfigurationModule;
import com.github.dozedoff.similarImage.module.ServerConnectionModule;
import com.github.dozedoff.similarImage.module.StatisticsModule;

import dagger.Component;

@MainScope
@Component(modules = { ServerConnectionModule.class,
		ServerConfigurationModule.class, StatisticsModule.class }, dependencies = CoreComponent.class)
public interface MessagingComponent {
	ArtemisSession getSessionModule();

	ArtemisEmbeddedServer getServer();

	// TODO remove methods below, temporary for refactoring

	MetricRegistry getMetricRegistry();

	Statistics getStatistics();

}
