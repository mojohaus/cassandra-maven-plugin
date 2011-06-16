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

import org.apache.cassandra.cli.CliMain;
import org.apache.cassandra.tools.NodeCmd;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.OS;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Base class for all the Cassandra Maven Plugin goals.
 *
 * @author stephenc
 */
public abstract class AbstractCassandraMojo extends AbstractMojo
{
    /**
     * The directory to hold cassandra's database.
     *
     * @parameter default-value="${project.build.directory}/cassandra"
     * @required
     */
    protected File cassandraDir;

    /**
     * The enclosing project.
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The directory containing generated classes.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File classesDirectory;

    /**
     * The directory containing generated test classes.
     *
     * @parameter expression="${project.build.testOutputDirectory}"
     * @required
     */
    private File testClassesDirectory;

    /**
     * Adds the test classpath to cassandra (for example you could use this when you have a custom comparator on your
     * test classpath.
     *
     * @parameter default-value="false"
     */
    protected boolean addTestClasspath;

    /**
     * Adds the main classpath to cassandra (for example you could use this when you have a custom comparator on your
     * main classpath.
     *
     * @parameter default-value="false"
     */
    protected boolean addMainClasspath;

    /**
     * Skip the execution.
     *
     * @parameter expression="${cassandra.skip}" default-value="false"
     */
    protected boolean skip;

    /**
     * @parameter default-value="${plugin.artifacts}"
     * @readonly
     */
    private List<Artifact> pluginDependencies;

    /**
     * @parameter default-value="${plugin.pluginArtifact}"
     * @readonly
     */
    private Artifact pluginArtifact;

    /**
     * The current build session instance. This is used for toolchain manager API calls.
     *
     * @parameter default-value="${session}"
     * @required
     * @readonly
     */
    protected MavenSession session;

    /**
     * In yaml format any overrides or additional configuration that you want to apply to the server.
     *
     * @parameter
     */
    private String yaml;

    /**
     * Address to use for the RPC interface. Do not change this unless you really know what you are doing.
     *
     * @parameter default-value="127.0.0.1"
     */
    protected String rpcAddress;

    /**
     * Port to listen to for the RPC interface.
     *
     * @parameter expression="${cassandra.rpcPort}" default-value="9160"
     */
    protected int rpcPort;

    /**
     * Port to listen to for the JMX interface.
     *
     * @parameter expression="${cassandra.jmxPort}" default-value="7199"
     */
    protected int jmxPort;

    /**
     * Address to bind to and tell other Cassandra nodes to connect to. You
     * <strong>must</strong> change this if you want multiple nodes to be able to
     * communicate!
     *
     * Leaving it blank leaves it up to InetAddress.getLocalHost(). This
     * will always do the Right Thing <em>if</em> the node is properly configured
     * (hostname, name resolution, etc), and the Right Thing is to use the
     * address associated with the hostname (it might not be).
     *
     * Setting this to 0.0.0.0 is always wrong.
     * Do not change this unless you really know what you are doing.
     *
     * @parameter default-value="127.0.0.1"
     */
    protected String listenAddress;

    /**
     * Port to listen to for the Storage interface.
     *
     * @parameter expression="${cassandra.storagePort}" default-value="7000"
     */
    protected int storagePort;

    /**
     * Port to listen to for receiving the stop command over
     *
     * @parameter expression="${cassandra.stopPort}" default-value="8081"
     */
    protected int stopPort;

    /**
     * Key to be provided when stopping cassandra
     *
     * @parameter expression="${cassandra.stopKey}" default-value="cassandra-maven-plugin"
     */
    protected String stopKey;

    /**
     * Number of megabytes to limit the cassandra JVM to.
     *
     * @parameter expression="${cassandra.maxMemory}" default-value="512"
     */
    protected int maxMemory;

    /**
     * Create a jar with just a manifest containing a Main-Class entry for SurefireBooter and a Class-Path entry for
     * all classpath elements. Copied from surefire (ForkConfiguration#createJar())
     *
     * @param jarFile   The jar file to create/update
     * @param mainClass The main class to run.
     * @throws java.io.IOException if something went wrong.
     */
    protected void createCassandraJar(File jarFile, String mainClass) throws IOException
    {
        createCassandraJar(jarFile, mainClass, cassandraDir);
    }
    /**
     * Create a jar with just a manifest containing a Main-Class entry for SurefireBooter and a Class-Path entry for
     * all classpath elements. Copied from surefire (ForkConfiguration#createJar())
     *
     * @param jarFile   The jar file to create/update
     * @param mainClass The main class to run.
     * @throws java.io.IOException if something went wrong.
     */
    protected void createCassandraJar(File jarFile, String mainClass, File cassandraDir) throws IOException
    {
        File conf = new File(cassandraDir, "conf");
        FileOutputStream fos = null;
        JarOutputStream jos = null;
        try
        {
            fos = new FileOutputStream(jarFile);
            jos = new JarOutputStream(fos);
            jos.setLevel(JarOutputStream.STORED);
            jos.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));

            Manifest man = new Manifest();

            // we can't use StringUtils.join here since we need to add a '/' to
            // the end of directory entries - otherwise the jvm will ignore them.
            StringBuilder cp = new StringBuilder();
            cp.append(new URL(conf.toURI().toASCIIString()).toExternalForm());
            cp.append(' ');
            getLog().debug("Adding plugin artifact: " + ArtifactUtils.versionlessKey(pluginArtifact) +
                    " to the classpath");
            cp.append(new URL(pluginArtifact.getFile().toURI().toASCIIString()).toExternalForm());
            cp.append(' ');

            for (Artifact artifact : this.pluginDependencies)
            {
                getLog().debug("Adding plugin dependency artifact: " + ArtifactUtils.versionlessKey(artifact) +
                        " to the classpath");
                // NOTE: if File points to a directory, this entry MUST end in '/'.
                cp.append(new URL(artifact.getFile().toURI().toASCIIString()).toExternalForm());
                cp.append(' ');
            }

            if (addMainClasspath || addTestClasspath)
            {
                if (addTestClasspath)
                {
                    getLog().debug("Adding: " + testClassesDirectory + " to the classpath");
                    cp.append(new URL(testClassesDirectory.toURI().toASCIIString()).toExternalForm());
                    cp.append(' ');
                }
                if (addMainClasspath)
                {
                    getLog().debug("Adding: " + classesDirectory + " to the classpath");
                    cp.append(new URL(classesDirectory.toURI().toASCIIString()).toExternalForm());
                    cp.append(' ');
                }
                for (Artifact artifact : (Set<Artifact>) this.project.getArtifacts())
                {
                    if ("jar".equals(artifact.getType())
                            && !Artifact.SCOPE_PROVIDED.equals(artifact.getScope())
                            && (!Artifact.SCOPE_TEST.equals(artifact.getScope()) || addTestClasspath))
                    {
                        getLog().debug("Adding dependency: " + ArtifactUtils.versionlessKey(artifact) +
                                " to the classpath");
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
        } finally
        {
            IOUtil.close(jos);
            IOUtil.close(fos);
        }
    }

    /**
     * Creates the cassandra home directory.
     *
     * @throws IOException if something goes wrong.
     */
    protected void createCassandraHome() throws IOException
    {
        createCassandraHome(cassandraDir, listenAddress, rpcAddress, null, new String[]{listenAddress});
    }

    /**
     * Creates the cassandra home directory.
     *
     * @param cassandraDir the cassandra home directory.
     *
     * @throws IOException if something goes wrong.
     */
    protected void createCassandraHome(File cassandraDir, String listenAddress, String rpcAddress,
                                     BigInteger initialToken, String[] seeds) throws IOException
    {
        File bin = new File(cassandraDir, "bin");
        File conf = new File(cassandraDir, "conf");
        File data = new File(cassandraDir, "data");
        File commitlog = new File(cassandraDir, "commitlog");
        File savedCaches = new File(cassandraDir, "saved_caches");
        for (File dir : Arrays.asList(cassandraDir, bin, conf, data, commitlog, savedCaches))
        {
            if (dir.isFile())
            {
                getLog().debug("Deleting file " + dir + " as we need to create a directory with the same name.");
                if (!dir.delete())
                {
                    getLog().warn("Could not delete file " + dir);
                }
            }
            if (!dir.isDirectory())
            {
                getLog().debug("Creating directory " + dir + " as it does not exist.");
                if (!dir.mkdirs())
                {
                    getLog().warn("Could not create directory " + dir);
                }
            }
        }
        File cassandraYaml = new File(conf, "cassandra.yaml");
        if (Utils.shouldGenerateResource(project, cassandraYaml))
        {
            getLog().debug((cassandraYaml.isFile() ? "Updating " : "Creating ") + cassandraYaml);
            createCassandraYaml(cassandraYaml, data, commitlog, savedCaches, listenAddress, rpcAddress, initialToken, seeds);
        }
        File log4jServerProperties = new File(conf, "log4j-server.properties");
        if (Utils.shouldGenerateResource(project, log4jServerProperties))
        {
            getLog().debug((log4jServerProperties.isFile() ? "Updating " : "Creating ") + log4jServerProperties);
            FileUtils.copyURLToFile(getClass().getResource("/log4j.properties"), log4jServerProperties);
        }
        File log4jClientProperties = new File(conf, "log4j-client.properties");
        if (Utils.shouldGenerateResource(project, log4jClientProperties))
        {
            getLog().debug((log4jClientProperties.isFile() ? "Updating " : "Creating ") + log4jClientProperties);
            FileUtils.copyURLToFile(getClass().getResource("/log4j.properties"), log4jClientProperties);
        }
        File cassandraJar = new File(bin, "cassandra.jar");
        if (Utils.shouldGenerateResource(project, cassandraJar))
        {
            getLog().debug((cassandraJar.isFile() ? "Updating " : "Creating ") + cassandraJar);
            createCassandraJar(cassandraJar, CassandraMonitor.class.getName(), cassandraDir);
        }
        File cassandraCliJar = new File(bin, "cassandra-cli.jar");
        if (Utils.shouldGenerateResource(project, cassandraCliJar))
        {
            getLog().debug((cassandraCliJar.isFile() ? "Updating " : "Creating ") + cassandraCliJar);
            createCassandraJar(cassandraCliJar, CliMain.class.getName(), cassandraDir);
        }
        File nodetoolJar = new File(bin, "nodetool.jar");
        if (Utils.shouldGenerateResource(project, nodetoolJar))
        {
            getLog().debug((nodetoolJar.isFile() ? "Updating " : "Creating ") + nodetoolJar);
            createCassandraJar(nodetoolJar, NodeCmd.class.getName(), cassandraDir);
        }
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
    private void createCassandraYaml(File cassandraYaml, File data, File commitlog, File savedCaches)
            throws IOException
    {
        createCassandraYaml(cassandraYaml, data, commitlog, savedCaches, listenAddress, rpcAddress, null, new String[]{listenAddress});
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
    private void createCassandraYaml(File cassandraYaml, File data, File commitlog, File savedCaches,
                                     String listenAddress, String rpcAddress,
                                     BigInteger initialToken, String[] seeds)
    throws IOException
    {
        String defaults = IOUtil.toString(getClass().getResourceAsStream("/cassandra.yaml"));
        StringBuilder config = new StringBuilder();
        config.append("data_file_directories:\n")
                .append("    - ").append(data.getAbsolutePath()).append("\n");
        config.append("commitlog_directory: ").append(commitlog).append("\n");
        config.append("saved_caches_directory: ").append(savedCaches).append("\n");
        config.append("initial_token: ").append(initialToken == null ? "" : initialToken.toString()).append("\n");
        config.append("listen_address: ").append(listenAddress).append("\n");
        config.append("storage_port: ").append(storagePort).append("\n");
        config.append("rpc_address: ").append(rpcAddress).append("\n");
        config.append("rpc_port: ").append(rpcPort).append("\n");
        if (seeds != null) {
            config.append("seed_provider: ").append("\n");
            config.append("    - class_name: org.apache.cassandra.locator.SimpleSeedProvider").append("\n");
            config.append("      parameters:").append("\n");
            String sep = "          - seeds: \"";
            for (int i = 0; i < seeds.length; i++) {
                config.append(sep).append(seeds[i]);
                sep =  ", ";
            }
            if (sep.length() == 2) {
                config.append("\"").append("\n");
            }
        }
        FileUtils.fileWrite(cassandraYaml.getAbsolutePath(),
                Utils.merge(Utils.merge(defaults, yaml), config.toString()));
    }

    /**
     * Gets the Java toolchain.
     *
     * @return the Java toolchain.
     */
    protected Toolchain getToolchain()
    {
        Toolchain tc = null;

        try
        {
            if (session != null) // session is null in tests..
            {
                ToolchainManager toolchainManager =
                        (ToolchainManager) session.getContainer().lookup(ToolchainManager.ROLE);

                if (toolchainManager != null)
                {
                    tc = toolchainManager.getToolchainFromBuildContext("jdk", session);
                }
            }
        } catch (ComponentLookupException componentLookupException)
        {
            // just ignore, could happen in pre-2.0.9 builds..
        }
        return tc;
    }

    /**
     * Create a {@link CommandLine} to launch Java.
     *
     * @return a {@link CommandLine} to launch Java.
     */
    protected CommandLine newJavaCommandLine()
    {
        String exec = null;
        Toolchain tc = getToolchain();

        // if the file doesn't exist & toolchain is null, java is probably in the PATH...
        // we should probably also test for isFile and canExecute, but the second one is only
        // available in SDK 6.
        if (tc != null)
        {
            getLog().info("Toolchain in cassandra-maven-plugin: " + tc);
            exec = tc.findTool("java");
        } else
        {
            if (OS.isFamilyWindows())
            {
                String ex = "java.exe";
                // now try to figure the path from PATH, PATHEXT env vars
                // if bat file, wrap in cmd /c
                String path = System.getenv("PATH");
                if (path != null)
                {
                    for (String elem : StringUtils.split(path, File.pathSeparator))
                    {
                        File f = new File(new File(elem), ex);
                        if (f.exists())
                        {
                            exec = ex;
                            break;
                        }
                    }
                }
            }
        }

        if (exec == null)
        {
            exec = "java";
        }

        return new CommandLine(exec);
    }

    /**
     * Creates the environment required when launching Cassandra or the CLI tools.
     *
     * @return the environment required when launching Cassandra or the CLI tools.
     */
    protected Map<String, String> createEnvironmentVars()
    {
        Map<String, String> enviro = new HashMap<String, String>();
        try
        {
            Properties systemEnvVars = CommandLineUtils.getSystemEnvVars();
            for (Map.Entry entry : systemEnvVars.entrySet())
            {
                enviro.put((String) entry.getKey(), (String) entry.getValue());
            }
        } catch (IOException x)
        {
            getLog().error("Could not assign default system enviroment variables.", x);
        }
        enviro.put("CASSANDRA_CONF", new File(cassandraDir, "conf").getAbsolutePath());
        return enviro;
    }

    /**
     * Creates the command line to launch the cassandra server.
     *
     * @return the command line to launch the cassandra server.
     * @throws IOException if there are issues creating the cassandra home directory.
     */
    protected CommandLine newServiceCommandLine() throws IOException
    {
        return newServiceCommandLine(cassandraDir, listenAddress, rpcAddress, null, new String[]{listenAddress}, true, jmxPort  );
    }

    /**
     * Creates the command line to launch the cassandra server.
     *
     * @return the command line to launch the cassandra server.
     * @throws IOException if there are issues creating the cassandra home directory.
     */
    protected CommandLine newServiceCommandLine(File cassandraDir, String listenAddress, String rpcAddress, BigInteger initialToken,
                                                String[] seeds, boolean jmxRemoteEnabled, int jmxPort) throws IOException
    {
        createCassandraHome(cassandraDir, listenAddress, rpcAddress, initialToken, seeds);
        CommandLine commandLine = newJavaCommandLine();
        List<String> args = new ArrayList<String>();
        args.add("-Xmx" + maxMemory + "m");
        if (stopKey != null && stopPort > 0 && stopPort < 65536)
        {
            args.add("-D" + CassandraMonitor.KEY_PROPERTY_NAME + "=" + stopKey);
            args.add("-D" + CassandraMonitor.PORT_PROPERTY_NAME + "=" + stopPort);
            args.add("-D" + CassandraMonitor.HOST_PROPERTY_NAME + "=" + listenAddress);
        }
        args.add("-Dlog4j.configuration=file://" + new File(new File(cassandraDir, "conf"), "log4j-server.properties").getAbsolutePath());
        args.add("-Dcom.sun.management.jmxremote=" + jmxRemoteEnabled);
        if (jmxRemoteEnabled) {
        args.add("-Dcom.sun.management.jmxremote.port=" + jmxPort);
        args.add("-Dcom.sun.management.jmxremote.ssl=false");
        args.add("-Dcom.sun.management.jmxremote.authenticate=false");
        }
        args.add("-jar");
        args.add(new File(new File(cassandraDir, "bin"), "cassandra.jar").toString());
        commandLine.addArguments(args.toArray(new String[args.size()]), true);
        return commandLine;
    }

    /**
     * Creates the command line to launch the {@code cassandra-cli} utility.
     *
     * @param args the command line arguments to pass to the {@code cassandra-cli} utility.
     * @return the {@link CommandLine} to launch {@code cassandra-cli} with the supplied arguments.
     * @throws IOException if there are issues creating the cassandra home directory.
     */
    protected CommandLine newCliCommandLine(String... args) throws IOException
    {
        createCassandraHome();
        CommandLine commandLine = newJavaCommandLine();
        List<String> args1 = new ArrayList<String>();
        args1.add("-jar");
        args1.add(new File(new File(cassandraDir, "bin"), "cassandra-cli.jar").toString());
        args1.add("--host");
        args1.add(rpcAddress);
        args1.add("--port");
        args1.add(Integer.toString(rpcPort));
        args1.add("--jmxport");
        args1.add(Integer.toString(jmxPort));
        args1.addAll(Arrays.asList(args));
        commandLine.addArguments(args1.toArray(new String[args1.size()]), true);
        return commandLine;
    }

    /**
     * Creates the command line to launch the {@code cassandra-cli} utility.
     *
     * @param args the command line arguments to pass to the {@code cassandra-cli} utility.
     * @return the {@link CommandLine} to launch {@code cassandra-cli} with the supplied arguments.
     * @throws IOException if there are issues creating the cassandra home directory.
     */
    protected CommandLine newNodetoolCommandLine(String... args) throws IOException
    {
        createCassandraHome();
        CommandLine commandLine = newJavaCommandLine();
        List<String> args1 = new ArrayList<String>();
        args1.add("-jar");
        args1.add(new File(new File(cassandraDir, "bin"), "nodetool.jar").toString());
        args1.add("--host");
        args1.add("127.0.0.1");
        args1.add("--port");
        args1.add(Integer.toString(jmxPort));
        args1.addAll(Arrays.asList(args));
        commandLine.addArguments(args1.toArray(new String[args1.size()]), true);
        return commandLine;
    }
}
