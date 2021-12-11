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

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.IOException;
import java.util.Map;

/**
 * Runs {@code nodetool repair} on a Cassandra instance.
 *
 * @author stephenc
 * @goal repair
 * @threadSafe
 */
public class RepairCassandraMojo extends AbstractCassandraMojo
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
            Map environment = createEnvironmentVars();
            CommandLine commandLine = newNodetoolCommandLine("repair");

            Executor exec = new DefaultExecutor();
            exec.setWorkingDirectory(cassandraDir);
            exec.setProcessDestroyer(new ShutdownHookProcessDestroyer());

            LogOutputStream stdout = new MavenLogOutputStream(getLog());
            LogOutputStream stderr = new MavenLogOutputStream(getLog());

            try
            {
                getLog().debug("Executing command line: " + commandLine);

                exec.setStreamHandler(new PumpStreamHandler(stdout, stderr, System.in));

                exec.execute(commandLine, environment);

                getLog().info("Repair triggered.");
            } catch (ExecuteException e)
            {
                throw new MojoExecutionException("Command execution failed.", e);
            } catch (IOException e)
            {
                throw new MojoExecutionException("Command execution failed.", e);
            }
        } catch (IOException e)
        {
            throw new MojoExecutionException(e.getLocalizedMessage(), e);
        }
    }
}
