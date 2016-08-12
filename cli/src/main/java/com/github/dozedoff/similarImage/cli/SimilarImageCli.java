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
package com.github.dozedoff.similarImage.cli;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.filefilter.SimpleImageFilter;
import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.handler.ExtendedAttributeUpdateHandler;
import com.github.dozedoff.similarImage.handler.HashNames;
import com.github.dozedoff.similarImage.io.HashAttribute;
import com.github.dozedoff.similarImage.io.Statistics;
import com.github.dozedoff.similarImage.thread.ImageFindJobVisitor;

import net.sourceforge.argparse4j.inf.ArgumentParserException;

/**
 * The main class for the SimilarImage command line tool
 * 
 * @author Nicholas Wright
 *
 */
public class SimilarImageCli {
	private static final Logger LOGGER = LoggerFactory.getLogger(SimilarImageCli.class);

	private final ArgumentPasrser parser;

	/**
	 * Setup an instance for parsing command line arguments and hashing files.
	 */
	public SimilarImageCli() {
		ImageFindJobVisitor visitor = new ImageFindJobVisitor(new SimpleImageFilter(), Arrays.asList(
				new ExtendedAttributeUpdateHandler(new HashAttribute(HashNames.DEFAULT_DCT_HASH_2), new ImagePHash())),
				new Statistics());
		parser = new ArgumentPasrser(visitor);
	}

	/**
	 * Get the configured parser instance.
	 * 
	 * @return a parser instance
	 */
	public final ArgumentPasrser getParser() {
		return parser;
	}

	/**
	 * The main program method.
	 * 
	 * @param args
	 *            command line arguments
	 */
	public static void main(String[] args) {
		try {
			new SimilarImageCli().getParser().parseArgs(args);
		} catch (ArgumentParserException e) {
			LOGGER.error("Failed to parse arguments: {}", e.toString());
		}
	}
}
