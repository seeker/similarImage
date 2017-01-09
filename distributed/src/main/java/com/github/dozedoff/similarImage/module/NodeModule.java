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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import javax.inject.Named;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.handler.HashNames;
import com.github.dozedoff.similarImage.image.ImageResizer;
import com.github.dozedoff.similarImage.io.ExtendedAttribute;
import com.github.dozedoff.similarImage.io.ExtendedAttributeDirectoryCache;
import com.github.dozedoff.similarImage.io.ExtendedAttributeQuery;
import com.github.dozedoff.similarImage.io.HashAttribute;
import com.github.dozedoff.similarImage.messaging.ArtemisQueue.QueueAddress;
import com.github.dozedoff.similarImage.messaging.HasherNode;
import com.github.dozedoff.similarImage.messaging.QueryMessage;
import com.github.dozedoff.similarImage.messaging.ResizerNode;

import dagger.Module;
import dagger.Provides;

@Module
public class NodeModule {
	private static final int IMAGE_SIZE = 32;
	private static final Logger LOGGER = LoggerFactory.getLogger(NodeModule.class);

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

	@Provides
	public ExtendedAttributeQuery provideExtendedAttributeQuery( ){
		return new ExtendedAttributeDirectoryCache(new ExtendedAttribute());
	}

	@Provides
	@Named("pending")
	public List<Path> providePendingPaths(QueryMessage queryMessage) {
		try {
			List<String> strings = queryMessage.pendingImagePaths();
			List<Path> paths = new LinkedList<Path>();

			strings.forEach(new Consumer<String>() {

				@Override
				public void accept(String t) {
					paths.add(Paths.get(t));
				}
			});

			return paths;
		} catch (Exception e) {
			LOGGER.warn("Failed to get pending image paths: {}", e.toString());
			return Collections.emptyList();
		}
	}

	@Provides
	public HashAttribute provideHashAttribute() {
		return new HashAttribute(HashNames.DEFAULT_DCT_HASH_2);
	}
}
