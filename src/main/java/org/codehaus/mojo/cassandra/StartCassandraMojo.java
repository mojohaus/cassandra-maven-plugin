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

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Starts a Cassandra instance in the background.
 *
 * @author stephenc
 *
 */
@Mojo(name = "start", threadSafe = true, defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class StartCassandraMojo extends AbstractCqlLoadMojo {
    /**
     * How long to wait for Cassandra to be started before finishing the goal. A value of 0 will wait indefinitely. A
     * value of -1 will not wait at all.
     *
     */
    @Parameter(defaultValue = "180")
    protected int startWaitSeconds;

    /**
     * When {@code true}, if this is a clean start then the load script will be applied automatically.
     *
     */
    @Parameter(property = "cassandra.load.after.first.start", defaultValue = "true")
    private boolean loadAfterFirstStart;

    /**
     * Whether to ignore errors when loading the script.
     *
     * @since 1.2.1-2
     */
    @Parameter(property = "cassandra.cu.load.failure.ignore")
    private boolean cuLoadFailureIgnore;

    /**
     * When {@code true}, if this is a clean start then the CassandraUnit dataSet will be applied automatically.
     *
     * @since 1.2.1-2
     */
    @Parameter(property = "cassandra.cu.load.after.first.start", defaultValue = "true")
    private boolean cuLoadAfterFirstStart;

    /**
     * If <code>true</code>, the java options --add-exports and --add-opens will be added to the cassandra start. Which
     * is needed, if cassandra runs with a Java runtime &gt;= 11
     *
     * @since 3.7
     */
    @Parameter(property = "cassandra.addJdk11Options", defaultValue = "false")
    protected boolean addJdk11Options;

    @Override
    protected boolean useJdk11Options() {
        return addJdk11Options;
    }

    /**
     * {@inheritDoc}
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping cassandra: cassandra.skip==true");
            return;
        }
        long timeStamp = System.currentTimeMillis();
        boolean isClean = !cassandraDir.isDirectory();
        getLog().debug((isClean ? "First start of Cassandra instance in " : "Re-using existing Cassandra instance in ")
                + cassandraDir.getAbsolutePath());
        try {
            Utils.startCassandraServer(cassandraDir, newServiceCommandLine(), createEnvironmentVars(), getLog());

            if (startWaitSeconds >= 0) {
                getLog().info("Waiting for Cassandra to start...");
                boolean started = Utils.waitUntilStarted(rpcAddress, nativeTransportPort, startWaitSeconds, getLog());
                if (!started) {
                    Utils.stopCassandraServer(
                            rpcAddress, nativeTransportPort, listenAddress, stopPort, stopKey, getLog());
                    throw new MojoFailureException("Cassandra failed to start within " + startWaitSeconds + "s");
                }
            }
            if (isClean && loadAfterFirstStart) {
                execCqlFile();
            }

            getLog().info("Cassandra started in " + ((System.currentTimeMillis() - timeStamp) / 100L) / 10.0 + "s");
        } catch (IOException e) {
            throw new MojoExecutionException(e.getLocalizedMessage(), e);
        }
    }
}
