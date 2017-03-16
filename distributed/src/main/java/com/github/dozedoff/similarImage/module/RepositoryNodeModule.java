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

import javax.inject.Named;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientSession;

import com.github.dozedoff.similarImage.messaging.ArtemisQueue.QueueAddress;
import com.github.dozedoff.similarImage.messaging.MessageCollector;
import com.github.dozedoff.similarImage.messaging.QueueToDatabaseTransaction;
import com.github.dozedoff.similarImage.messaging.ResultMessageSink;

import dagger.Module;
import dagger.Provides;

@Module
public class RepositoryNodeModule {
	private static final int COLLECTED_MESSAGE_THRESHOLD = 100;
	/**
	 * Time in milliseconds
	 */
	private static final long COLLECTED_MESSAGE_DRAIN_INTERVAL = TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS);

	@Provides
	public MessageCollector provideMessageCollector(QueueToDatabaseTransaction qdt) {
		return new MessageCollector(COLLECTED_MESSAGE_THRESHOLD, qdt);
	}

	@Provides
	public ResultMessageSink providesResultMessageSink(@Named("transacted") ClientSession transactedSession,
			MessageCollector collector) {
		try {
			return new ResultMessageSink(transactedSession, collector, QueueAddress.RESULT.toString(),
					COLLECTED_MESSAGE_DRAIN_INTERVAL);
		} catch (ActiveMQException e) {
			throw new RuntimeException("Failed to create message sink", e);
		}
	}
}
