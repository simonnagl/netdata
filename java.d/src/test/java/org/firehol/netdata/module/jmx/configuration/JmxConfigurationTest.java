package org.firehol.netdata.module.jmx.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;

import org.firehol.netdata.plugin.configuration.ConfigurationService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;

public class JmxConfigurationTest {

	// We cannot instantiate ConfigurationService here because it depends on an
	// environment Variable.

	@Rule
	public TemporaryFolder tmpFolder = new TemporaryFolder();

	@Rule
	public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

	public static final File defaultJmxConfFile = new File("../conf.d/java.d/jmx.conf");

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
		assumeTrue("Default configuration file not available: " + defaultJmxConfFile, defaultJmxConfFile.isFile());

		// Object under test
		ConfigurationService configService = ConfigurationService.getInstance();

		// Copy default configuration to a temporary file.
		File testConfigurationFile = new File(tmpFolder.newFolder("java.d"), "jmx.conf");
		Files.copy(defaultJmxConfFile.toPath(), testConfigurationFile.toPath());

		// Test
		JmxModuleConfiguration defaultConfig = configService.readPluginConfiguration("jmx",
				JmxModuleConfiguration.class);

		// Verify
		assertNotNull(defaultConfig);
		assertEquals(Collections.emptyList(), defaultConfig.getJmxServers());
		assertNotEquals(Collections.emptyList(), defaultConfig.getCommonCharts());
		assertEquals(true, defaultConfig.isAutoDetectLocalVirtualMachines());
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
		JmxModuleConfiguration invalidConfiguration = configService.readPluginConfiguration("jmx",
				JmxModuleConfiguration.class);
		assertEquals(Collections.emptyList(), invalidConfiguration.getJmxServers());
		assertEquals(Collections.emptyList(), invalidConfiguration.getCommonCharts());
	}

}
