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
package com.github.dozedoff.similarImage.module;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;

import javax.inject.Singleton;

import org.cfg4j.provider.ConfigurationProvider;
import org.cfg4j.provider.ConfigurationProviderBuilder;
import org.cfg4j.source.ConfigurationSource;
import org.cfg4j.source.compose.FallbackConfigurationSource;
import org.cfg4j.source.compose.MergeConfigurationSource;
import org.cfg4j.source.context.filesprovider.ConfigFilesProvider;
import org.cfg4j.source.files.FilesConfigurationSource;
import org.cfg4j.source.inmemory.InMemoryConfigurationSource;

import com.github.dozedoff.similarImage.app.MainSetting;

import dagger.Module;
import dagger.Provides;

@Module
public class Cfg4jModule {
	/**
	 * Provides a list of configuration file paths
	 * 
	 * @return a list of configuration files
	 */
	@Singleton
	@Provides
	public ConfigFilesProvider provideConfigFilesProvider() {
		return () -> Arrays.asList(Paths.get("").toAbsolutePath().resolve("similarImage.yml"));
	}

	/**
	 * Default settings if no configuration is provided.
	 * 
	 * @return default configuration property
	 */
	@Singleton
	@Provides
	public Properties providesDefaultConfiguration() {
		Properties props = new Properties();

		props.put("all.threads", Runtime.getRuntime().availableProcessors());
		props.put("all.includeIgnoredImages", false);

		return props;
	}

	/**
	 * Provides a configuration source for this application.
	 * 
	 * @param configFilesProvider
	 *            contains a list of configuration files
	 * @param defaultConfig
	 *            default configuration
	 * @return a classpath based configuration provider
	 */
	@Singleton
	@Provides
	public ConfigurationSource provideConfigurationSource(ConfigFilesProvider configFilesProvider,
			Properties defaultConfig) {

		FilesConfigurationSource files = new FilesConfigurationSource(configFilesProvider);
		InMemoryConfigurationSource memory = new InMemoryConfigurationSource(defaultConfig);

		return new MergeConfigurationSource(memory, new FallbackConfigurationSource(files, memory));
	}

	/**
	 * Provides the configuration for this application
	 * 
	 * @param configSource
	 *            where the configuration is read from
	 * @return a initialized configuration instance
	 */
	@Singleton
	@Provides
	public ConfigurationProvider provideConfigurationProvider(ConfigurationSource configSource) {
		return new ConfigurationProviderBuilder().withConfigurationSource(configSource).build();
	}

	/**
	 * Provides the main application settings
	 * 
	 * @param configurationProvider
	 *            the current configuration
	 * @return values for the configuration
	 */
	@Singleton
	@Provides
	public MainSetting provideMainSetting(ConfigurationProvider configurationProvider) {
		return configurationProvider.bind("all", MainSetting.class);
	}
}
