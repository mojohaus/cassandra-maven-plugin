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
import org.apache.cassandra.thrift.InvalidRequestException;

import org.apache.commons.exec.*;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.codehaus.plexus.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Utility classes for interacting with Cassandra.
 *
 * @author stephenc
 */
public final class Utils
{
    /**
     * Do not instantiate.
     */
    private Utils()
    {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Stops the Cassandra service.
     *
     * @param rpcAddress The rpcAddress to connect to in order to see if Cassandra has stopped.
     * @param rpcPort    The rpcPort to connect on to check if Cassandra has stopped.
     * @param stopPort   The port to stop on.
     * @param stopKey    The key to stop with,
     * @param log        The log to write to.
     */
    static void stopCassandraServer(String rpcAddress, int rpcPort, String stopAddress, int stopPort, String stopKey, Log log)
    {
        try
        {
            Socket s = new Socket(InetAddress.getByName(stopAddress), stopPort);
            s.setSoLinger(false, 0);

            OutputStream out = s.getOutputStream();
            out.write((stopKey + "\r\nstop\r\n").getBytes());
            out.flush();
            s.close();
        } catch (ConnectException e)
        {
            log.info("Cassandra not running!");
            return;
        } catch (Exception e)
        {
            log.error(e);
            return;
        }
        log.info("Waiting for Cassandra to stop...");
        long maxWaiting = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
        boolean stopped = false;
        while (!stopped && System.currentTimeMillis() < maxWaiting)
        {
            TTransport tr = new TFramedTransport(new TSocket(rpcAddress, rpcPort));
            try
            {
                TProtocol proto = new TBinaryProtocol(tr);
                Cassandra.Client client = new Cassandra.Client(proto);
                try
                {
                    tr.open();
                } catch (TTransportException e)
                {
                    if (e.getCause() instanceof ConnectException)
                    {
                        stopped = true;
                        continue;
                    }
                    log.debug(e.getLocalizedMessage(), e);
                    try
                    {
                        Thread.sleep(500);
                    } catch (InterruptedException e1)
                    {
                        // ignore
                    }
                }
            } finally
            {
                if (tr.isOpen())
                {
                    tr.close();
                }
            }
        }
        if (stopped)
        {
            log.info("Cassandra has stopped.");
        } else
        {
            log.warn("Gave up waiting for Cassandra to stop.");
        }
    }

    /**
     * Starts the Cassandra server.
     *
     * @param cassandraDir The directory to start the Server process in.
     * @param commandLine  The command line to use to start the Server process.
     * @param environment  The environment to start the Server process with.
     * @param log          The log to send the output to.
     * @return The {@link ExecuteResultHandler} for the started process.
     * @throws MojoExecutionException if something went wrong.
     */
    protected static DefaultExecuteResultHandler startCassandraServer(File cassandraDir, CommandLine commandLine,
                                                                      Map environment, Log log)
            throws MojoExecutionException
    {

        try
        {
            Executor exec = new DefaultExecutor();
            DefaultExecuteResultHandler execHandler = new DefaultExecuteResultHandler();
            exec.setWorkingDirectory(cassandraDir);
            exec.setProcessDestroyer(new ShutdownHookProcessDestroyer());

            LogOutputStream stdout = new MavenLogOutputStream(log);
            LogOutputStream stderr = new MavenLogOutputStream(log);

            log.debug("Executing command line: " + commandLine);

            exec.setStreamHandler(new PumpStreamHandler(stdout, stderr));

            exec.execute(commandLine, environment, execHandler);

            return execHandler;
        } catch (ExecuteException e)
        {
            throw new MojoExecutionException("Command execution failed.", e);
        } catch (IOException e)
        {
            throw new MojoExecutionException("Command execution failed.", e);
        }
    }

    /**
     * Returns {@code true} if the resource is not a file, does not exist or is older than the project file.
     *
     * @param project  the project that the resource is dependent on.
     * @param resource the resource to query.
     * @return {@code true} if the resource is not a file, does not exist or is older than the project file.
     */
    static boolean shouldGenerateResource(MavenProject project, File resource)
    {
        if (!resource.isFile())
        {
            return true;
        }
        long resourceLM = resource.lastModified();
        long projectLM = project.getFile().lastModified();
        if (Long.signum(resourceLM) == Long.signum(projectLM))
        {
            // the two dates are in the same epoch or else the universe is lasting a really long time.
            return resourceLM < projectLM;
        }
        // the universe has been around long enough that we should rewrite the resource.
        return true;
    }

    /**
     * Applies the glossYaml on top of the baseYaml and returns the result.
     *
     * @param baseYaml  the base Yaml.
     * @param glossYaml the Yaml to overide the base with.
     * @return the resulting Yaml.
     */
    public static String merge(String baseYaml, String glossYaml)
    {
        if (StringUtils.isBlank(glossYaml))
        {
            return baseYaml;
        }
        if (StringUtils.isBlank(baseYaml))
        {
            return glossYaml;
        }
        Yaml yaml = new Yaml();
        Map<String, Object> baseMap = (Map<String, Object>) yaml.load(baseYaml);
        Map<String, Object> glossMap = (Map<String, Object>) yaml.load(glossYaml);
        for (Map.Entry<String, Object> glossEntry : glossMap.entrySet())
        {
            baseMap.put(glossEntry.getKey(), glossEntry.getValue());
        }
        return yaml.dump(baseMap);
    }

    /**
     * Waits until the Cassandra server at the specified RPC address and port has started accepting connections.
     *
     * @param rpcAddress       The RPC address to connect to.
     * @param rpcPort          The RPC port to connect on.
     * @param startWaitSeconds The maximum number of seconds to wait.
     * @param log              the {@link Log} to log to.
     * @return {@code true} if Cassandra is started.
     * @throws MojoExecutionException if something went wrong.
     */
    static boolean waitUntilStarted(String rpcAddress, int rpcPort, int startWaitSeconds, Log log)
            throws MojoExecutionException
    {
        long maxWaiting = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(startWaitSeconds);
        while (startWaitSeconds == 0 || System.currentTimeMillis() < maxWaiting)
        {
            TTransport tr = new TFramedTransport(new TSocket(rpcAddress, rpcPort));
            try
            {
                TProtocol proto = new TBinaryProtocol(tr);
                Cassandra.Client client = new Cassandra.Client(proto);
                try
                {
                    tr.open();
                } catch (TTransportException e)
                {
                    if (!(e.getCause() instanceof ConnectException))
                    {
                        log.debug(e.getLocalizedMessage(), e);
                    }
                    try
                    {
                        Thread.sleep(500);
                    } catch (InterruptedException e1)
                    {
                        // ignore
                    }
                    continue;
                }
                try
                {
                    log.info("Cassandra cluster \"" + client.describe_cluster_name() + "\" started.");
                    return true;
                } catch (TException e)
                {
                    throw new MojoExecutionException(e.getLocalizedMessage(), e);
                }
            } finally
            {
                if (tr.isOpen())
                {
                    tr.close();
                }
            }
        }
        return false;
    }

    /**
     * Call {@link #executeOperation(Cassandra.Client)} on the provided operation
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */    
    public static void executeThrift(ThriftApiOperation thriftApiOperation) throws MojoExecutionException
    {
        TSocket socket = new TSocket(thriftApiOperation.getRpcAddress(), thriftApiOperation.getRpcPort());
        TTransport transport = new TFramedTransport(socket);

        TBinaryProtocol binaryProtocol = new TBinaryProtocol(transport, true, true);
        Cassandra.Client cassandraClient = new Cassandra.Client(binaryProtocol);

        try 
        {
            transport.open();
            if ( StringUtils.isNotBlank(thriftApiOperation.getKeyspace()) ) 
            {
                cassandraClient.set_keyspace(thriftApiOperation.getKeyspace());
            }
            cassandraClient.set_cql_version(thriftApiOperation.getCqlVersion());
            thriftApiOperation.executeOperation(cassandraClient);
        } catch (ThriftApiExecutionException taee) 
        {
            throw new MojoExecutionException("API Exception calling Apache Cassandra", taee);
        } catch (Exception ex) 
        {
            throw new MojoExecutionException("General exception from Thrift", ex);
        }
        
        finally 
        {
            if ( transport != null && transport.isOpen() )
            {
                try 
                {
                    transport.flush();
                    transport.close();
                } catch (Exception e) 
                { 
                    throw new MojoExecutionException("Something went wrong cleaning up", e);
                }
            }
        }

    }
}
