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
     * {@inheritDoc}
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if (skip)
        {
            getLog().info("Skipping cassandra: cassandra.skip==true");
            return;
        }
        try
        {
            DefaultExecuteResultHandler execHandler = Utils.startCassandraServer(cassandraDir,
                    newServiceCommandLine(),
                    createEnvironmentVars(), getLog());
            getLog().info("Waiting for Cassandra to start...");
            Utils.waitUntilStarted(rpcAddress, rpcPort, 0, getLog());
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
            Utils.stopCassandraServer(rpcAddress, rpcPort, stopPort, stopKey, getLog());
            try
            {
                execHandler.waitFor();
            } catch (InterruptedException e)
            {
                throw new MojoExecutionException("Command execution interrupted.", e);
            }
        } catch (IOException e)
        {
            throw new MojoExecutionException(e.getLocalizedMessage(), e);
        }
    }
}
