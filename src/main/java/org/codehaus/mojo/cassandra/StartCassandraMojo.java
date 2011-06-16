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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;

/**
 * Starts a Cassandra instance in the background.
 *
 * @author stephenc
 * @goal start
 * @threadSafe
 * @phase pre-integration-test
 */
public class StartCassandraMojo extends AbstractCassandraMojo
{
    /**
     * How long to wait for Cassandra to be started before finishing the goal. A value of 0 will wait indefinately. A
     * value of -1 will not wait at all.
     *
     * @parameter default-value="180"
     */
    protected int startWaitSeconds;

    /**
     * The script to load.
     *
     * @parameter default-value="${basedir}/src/cassandra/cli/load.script"
     */
    protected File script;

    /**
     * Whether to ignore errors when loading the script.
     *
     * @parameter expression="${cassandra.load.failure.ignore}"
     */
    private boolean loadFailureIgnore;

    /**
     * When {@code true}, if this is a clean start then the load script will be applied automatically.
     *
     * @parameter expression="${cassandra.load.after.first.start}" default-value="true"
     */
    private boolean loadAfterFirstStart;

    /**
     * {@inheritDoc}
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if (skip)
        {
            getLog().info("Skipping cassandra: cassandra.skip==true");
            return;
        }
        long timeStamp = System.currentTimeMillis();
        boolean isClean = !cassandraDir.isDirectory();
        getLog().debug( (isClean ? "First start of Cassandra instance in " : "Re-using existing Cassandra instance in ")
            + cassandraDir.getAbsolutePath());
        try
        {
            Utils.startCassandraServer(cassandraDir, newServiceCommandLine(), createEnvironmentVars(), getLog());

            if (startWaitSeconds >= 0)
            {
                getLog().info("Waiting for Cassandra to start...");
                boolean started = Utils.waitUntilStarted(rpcAddress, rpcPort, startWaitSeconds, getLog());
                if (!started)
                {
                    Utils.stopCassandraServer(rpcAddress, rpcPort, listenAddress, stopPort, stopKey, getLog());
                    throw new MojoFailureException("Cassandra failed to start within " + startWaitSeconds + "s");
                }
            }
            if (isClean && loadAfterFirstStart && script != null && script.isFile()) {
                getLog().info("Running " + script + "...");
                int rv = Utils.runLoadScript(cassandraDir, newCliCommandLine("--file", script.getAbsolutePath()),
                        createEnvironmentVars(), getLog());
                if (rv != 0)
                {
                    if (loadFailureIgnore)
                    {
                        getLog().error("Command exited with error code " + rv + ". Ignoring as loadFailureIgnore is true");
                    }
                    else
                    {
                        throw new MojoExecutionException("Command exited with error code " + rv);
                    }
                } else
                {
                    getLog().info("Finished " + script + ".");
                }
            }
            getLog().info("Cassandra started in " + ((System.currentTimeMillis() - timeStamp) / 100L) / 10.0 + "s");
        } catch (IOException e)
        {
            throw new MojoExecutionException(e.getLocalizedMessage(), e);
        }
    }
}
