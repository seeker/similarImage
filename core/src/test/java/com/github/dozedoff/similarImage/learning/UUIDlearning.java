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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

public class UUIDlearning {
	private static final Logger LOGGER = LoggerFactory.getLogger(UUIDlearning.class);
	private UUID uuid;

	@Before
	public void setUp() {
		uuid = UUID.randomUUID();
	}

	@Test
	public void testDisplay() {
		LOGGER.info("Generated uuid: {}", uuid);
	}

	@Test
	public void testStringSize() {
		LOGGER.info("UUID string size: {} bits", uuid.toString().getBytes().length * 8);
	}

	@Test
	public void testPathSize() throws Exception {
		LOGGER.info("Path string size: {} bits",
				"/tmp/foo/bar/baz/a/this is a test/to see how long paths compare/in size".getBytes().length * 8);
	}

	@Test
	public void testGenerationBenchmark() throws Exception {
		int sampleCount = 1000;
		List<UUID> uuids = new ArrayList<>(sampleCount);

		Stopwatch sw = Stopwatch.createStarted();
		for (int i = 0; i < sampleCount; i++) {
			uuids.add(UUID.randomUUID());
		}

		sw.stop();
		LOGGER.info("Generated {} uuids in {}", uuids.size(), sw);
	}
}
