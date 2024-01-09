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
@Mojo(name = "stop", threadSafe = true, defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class StopCassandraMojo extends AbstractMojo
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
     * port for the CQL native transport to listen for clients on.
     *
     */
    @Parameter(property="cassandra.nativeTransportPort", defaultValue="9042")
    protected int nativeTransportPort;

    /**
     * Address to bind to and tell other Cassandra nodes to connect to. You
     * <strong>must</strong> change this if you want multiple nodes to be able to
     * communicate!
     *
     * Leaving it blank leaves it up to InetAddress.getLocalHost(). This
     * will always do the Right Thing <em>if</em> the node is properly configured
     * (hostname, name resolution, etc), and the Right Thing is to use the
     * address associated with the hostname (it might not be).
     *
     * Setting this to 0.0.0.0 is always wrong.
     * Do not change this unless you really know what you are doing.
     *
     */
    @Parameter(defaultValue = "127.0.0.1")
    protected String listenAddress;

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

        Utils.stopCassandraServer(rpcAddress, nativeTransportPort, listenAddress, stopPort, stopKey, getLog());
    }
}
