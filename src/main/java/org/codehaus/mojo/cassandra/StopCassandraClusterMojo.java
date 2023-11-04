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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Stops a background Cassandra instance.
 *
 * @author stephenc
 *
 */
@Mojo(name = "stop-cluster", threadSafe = true, defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class StopCassandraClusterMojo extends AbstractMojo
{
    /**
     * Skip the execution.
     *
     */
    @Parameter(property="cassandra.skip", defaultValue="false")
    private boolean skip;

    /**
     * Port to send stop command over
     *
     *
     */
    @Parameter(property="cassandra.stopPort", defaultValue="8081", required = true)
    protected int stopPort;

    /**
     * Key to provide when stopping cassandra
     *
     */
    @Parameter(property="cassandra.stopKey", defaultValue="cassandra-maven-plugin", required = true)
    protected String stopKey;

    /**
     * Address to use for the RPC interface. Do not change this unless you really know what you are doing.
     *
     */
    @Parameter(defaultValue="127.0.0.1")
    private String rpcAddress;

    /**
     * Port to listen to for the RPC interface.
     *
     */
    @Parameter(property="cassandra.rpcPort", defaultValue="9160")
    protected int rpcPort;

    @Parameter(property="cassandra.nativeTransportPort", defaultValue="9042")
    protected int nativeTransportPort;

    /**
     * The number of nodes in the cluster.
     *
     */
    @Parameter(property="cassandra.cluster.size", defaultValue="4")
    private int clusterSize;

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
        if (stopPort <= 0)
        {
            throw new MojoExecutionException("Please specify a valid port");
        }
        if (stopKey == null)
        {
            throw new MojoExecutionException("Please specify a valid stopKey");
        }
        if (clusterSize < 1) {
            throw new MojoExecutionException("Invalid cluster size of " + clusterSize + " specified. Must be at least 1");
        }
        if (clusterSize > 254) {
            throw new MojoExecutionException("Invalid cluster size of " + clusterSize + " specified. Must be less than 254");
        }
        for (int node = 0; node < clusterSize; node++) {
            Utils.stopCassandraServer("127.0.0." + (node + 1), nativeTransportPort, "127.0.0." + (node + 1), stopPort, stopKey, getLog());
        }

    }
}
