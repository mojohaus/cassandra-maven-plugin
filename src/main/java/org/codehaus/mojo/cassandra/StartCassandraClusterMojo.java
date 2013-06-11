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
import org.cassandraunit.DataLoader;
import org.cassandraunit.dataset.FileDataSet;
import org.cassandraunit.dataset.ParseException;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * Starts a Cassandra instance in the background.
 *
 * @author stephenc
 * @goal start-cluster
 * @threadSafe
 * @phase pre-integration-test
 */
public class StartCassandraClusterMojo
    extends AbstractCassandraMojo
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
     * The CassandraUnit dataSet to load.
     *
     * @parameter default-value="${basedir}/src/test/resources/dataSet.xml"
     * @since 1.2.1-2
     */
    protected File cuDataSet;

    /**
     * Whether to ignore errors when loading the script.
     *
     * @parameter expression="${cassandra.cu.load.failure.ignore}"
     * @since 1.2.1-2
     */
    private boolean cuLoadFailureIgnore;

    /**
     * When {@code true}, if this is a clean start then the CassandraUnit dataSet will be applied automatically.
     *
     * @parameter expression="${cassandra.cu.load.after.first.start}" default-value="true"
     * @since 1.2.1-2
     */
    private boolean cuLoadAfterFirstStart;

    /**
     * The number of nodes in the cluster.
     *
     * @parameter expression="${cassandra.cluster.size}" default-value="4"
     */
    private int clusterSize;

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
        if ( clusterSize < 1 )
        {
            throw new MojoExecutionException(
                "Invalid cluster size of " + clusterSize + " specified. Must be at least 1" );
        }
        if ( clusterSize > 254 )
        {
            throw new MojoExecutionException(
                "Invalid cluster size of " + clusterSize + " specified. Must be less than 254" );
        }
        File[] cassandraDir = new File[clusterSize];
        BigInteger[] initialToken = new BigInteger[clusterSize];
        String[] listenAddress = new String[clusterSize];
        boolean isClean = true;
        for ( int node = 0; node < clusterSize; node++ )
        {
            listenAddress[node] = "127.0.0." + ( node + 1 );
            initialToken[node] = BigInteger.valueOf( 2 ).pow( 127 ).multiply( BigInteger.valueOf( node ) ).divide(
                BigInteger.valueOf( clusterSize ) );
            cassandraDir[node] =
                new File( this.cassandraDir.getParent(), this.cassandraDir.getName() + "-node" + ( node + 1 ) );
            if ( isClean && cassandraDir[node].isDirectory() )
            {
                getLog().debug( "Re-using existing Cassandra cluster in " + cassandraDir[node].getAbsolutePath() );
                isClean = false;
            }
        }
        long timeStamp = System.currentTimeMillis();
        if ( isClean )
        {
            getLog().debug( "First start of Cassandra cluster in " + Arrays.asList( cassandraDir ) );
        }
        try
        {
            for ( int node = 0; node < clusterSize; node++ )
            {
                getLog().info( "Starting for Cassandra Node " + ( node + 1 ) + "..." );
                Utils.startCassandraServer( cassandraDir[node],
                                            newServiceCommandLine( cassandraDir[node], listenAddress[node],
                                                                   listenAddress[node], initialToken[node],
                                                                   listenAddress, node == 0, node == 0 ? jmxPort : 0 ),
                                            createEnvironmentVars(), getLog() );
            }

            if ( startWaitSeconds >= 0 )
            {
                for ( int node = 0; node < clusterSize; node++ )
                {
                    getLog().info( "Waiting for Cassandra Node " + ( node + 1 ) + " to start..." );
                    boolean started =
                        Utils.waitUntilStarted( listenAddress[node], rpcPort, startWaitSeconds, getLog() );
                    if ( !started )
                    {
                        Utils.stopCassandraServer( listenAddress[node], rpcPort, listenAddress[node], stopPort, stopKey,
                                                   getLog() );
                        throw new MojoFailureException( "Cassandra failed to start within " + startWaitSeconds + "s" );
                    }
                }
            }
            if ( isClean && loadAfterFirstStart && script != null && script.isFile() )
            {
                getLog().info( "Running " + script + "..." );
                int rv = Utils.runLoadScript( cassandraDir[0], newCliCommandLine( "--file", script.getAbsolutePath() ),
                                              createEnvironmentVars(), getLog() );
                if ( rv != 0 )
                {
                    if ( loadFailureIgnore )
                    {
                        getLog().error(
                            "Command exited with error code " + rv + ". Ignoring as loadFailureIgnore is true" );
                    }
                    else
                    {
                        throw new MojoExecutionException( "Command exited with error code " + rv );
                    }
                }
                else
                {
                    getLog().info( "Finished " + script + "." );
                }
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
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getLocalizedMessage(), e );
        }
    }
}
