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

import javax.inject.Inject;

import org.apache.activemq.artemis.core.config.FileDeploymentManager;
import org.apache.activemq.artemis.core.config.impl.FileConfiguration;

import dagger.Module;
import dagger.Provides;

@Module
public class ServerConfigurationModule {
	private Path serverWorkingDirectory;

	@Inject
	public ServerConfigurationModule() {
		serverWorkingDirectory = Paths.get("");
	}

	public ServerConfigurationModule(Path serverWorkingDirectory) {
		this.serverWorkingDirectory = serverWorkingDirectory;
	}

	@Provides
	public FileConfiguration provideServerConfiguration() {
		try {
			FileConfiguration config = new FileConfiguration();
			FileDeploymentManager fdm = new FileDeploymentManager("broker.xml");
			fdm.addDeployable(config);
			fdm.readConfiguration();
			config.setBrokerInstance(serverWorkingDirectory.toFile());

			return config;
		} catch (Exception e) {
			throw new RuntimeException("Failed to create server configuration");
		}
	}
}
