package org.firehol.netdata.plugin.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.firehol.netdata.plugin.configuration.schema.PluginDaemonConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;

public class PluginDaemonConfigurationTest {

	// We cannot instantiate ConfigurationService here because it depends on an
	// environment Variable.

	@Rule
	public TemporaryFolder tmpFolder = new TemporaryFolder();

	@Rule
	public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

	public static final File defaultPluginDaemonConfFile = new File("../conf.d/java.d.conf");

	@Before
	public void setUp() throws IOException {
		environmentVariables.set("NETDATA_CONFIG_DIR", tmpFolder.getRoot().toString());
	}

	/**
	 * Test reading default {@code JmxModuleConfiguration} from {@code conf.d}
	 * folder.
	 */
	@Test
	public void testReadDefaultConfiguration() throws Exception {
		assumeTrue("Default configuration file not available: " + defaultPluginDaemonConfFile,
				defaultPluginDaemonConfFile.isFile());

		// Object under test
		ConfigurationService configService = ConfigurationService.getInstance();

		// Copy default configuration to a temporary file.
		File testConfigurationFile = new File(tmpFolder.getRoot(), "java.d.conf");
		Files.copy(defaultPluginDaemonConfFile.toPath(), testConfigurationFile.toPath());

		// Test
		PluginDaemonConfiguration defaultConfig = configService.readConfiguration(testConfigurationFile,
				PluginDaemonConfiguration.class);

		// Verify
		assertNotNull(defaultConfig);
		assertEquals(false, defaultConfig.getLogFullStackTraces());
		// assertNotEquals(0, defaultConfig.getModules().size());
	}

	/**
	 * Test reading invalid {@code JmxModuleConfiguration}
	 */
	@Test
	public void testReadConfigurationInvalid() throws Exception {
		// Object under test
		ConfigurationService configService = ConfigurationService.getInstance();

		// Write a configuration to a temporary file.
		File testConfigurationFile = new File(tmpFolder.newFolder("java.d"), "jmx.conf");
		Files.write(testConfigurationFile.toPath(), "{ \"noClassProperty\": \"testValue\" }".getBytes("UTF-8"));

		// Test
		PluginDaemonConfiguration invalidConfig = configService.readConfiguration(testConfigurationFile,
				PluginDaemonConfiguration.class);
		assertNull(invalidConfig.getLogFullStackTraces());
		// assertEquals(0, invalidConfig.getModules().size());
	}

}
