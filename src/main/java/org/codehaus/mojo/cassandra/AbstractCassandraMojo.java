/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.codehaus.mojo.cassandra;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.OS;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

/**
 * Base class for all the Cassandra Maven Plugin goals.
 *
 * @author stephenc
 */
public abstract class AbstractCassandraMojo extends AbstractMojo {
    /**
     * The directory to hold cassandra's database.
     */
    @Parameter(defaultValue = "${project.build.directory}/cassandra")
    protected File cassandraDir;

    /**
     * The enclosing project.
     */
    @Parameter(readonly = true)
    protected MavenProject project;

    /**
     * The directory containing generated classes.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
    private File classesDirectory;

    /**
     * The directory containing generated test classes.
     */
    @Parameter(defaultValue = "${project.build.testOutputDirectory}", readonly = true)
    private File testClassesDirectory;

    /**
     * Adds the test classpath to cassandra (for example you could use this when you have a custom comparator on your
     * test classpath.
     */
    @Parameter(defaultValue = "false")
    protected boolean addTestClasspath;

    /**
     * Adds the main classpath to cassandra (for example you could use this when you have a custom comparator on your
     * main classpath.
     *
     */
    @Parameter(defaultValue = "false")
    protected boolean addMainClasspath;

    /**
     * Skip the execution.
     *
     */
    @Parameter(property = "cassandra.skip", defaultValue = "false")
    protected boolean skip;

    @Parameter(defaultValue = "${plugin.artifacts}", readonly = true, required = true)
    private List<Artifact> pluginDependencies;

    @Parameter(defaultValue = "${plugin.pluginArtifact}", readonly = true, required = true)
    private Artifact pluginArtifact;

    /**
     * The current build session instance. This is used for toolchain manager API calls.
     *
     */
    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    protected MavenSession session;

    /**
     * In yaml format any overrides or additional configuration that you want to apply to the server.
     *
     */
    @Parameter
    private String yaml;

    /**
     * Address to use for the RPC interface. Do not change this unless you really know what you are doing.
     *
     */
    @Parameter(defaultValue = "127.0.0.1", required = true)
    protected String rpcAddress;

    /**
     * Port to listen to for the JMX interface.
     *
     */
    @Parameter(property = "cassandra.jmxPort", defaultValue = "7199")
    protected int jmxPort;

    /**
     * Port on which the CQL native transport listens for clients.
     *
     * @since 2.0.0-1
     */
    @Parameter(property = "cassandra.nativeTransportPort", defaultValue = "9042")
    protected int nativeTransportPort;

    /**
     * Enable or disable the native transport server. Currently, only the Thrift
     * server is started by default because the native transport is considered beta.
     *
     * @since 2.0.0-1
     */
    @Parameter(property = "cassandra.startNativeTransport", defaultValue = "true")
    protected boolean startNativeTransport;

    /**
     * <p>
     * Address to bind to and tell other Cassandra nodes to connect to. You
     * <strong>must</strong> change this if you want multiple nodes to be able to
     * communicate!
     * </p>
     * <p>
     * Leaving it blank leaves it up to InetAddress.getLocalHost(). This
     * will always do the Right Thing <em>if</em> the node is properly configured
     * (hostname, name resolution, etc), and the Right Thing is to use the
     * address associated with the hostname (it might not be).
     * </p>
     * Setting this to 0.0.0.0 is always wrong.
     * Do not change this unless you really know what you are doing.
     *
     */
    @Parameter(defaultValue = "127.0.0.1")
    protected String listenAddress;

    /**
     * Port to listen to for the Storage interface.
     *
     */
    @Parameter(property = "cassandra.storagePort", defaultValue = "7000")
    protected int storagePort;

    /**
     * Port to listen to for receiving the stop command over
     *
     */
    @Parameter(property = "cassandra.stopPort", defaultValue = "8081")
    protected int stopPort;

    /**
     * Key to be provided when stopping cassandra
     *
     */
    @Parameter(property = "cassandra.stopKey", defaultValue = "cassandra-maven-plugin")
    protected String stopKey;

    /**
     * Number of megabytes to limit the cassandra JVM to.
     *
     */
    @Parameter(property = "cassandra.maxMemory", defaultValue = "512")
    protected int maxMemory;

    /**
     * The keyspace against which individual operations will be executed
     *
     */
    @Parameter(property = "cassandra.keyspace")
    protected String keyspace;

    /**
     * List of System properties to pass to the JUnit tests.
     * @since 1.2.1-2
     */
    @Parameter
    protected Map<String, String> systemPropertyVariables;

    /**
     * Log level of cassandra process. Logging is performed via log4j2.
     *
     * @since 3.5
     */
    @Parameter(defaultValue = "ERROR")
    protected String logLevel;

    /**
     * Create a jar with just a manifest containing a Main-Class entry for SurefireBooter and a Class-Path entry for
     * all classpath elements. Copied from surefire (ForkConfiguration#createJar())
     *
     * @param jarFile   The jar file to create/update
     * @param mainClass The main class to run.
     * @throws java.io.IOException if something went wrong.
     */
    protected void createCassandraJar(File jarFile, String mainClass) throws IOException {
        createCassandraJar(jarFile, mainClass, cassandraDir);
    }

    protected boolean useJdk11Options() {
        return false;
    }

    /**
     * Create a jar with just a manifest containing a Main-Class entry for SurefireBooter and a Class-Path entry for
     * all classpath elements. Copied from surefire (ForkConfiguration#createJar())
     *
     * @param jarFile   The jar file to create/update
     * @param mainClass The main class to run.
     * @throws java.io.IOException if something went wrong.
     */
    protected void createCassandraJar(File jarFile, String mainClass, File cassandraDir) throws IOException {
        File conf = new File(cassandraDir, "conf");

        try (FileOutputStream fos = new FileOutputStream(jarFile);
                JarOutputStream jos = new JarOutputStream(fos)) {
            jos.setLevel(JarOutputStream.STORED);
            jos.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));

            Manifest man = new Manifest();

            // we can't use StringUtils.join here since we need to add a '/' to
            // the end of directory entries - otherwise the jvm will ignore them.
            StringBuilder cp = new StringBuilder();
            cp.append(new URL(conf.toURI().toASCIIString()).toExternalForm());
            cp.append(' ');
            getLog().debug("Adding plugin artifact: " + ArtifactUtils.versionlessKey(pluginArtifact)
                    + " to the classpath");
            cp.append(new URL(pluginArtifact.getFile().toURI().toASCIIString()).toExternalForm());
            cp.append(' ');

            for (Artifact artifact : this.pluginDependencies) {
                getLog().debug("Adding plugin dependency artifact: " + ArtifactUtils.versionlessKey(artifact)
                        + " to the classpath");
                // NOTE: if File points to a directory, this entry MUST end in '/'.
                cp.append(new URL(artifact.getFile().toURI().toASCIIString()).toExternalForm());
                cp.append(' ');
            }

            if (addMainClasspath || addTestClasspath) {
                if (addTestClasspath) {
                    getLog().debug("Adding: " + testClassesDirectory + " to the classpath");
                    cp.append(new URL(testClassesDirectory.toURI().toASCIIString()).toExternalForm());
                    cp.append(' ');
                }
                if (addMainClasspath) {
                    getLog().debug("Adding: " + classesDirectory + " to the classpath");
                    cp.append(new URL(classesDirectory.toURI().toASCIIString()).toExternalForm());
                    cp.append(' ');
                }
                for (Artifact artifact : (Set<Artifact>) this.project.getArtifacts()) {
                    if ("jar".equals(artifact.getType())
                            && !Artifact.SCOPE_PROVIDED.equals(artifact.getScope())
                            && (!Artifact.SCOPE_TEST.equals(artifact.getScope()) || addTestClasspath)) {
                        getLog().debug("Adding dependency: " + ArtifactUtils.versionlessKey(artifact)
                                + " to the classpath");
                        // NOTE: if File points to a directory, this entry MUST end in '/'.
                        cp.append(new URL(artifact.getFile().toURI().toASCIIString()).toExternalForm());
                        cp.append(' ');
                    }
                }
            }

            man.getMainAttributes().putValue("Manifest-Version", "1.0");
            man.getMainAttributes().putValue("Class-Path", cp.toString().trim());
            man.getMainAttributes().putValue("Main-Class", mainClass);

            man.write(jos);
        }
    }

    /**
     * Creates the cassandra home directory.
     *
     * @throws IOException if something goes wrong.
     */
    protected void createCassandraHome() throws IOException {
        createCassandraHome(cassandraDir, listenAddress, rpcAddress, null, new String[] {listenAddress});
    }

    /**
     * Creates the cassandra home directory.
     *
     * @param cassandraDir the cassandra home directory.
     * @throws IOException if something goes wrong.
     */
    protected void createCassandraHome(
            File cassandraDir, String listenAddress, String rpcAddress, BigInteger initialToken, String[] seeds)
            throws IOException {
        File bin = new File(cassandraDir, "bin");
        File conf = new File(cassandraDir, "conf");
        File data = new File(cassandraDir, "data");
        File commitlog = new File(cassandraDir, "commitlog");
        File cdcRawDirectory = new File(cassandraDir, "cdcRawDirectory");
        if (!Files.exists(cdcRawDirectory.toPath())) {
            Files.createDirectories(cdcRawDirectory.toPath());
        }
        File savedCaches = new File(cassandraDir, "saved_caches");
        for (File dir : Arrays.asList(cassandraDir, bin, conf, data, commitlog, savedCaches)) {
            if (dir.isFile()) {
                getLog().debug("Deleting file " + dir + " as we need to create a directory with the same name.");
                if (!dir.delete()) {
                    getLog().warn("Could not delete file " + dir);
                }
            }
            if (!dir.isDirectory()) {
                getLog().debug("Creating directory " + dir + " as it does not exist.");
                if (!dir.mkdirs()) {
                    getLog().warn("Could not create directory " + dir);
                }
            }
        }
        File cassandraYaml = new File(conf, "cassandra.yaml");
        if (Utils.shouldGenerateResource(project, cassandraYaml)) {
            getLog().debug((cassandraYaml.isFile() ? "Updating " : "Creating ") + cassandraYaml);
            createCassandraYaml(
                    cassandraYaml,
                    data,
                    commitlog,
                    savedCaches,
                    listenAddress,
                    rpcAddress,
                    initialToken,
                    seeds,
                    cdcRawDirectory);
        }
        File log4jServerConfig = new File(conf, "log4j-server.xml");
        if (Utils.shouldGenerateResource(project, log4jServerConfig)) {
            getLog().debug((log4jServerConfig.isFile() ? "Updating " : "Creating ") + log4jServerConfig);
            FileUtils.copyURLToFile(getClass().getResource("/log4j2.xml"), log4jServerConfig);
        }
        File log4jClientConfig = new File(conf, "log4j-client.xml");
        if (Utils.shouldGenerateResource(project, log4jClientConfig)) {
            getLog().debug((log4jClientConfig.isFile() ? "Updating " : "Creating ") + log4jClientConfig);
            FileUtils.copyURLToFile(getClass().getResource("/log4j2.xml"), log4jClientConfig);
        }
        File cassandraJar = new File(bin, "cassandra.jar");
        if (Utils.shouldGenerateResource(project, cassandraJar)) {
            getLog().debug((cassandraJar.isFile() ? "Updating " : "Creating ") + cassandraJar);
            createCassandraJar(cassandraJar, CassandraMonitor.class.getName(), cassandraDir);
        }
        /*
        File nodetoolJar = new File( bin, "nodetool.jar" );
        if ( Utils.shouldGenerateResource( project, nodetoolJar ) )
        {
            getLog().debug( ( nodetoolJar.isFile() ? "Updating " : "Creating " ) + nodetoolJar );
            createCassandraJar( nodetoolJar, NodeCmd.class.getName(), cassandraDir );
        }
        */
    }

    /**
     * Generates the {@code cassandra.yaml} file.
     *
     * @param cassandraYaml the {@code cassandra.yaml} file.
     * @param data          The data directory.
     * @param commitlog     The commitlog directory.
     * @param savedCaches   The saved caches directory.
     * @throws IOException If something went wrong.
     */
    private void createCassandraYaml(
            File cassandraYaml, File data, File commitlog, File savedCaches, File cdcRawDirectory) throws IOException {
        createCassandraYaml(
                cassandraYaml,
                data,
                commitlog,
                savedCaches,
                listenAddress,
                rpcAddress,
                null,
                new String[] {listenAddress},
                cdcRawDirectory);
    }

    /**
     * Generates the {@code cassandra.yaml} file.
     *
     * @param cassandraYaml the {@code cassandra.yaml} file.
     * @param data          The data directory.
     * @param commitlog     The commitlog directory.
     * @param savedCaches   The saved caches directory.
     * @param listenAddress The address to listen on for storage and other cassandra servers.
     * @param rpcAddress    The address to listen on for clients.
     * @param seeds         The seeds.
     * @throws IOException If something went wrong.
     */
    private void createCassandraYaml(
            File cassandraYaml,
            File data,
            File commitlog,
            File savedCaches,
            String listenAddress,
            String rpcAddress,
            BigInteger initialToken,
            String[] seeds,
            File cdcRawDirectory)
            throws IOException {
        String defaults = IOUtil.toString(getClass().getResourceAsStream("/cassandra.yaml"));
        StringBuilder config = new StringBuilder();
        config.append("data_file_directories:\n")
                .append("    - ")
                .append(data.getAbsolutePath())
                .append("\n");
        config.append("commitlog_directory: ").append(commitlog).append("\n");
        config.append("cdc_raw_directory: ").append(cdcRawDirectory).append("\n");
        config.append("saved_caches_directory: ").append(savedCaches).append("\n");
        config.append("initial_token: ")
                .append(initialToken == null || "null".equals(initialToken) ? "" : initialToken.toString())
                .append("\n");
        config.append("listen_address: ").append(listenAddress).append("\n");
        config.append("storage_port: ").append(storagePort).append("\n");
        config.append("rpc_address: ").append(rpcAddress).append("\n");
        config.append("native_transport_port: ").append(nativeTransportPort).append("\n");
        config.append("start_native_transport: ").append(startNativeTransport).append("\n");
        if (seeds != null) {
            config.append("seed_provider: ").append("\n");
            config.append("    - class_name: org.apache.cassandra.locator.SimpleSeedProvider")
                    .append("\n");
            config.append("      parameters:").append("\n");
            String sep = "          - seeds: \"";
            for (int i = 0; i < seeds.length; i++) {
                config.append(sep).append(seeds[i]);
                sep = ", ";
            }
            if (sep.length() == 2) {
                config.append("\"").append("\n");
            }
        }
        FileUtils.fileWrite(
                cassandraYaml.getAbsolutePath(), Utils.merge(Utils.merge(defaults, yaml), config.toString()));
    }

    /**
     * Gets the Java toolchain.
     *
     * @return the Java toolchain.
     */
    protected Toolchain getToolchain() {
        Toolchain tc = null;

        try {
            if (session != null) // session is null in tests..
            {
                ToolchainManager toolchainManager =
                        (ToolchainManager) session.getContainer().lookup(ToolchainManager.ROLE);

                if (toolchainManager != null) {
                    tc = toolchainManager.getToolchainFromBuildContext("jdk", session);
                }
            }
        } catch (ComponentLookupException componentLookupException) {
            // just ignore, could happen in pre-2.0.9 builds..
        }
        return tc;
    }

    /**
     * Create a {@link CommandLine} to launch Java.
     *
     * @return a {@link CommandLine} to launch Java.
     */
    protected CommandLine newJavaCommandLine() {
        String exec = null;
        Toolchain tc = getToolchain();

        // if the file doesn't exist & toolchain is null, java is probably in the PATH...
        // we should probably also test for isFile and canExecute, but the second one is only
        // available in SDK 6.
        if (tc != null) {
            getLog().info("Toolchain in cassandra-maven-plugin: " + tc);
            exec = tc.findTool("java");
        } else {
            if (OS.isFamilyWindows()) {
                String ex = "java.exe";
                // now try to figure the path from PATH, PATHEXT env vars
                // if bat file, wrap in cmd /c
                String path = System.getenv("PATH");
                if (path != null) {
                    for (String elem : StringUtils.split(path, File.pathSeparator)) {
                        File f = new File(new File(elem), ex);
                        if (f.exists()) {
                            exec = ex;
                            break;
                        }
                    }
                }
            }
        }

        if (exec == null) {
            exec = "java";
        }

        return new CommandLine(exec);
    }

    /**
     * Creates the environment required when launching Cassandra or the CLI tools.
     *
     * @return the environment required when launching Cassandra or the CLI tools.
     */
    protected Map<String, String> createEnvironmentVars() {
        Map<String, String> enviro = new HashMap(System.getenv());
        enviro.put("CASSANDRA_CONF", new File(cassandraDir, "conf").getAbsolutePath());
        return enviro;
    }

    /**
     * Creates the command line to launch the cassandra server.
     *
     * @return the command line to launch the cassandra server.
     * @throws IOException if there are issues creating the cassandra home directory.
     */
    protected CommandLine newServiceCommandLine() throws IOException {
        return newServiceCommandLine(
                cassandraDir, listenAddress, rpcAddress, null, new String[] {listenAddress}, true, jmxPort);
    }

    /**
     * Creates the command line to launch the cassandra server.
     *
     * @return the command line to launch the cassandra server.
     * @throws IOException if there are issues creating the cassandra home directory.
     */
    protected CommandLine newServiceCommandLine(
            File cassandraDir,
            String listenAddress,
            String rpcAddress,
            BigInteger initialToken,
            String[] seeds,
            boolean jmxRemoteEnabled,
            int jmxPort)
            throws IOException {
        createCassandraHome(cassandraDir, listenAddress, rpcAddress, initialToken, seeds);
        CommandLine commandLine = newJavaCommandLine();
        commandLine.addArgument("-Xmx" + maxMemory + "m");

        if (useJdk11Options()) {
            commandLine.addArgument("-Djdk.attach.allowAttachSelf=true");
            commandLine.addArgument("--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED");
            commandLine.addArgument("--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED");
            commandLine.addArgument("--add-exports=java.base/sun.nio.ch=ALL-UNNAMED");
            commandLine.addArgument("--add-exports=java.management.rmi/com.sun.jmx.remote.internal.rmi=ALL-UNNAMED");
            commandLine.addArgument("--add-exports=java.rmi/sun.rmi.registry=ALL-UNNAMED");
            commandLine.addArgument("--add-exports=java.rmi/sun.rmi.server=ALL-UNNAMED");
            commandLine.addArgument("--add-exports=java.sql/java.sql=ALL-UNNAMED");
            commandLine.addArgument("--add-opens=java.base/java.lang.module=ALL-UNNAMED");
            commandLine.addArgument("--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED");
            commandLine.addArgument("--add-opens=java.base/jdk.internal.ref=ALL-UNNAMED");
            commandLine.addArgument("--add-opens=java.base/jdk.internal.reflect=ALL-UNNAMED");
            commandLine.addArgument("--add-opens=java.base/jdk.internal.math=ALL-UNNAMED");
            commandLine.addArgument("--add-opens=java.base/jdk.internal.module=ALL-UNNAMED");
            commandLine.addArgument("--add-opens=java.base/jdk.internal.util.jar=ALL-UNNAMED");
            commandLine.addArgument("--add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED");
            commandLine.addArgument("--add-opens=java.base/java.io=ALL-UNNAMED");
            commandLine.addArgument("--add-opens=java.base/java.nio=ALL-UNNAMED");
            commandLine.addArgument("--add-opens=java.base/sun.nio.ch=ALL-UNNAMED");
            commandLine.addArgument("--add-opens=java.base/java.util.concurrent=ALL-UNNAMED");
            commandLine.addArgument("--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED");
            commandLine.addArgument("--add-opens=java.base/java.util=ALL-UNNAMED");
            commandLine.addArgument("--add-opens=java.xml/jdk.xml.internal=ALL-UNNAMED");
        }

        // Only value should be quoted so we have to do it ourselves explicitly and disable additional quotation of
        // whole
        // argument because it causes errors during launch. Also URLEncode.encode on value seems to work correctly too,
        // it is done for log4j.configuration during toURL().toString() conversion.
        commandLine.addArgument(
                "-Dcassandra.storagedir="
                        + org.apache.commons.exec.util.StringUtils.quoteArgument(cassandraDir.getAbsolutePath()),
                false);
        if (stopKey != null && stopPort > 0 && stopPort < 65536) {
            commandLine.addArgument("-D" + CassandraMonitor.KEY_PROPERTY_NAME + "=" + stopKey);
            commandLine.addArgument("-D" + CassandraMonitor.PORT_PROPERTY_NAME + "=" + stopPort);
            commandLine.addArgument("-D" + CassandraMonitor.HOST_PROPERTY_NAME + "=" + listenAddress);
        }
        String log4jConfigurationFile = System.getProperty(
                "log4j.configurationFile",
                new File(new File(cassandraDir, "conf"), "log4j-server.xml")
                        .toURI()
                        .toURL()
                        .toString());
        commandLine.addArgument("-Dlog4j.configurationFile=" + log4jConfigurationFile);
        commandLine.addArgument("-Dcom.sun.management.jmxremote=" + jmxRemoteEnabled);
        commandLine.addArgument("-DcassandraLogLevel=" + logLevel);
        if (jmxRemoteEnabled) {
            commandLine.addArgument("-Dcom.sun.management.jmxremote.port=" + jmxPort);
            commandLine.addArgument("-Dcom.sun.management.jmxremote.ssl=false");
            commandLine.addArgument("-Dcom.sun.management.jmxremote.authenticate=false");
        }

        if (systemPropertyVariables != null && !systemPropertyVariables.isEmpty()) {
            for (Map.Entry<String, String> entry : systemPropertyVariables.entrySet()) {
                commandLine.addArgument("-D" + entry.getKey() + "=" + entry.getValue());
            }
        }

        commandLine.addArgument("-jar");
        // It seems that java cannot handle quoted jar file names...
        commandLine.addArgument(new File(new File(cassandraDir, "bin"), "cassandra.jar").getAbsolutePath(), false);

        return commandLine;
    }

    /**
     * Creates the command line to launch the {@code nodetool} utility.
     *
     * @param args the command line arguments to pass to the {@code nodetool} utility.
     * @return the {@link CommandLine} to launch {@code nodetool} with the supplied arguments.
     * @throws IOException if there are issues creating the cassandra home directory.
     */
    protected CommandLine newNodetoolCommandLine(String... args) throws IOException {
        createCassandraHome();
        CommandLine commandLine = newJavaCommandLine();
        commandLine.addArgument("-jar");
        // It seems that java cannot handle quoted jar file names...
        commandLine.addArgument(new File(new File(cassandraDir, "bin"), "nodetool.jar").getAbsolutePath(), false);
        commandLine.addArgument("--host");
        commandLine.addArgument("127.0.0.1");
        commandLine.addArgument("--port");
        commandLine.addArgument(Integer.toString(jmxPort));
        commandLine.addArguments(args);
        return commandLine;
    }

    /**
     * Turns a file into a path string that is quoted (and escaped) if necessary
     *
     * @param file the file to convert to a path string.
     * @return the path string.
     */
    private static String toPathString(File file) {
        String path = file.getAbsolutePath();
        boolean hasSpaces = path.indexOf(' ') != -1;
        boolean hasDoubleQuotes = path.indexOf('\"') != -1;
        boolean hasSingleQuotes = path.indexOf('\'') != -1;
        if (!(hasSpaces || hasDoubleQuotes || hasSingleQuotes)) {
            return path;
        }
        if (!hasDoubleQuotes) {
            return '\"' + path + '\"';
        }
        if (!hasSingleQuotes) {
            return '\'' + path + '\'';
        }
        return '\"' + StringUtils.escape(path, new char[] {'\"'}, '\'') + '\"';
    }
}
