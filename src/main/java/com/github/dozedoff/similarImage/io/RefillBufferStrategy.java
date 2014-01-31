/*  Copyright (C) 2014  Nicholas Wright
    
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
package com.github.dozedoff.similarImage.io;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.util.Pair;

public class RefillBufferStrategy extends AbstractBufferStrategy {
	private static final Logger logger = LoggerFactory.getLogger(RefillBufferStrategy.class);
	private final int MAX_WAIT_TIME = 10000;

	public RefillBufferStrategy(LinkedBlockingQueue<Path> input, LinkedBlockingQueue<Pair<Path, BufferedImage>> output, int outputCapacity) {
		super(input, output, outputCapacity);
	}

	@Override
	public void bufferCheck() {
		if (isBufferLow() && (!input.isEmpty())) {
			synchronized (output) {
				logger.debug("Low buffer, suspending drain");

				try {
					output.wait(MAX_WAIT_TIME);
				} catch (InterruptedException e) {
					logger.debug("Max wait has timed out, resuming drain");
					return;
				}

				logger.debug("Buffer re-filled, resuming drain");
			}
		}
	}

	private boolean isBufferLow() {
		return output.size() < (float) outputCapacity * 0.10f;
	}

	private boolean isBufferFilled() {
		return output.size() > (float) outputCapacity * 0.90f;
	}

	@Override
	public boolean workAvailable() {
		return (isBufferFilled() || input.isEmpty());
	}
}
