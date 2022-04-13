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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Deletes the Cassandra home directory that we create for running Cassandra.
 *
 * @author stephenc
 *
 */
@Mojo(name = "delete", threadSafe = true)
public class DeleteCassandraMojo extends AbstractMojo
{
    /**
     * The directory to hold cassandra's database.
     *
     */
    @Parameter(defaultValue="${project.build.directory}/cassandra")
    protected File cassandraDir;

    /**
     * Skip the execution.
     *
     */
    @Parameter(property="cassandra.skip", defaultValue="false")
    private boolean skip;

    /**
     * Fail execution in case of error.
     *
     * @since 2.0.0-1
     */
    @Parameter(property="cassandra.failOnError", defaultValue="true")
    protected boolean failOnError;

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
            getLog().info("Deleting " + cassandraDir.getAbsolutePath());
            FileUtils.deleteDirectory(cassandraDir);
        } catch (IOException e)
        {
            if (failOnError)
            {
                throw new MojoFailureException(e.getLocalizedMessage(), e);
            }
            getLog().warn("Failed to delete " + cassandraDir.getAbsolutePath(), e);
        }
    }
}
