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
package com.github.dozedoff.similarImage.handler;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientSession;

import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.io.HashAttribute;
import com.github.dozedoff.similarImage.io.Statistics;
import com.github.dozedoff.similarImage.messaging.ArtemisSession;

public class HandlerCollectionFactory {
	private final ImageRepository imageRepository;
	private final Statistics statistics;
	private final ClientSession session;

	public HandlerCollectionFactory(ImageRepository imageRepository, Statistics statistics, ClientSession session) {
		this.imageRepository = imageRepository;
		this.statistics = statistics;
		this.session = session;
	}

	public Collection<HashHandler> withExtendedAttributeSupport(HashAttribute hashAttribute) throws ActiveMQException {
		Collection<HashHandler> handlers = new LinkedList<HashHandler>();

		handlers.add(new DatabaseHandler(imageRepository, statistics));
		handlers.add(new ExtendedAttributeHandler(hashAttribute, imageRepository));
		handlers.add(new ArtemisHashProducer(session, ArtemisSession.ADDRESS_HASH_QUEUE));

		return handlers;
	}

	public Collection<HashHandler> noExtendedAttributeSupport(HashAttribute hashAttribute) throws ActiveMQException {
		Collection<HashHandler> handlers = new LinkedList<HashHandler>();

		handlers.add(new DatabaseHandler(imageRepository, statistics));
		handlers.add(new ArtemisHashProducer(session, ArtemisSession.ADDRESS_HASH_QUEUE));

		return handlers;
	}
}
