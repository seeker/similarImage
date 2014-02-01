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

import com.github.dozedoff.commonj.io.DataProducer;
import com.github.dozedoff.commonj.util.Pair;

public class SimpleBufferStrategy extends AbstractBufferStrategy<Path, Pair<Path, BufferedImage>> {

	public SimpleBufferStrategy(DataProducer<Path, Pair<Path, BufferedImage>> producer, LinkedBlockingQueue<Path> input,
			LinkedBlockingQueue<Pair<Path, BufferedImage>> output, int outputCapacity) {
		super(producer, input, output, outputCapacity);
	}

	@Override
	public void bufferCheck() {
	}

	@Override
	public boolean workAvailable() {
		return (!output.isEmpty());
	}
}
