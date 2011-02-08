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

import org.apache.commons.exec.ExecuteException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;

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
     * Whether to ignore errors when loading the script.
     *
     * @parameter expression="${cassandra.load.failure.ignore}"
     */
    private boolean loadFailureIgnore;

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
                if (loadFailureIgnore)
                {
                    getLog().error("Specified script " + script + " does not exist."
                            + ". Ignoring as loadFailureIgnore is true");
                    return;
                }
                else
                {
                    throw new MojoFailureException("Specified script " + script + " does not exist.");
                }
            }

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
            }
        } catch (IOException e)
        {
            throw new MojoExecutionException(e.getLocalizedMessage(), e);
        }
    }

}
