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

import com.datastax.oss.driver.api.core.CqlSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.dataset.ParseException;
import org.cassandraunit.dataset.cql.FileCQLDataSet;

import java.io.File;
import java.net.InetSocketAddress;

/**
 * Loads a CassandraUnit DataSet into a Cassandra instance.
 *
 * @author jsevellec
 * @goal cu-load
 * @threadSafe
 * @phase pre-integration-test
 * @since 1.2.1-2
 */
public class LoadCassandraUnitDataSetMojo
    extends AbstractCassandraMojo
{
    /**
     * The CassandraUnit dataSet to load.
     *
     * @parameter default-value="${basedir}/src/test/resources/dataSet.xml"
     */
    protected File cuDataSet;

    /**
     * Whether to ignore errors when loading the dataSet.
     *
     * @parameter property="cassandra.cuload.failure.ignore"
     */
    private boolean cuLoadFailureIgnore;

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

        if ( !cuDataSet.isFile() )
        {
            if ( cuLoadFailureIgnore )
            {
                getLog().error( "CassandraUnit dataSet " + cuDataSet + " does not exist."
                                    + ". Ignoring as cuLoadFailureIgnore is true" );
                return;
            }
            else
            {
                throw new MojoFailureException( "CassandraUnit dataSet " + cuDataSet + " does not exist." );
            }
        }

        try
        {
            CqlSession session = CqlSession.builder()
                    .withApplicationName( "cassandraUnitCluster" )
                    .addContactPoint( new InetSocketAddress( rpcAddress, rpcPort ) )
                    .build();
            CQLDataLoader dataLoader = new CQLDataLoader( session );
            dataLoader.load( new FileCQLDataSet( cuDataSet.getAbsolutePath() ) );
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
    }
}
