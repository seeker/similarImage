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

import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * Parses command line parameters and executes the corresponding tasks.
 * 
 * @author Nicholas Wright
 */
public class ArgumentPasrser {
	private static final Logger LOGGER = LoggerFactory.getLogger(ArgumentPasrser.class);
	
	private enum CommandLineOptions {
		path, update,
	};

	private final FileVisitor<Path> visitor;
	private final ArgumentParser parser;

	private static String enumToString(CommandLineOptions optionEnum) {
		return optionEnum.toString();
	}

	/**
	 * Setup the command line parser.
	 * 
	 * @param visitor
	 *            visitor to use processing files
	 */
	public ArgumentPasrser(FileVisitor<Path> visitor) {
		this.visitor = visitor;

		// TODO add hash selection

		parser = ArgumentParsers.newArgumentParser("SimilarImage CLI").defaultHelp(true)
				.description("Limited set of SimilarImage functions");
		parser.addArgument("--" + enumToString(CommandLineOptions.update)).action(Arguments.storeTrue())
				.help("Check extended attributes and update if missing or invalid");
		parser.addArgument(enumToString(CommandLineOptions.path)).metavar("P").nargs("*").type(String.class)
				.help("Process all files in the given directory");
	}

	/**
	 * Parse command line arguments and execute tasks accordingly.
	 * 
	 * @param args
	 *            command line parameters
	 * @throws ArgumentParserException
	 *             if there is an error parsing command line arguments
	 */
	public void parseArgs(String[] args) throws ArgumentParserException {
		Namespace parsedArgs = parser.parseArgs(args);

		if (parsedArgs.getBoolean(enumToString(CommandLineOptions.update))) {
			for (Object path : parsedArgs.getList(enumToString(CommandLineOptions.path))) {
				try {
					Files.walkFileTree(Paths.get((String) path), visitor);
				} catch (IOException e) {
					LOGGER.error("Failed to walk {}: {}", e.toString());
				}
			}
		}
	}
}
