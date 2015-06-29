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
import java.io.IOException;

import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.cassandraunit.DataLoader;
import org.cassandraunit.dataset.FileDataSet;
import org.cassandraunit.dataset.ParseException;

/**
 * Runs Cassandra in the foreground.
 *
 * @author stephenc
 * @goal run
 * @threadSafe
 */
public class RunCassandraMojo
    extends AbstractCqlLoadMojo
{
    /**
     * When {@code true}, if this is a clean start then the load script will be applied automatically.
     *
     * @parameter property="cassandra.load.after.first.start" default-value="true"
     */
    private boolean loadAfterFirstStart;

    /**
     * The CassandraUnit dataSet to load.
     *
     * @parameter default-value="${basedir}/src/test/resources/dataSet.xml"
     * @since 1.2.1-2
     */
    protected File cuDataSet;

    /**
     * Whether to ignore errors when loading the script.
     *
     * @parameter property="cassandra.cu.load.failure.ignore"
     * @since 1.2.1-2
     */
    private boolean cuLoadFailureIgnore;

    /**
     * When {@code true}, if this is a clean start then the CassandraUnit dataSet will be applied automatically.
     *
     * @parameter property="cassandra.cu.load.after.first.start" default-value="true"
     * @since 1.2.1-2
     */
    private boolean cuLoadAfterFirstStart;

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( skip )
        {
            getLog().info( "Skipping cassandra: cassandra.skip==true" );
            return;
        }
        long timeStamp = System.currentTimeMillis();
        boolean isClean = !cassandraDir.isDirectory();
        getLog().debug(
            ( isClean ? "First start of Cassandra instance in " : "Re-using existing Cassandra instance in " )
                + cassandraDir.getAbsolutePath() );
        try
        {
            DefaultExecuteResultHandler execHandler =
                Utils.startCassandraServer( cassandraDir, newServiceCommandLine(), createEnvironmentVars(), getLog() );
            try
            {
                getLog().info( "Waiting for Cassandra to start..." );
                Utils.waitUntilStarted( rpcAddress, rpcPort, 0, getLog() );

                if ( isClean && loadAfterFirstStart)
                {
                    execCqlFile();
                }

                if ( isClean && cuLoadAfterFirstStart && cuDataSet != null && cuDataSet.isFile() )
                {
                    getLog().info( "Loading CassandraUnit dataSet " + cuDataSet + "..." );
                    try
                    {
                        DataLoader dataLoader = new DataLoader( "cassandraUnitCluster", rpcAddress + ":" + rpcPort );
                        dataLoader.load( new FileDataSet( cuDataSet.getAbsolutePath() ) );
                    }
                    catch ( ParseException e )
                    {
                        if ( cuLoadFailureIgnore )
                        {
                            getLog().error( e.getMessage() + ". Ignoring as cuLoadFailureIgnore is true" );
                        }
                        else
                        {
                            throw new MojoExecutionException( "Error while loading CassandraUnit dataSet", e );
                        }
                    }
                    getLog().info( "Finished " + cuDataSet + "." );
                }

                getLog().info(
                    "Cassandra started in " + ( ( System.currentTimeMillis() - timeStamp ) / 100L ) / 10.0 + "s" );
                ConsoleScanner consoleScanner = new ConsoleScanner();
                consoleScanner.start();
                getLog().info( "Hit ENTER on the console to stop Cassandra and continue the build." );
                try
                {
                    consoleScanner.waitForFinished();
                }
                catch ( InterruptedException e )
                {
                    // ignore
                }
            }
            finally
            {
                Utils.stopCassandraServer(rpcAddress, rpcPort, listenAddress, stopPort, stopKey, getLog());
                try
                {
                    execHandler.waitFor();
                }
                catch ( InterruptedException e )
                {
                    // ignore
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getLocalizedMessage(), e );
        }
    }
}
