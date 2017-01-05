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

import javax.inject.Named;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientSession;

import com.codahale.metrics.MetricRegistry;
import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.image.ImageResizer;
import com.github.dozedoff.similarImage.messaging.ArtemisQueue.QueueAddress;
import com.github.dozedoff.similarImage.messaging.HasherNode;
import com.github.dozedoff.similarImage.messaging.ResizerNode;

import dagger.Module;
import dagger.Provides;

@Module
public class NodeModule {
	private static final int IMAGE_SIZE = 32;

	@Provides
	public HasherNode provideHasherNode(MetricRegistry metrics, @Named("normal") ClientSession session) {
		try {
			return new HasherNode(session, new ImagePHash(), QueueAddress.HASH_REQUEST.toString(),
					QueueAddress.RESULT.toString(), metrics);
		} catch (ActiveMQException e) {
			throw new RuntimeException("Failed to create " + HasherNode.class.getSimpleName(), e);
		}
	}

	@Provides
	public ResizerNode provideResizerNode(@Named("normal")ClientSession session, MetricRegistry metrics) {
		return new ResizerNode(session, new ImageResizer(IMAGE_SIZE), metrics);
	}
}
