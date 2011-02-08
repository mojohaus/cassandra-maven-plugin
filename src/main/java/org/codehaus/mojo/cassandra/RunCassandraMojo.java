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

import org.apache.commons.exec.DefaultExecuteResultHandler;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;

/**
 * Runs Cassandra in the foreground.
 *
 * @author stephenc
 * @goal run
 * @threadSafe
 */
public class RunCassandraMojo extends AbstractCassandraMojo
{
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
        boolean isClean = !cassandraDir.isDirectory();
        getLog().debug( (isClean ? "First start of Cassandra instance in " : "Re-using existing Cassandra instance in ")
            + cassandraDir.getAbsolutePath());
        try
        {
            DefaultExecuteResultHandler execHandler = Utils.startCassandraServer(cassandraDir,
                    newServiceCommandLine(),
                    createEnvironmentVars(), getLog());
            try {
                getLog().info("Waiting for Cassandra to start...");
                Utils.waitUntilStarted(rpcAddress, rpcPort, 0, getLog());

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

                ConsoleScanner consoleScanner = new ConsoleScanner();
                consoleScanner.start();
                getLog().info("Hit ENTER on the console to stop Cassandra.");
                try
                {
                    consoleScanner.waitForFinished();
                } catch (InterruptedException e)
                {
                    // ignore
                }
            } finally
            {
                Utils.stopCassandraServer(rpcAddress, rpcPort, stopPort, stopKey, getLog());
                try
                {
                    execHandler.waitFor();
                } catch (InterruptedException e)
                {
                    // ignore
                }
            }
        } catch (IOException e)
        {
            throw new MojoExecutionException(e.getLocalizedMessage(), e);
        }
    }
}
