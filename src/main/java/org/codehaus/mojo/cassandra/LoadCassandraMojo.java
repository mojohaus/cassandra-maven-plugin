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

import org.apache.cassandra.thrift.Cassandra;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Loads a {@code cassandra-cli} bscript into a Cassandra instance.
 *
 * @author stephenc
 * @goal load
 * @threadSafe
 * @phase pre-integration-test
 */
public class LoadCassandraMojo extends AbstractCassandraMojo
{
    /**
     * The script to load.
     *
     * @parameter default-value="${basedir}/src/cassandra/cli/load.script"
     */
    protected File script;

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
            if (!script.isFile())
            {
                throw new MojoFailureException("Specified script " + script + " does not exist.");
            }
            Map environment = createEnvironmentVars();
            CommandLine commandLine = newCliCommandLine("--file", script.getAbsolutePath());

            Executor exec = new DefaultExecutor();
            exec.setWorkingDirectory(cassandraDir);
            exec.setProcessDestroyer(new ShutdownHookProcessDestroyer());

            LogOutputStream stdout = new MavenLogOutputStream(getLog());
            LogOutputStream stderr = new MavenLogOutputStream(getLog());

            try
            {
                getLog().debug("Executing command line: " + commandLine);

                exec.setStreamHandler(new PumpStreamHandler(stdout, stderr));

                exec.execute(commandLine, environment);
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
