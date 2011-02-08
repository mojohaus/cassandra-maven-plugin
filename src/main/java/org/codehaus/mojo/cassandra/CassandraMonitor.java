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

import org.apache.cassandra.thrift.CassandraDaemon;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A Monitor for controlling the Cassandra process.
 *
 * @author stephenc
 */
public class CassandraMonitor extends Thread
{
    public static final String PORT_PROPERTY_NAME = "STOP.PORT";

    public static final String KEY_PROPERTY_NAME = "STOP.KEY";

    private final String key;

    private ServerSocket serverSocket;

    /**
     * Creates a CassandraMonitor bound to the specified port on the localhost interface using the supplied key.
     *
     * @param port the port to bind to.
     * @param key  the key to require.
     * @throws IOException if something goes wrong.
     */
    public CassandraMonitor(int port, String key) throws IOException
    {
        this.key = key;
        serverSocket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
        serverSocket.setReuseAddress(true);
    }

    /**
     * {@inheritDoc}
     */
    public void run()
    {
        while (serverSocket != null)
        {
            Socket socket = null;
            try
            {
                socket = serverSocket.accept();
                socket.setSoLinger(false, 0);
                LineNumberReader lin = new LineNumberReader(new InputStreamReader(socket.getInputStream()));
                String key = lin.readLine();
                if (this.key.equals(key))
                {
                    String cmd = lin.readLine();
                    if ("stop".equals(cmd))
                    {
                        try
                        {
                            socket.close();
                        } catch (IOException e)
                        {
                            // ignore
                        }
                        try
                        {
                            socket.close();
                        } catch (IOException e)
                        {
                            // ignore
                        }
                        try
                        {
                            serverSocket.close();
                        } catch (IOException e)
                        {
                            // ignore
                        }
                        serverSocket = null;
                        System.out.println("Killing Cassandra");
                        System.exit(0);
                    } else
                    {
                        System.out.println("Unsupported monitor operation.");
                    }
                }
            } catch (IOException e)
            {
                e.printStackTrace();
            } finally
            {
                if (socket != null)
                {
                    try
                    {
                        socket.close();
                    } catch (IOException e)
                    {
                        // ignore
                    }
                }
                socket = null;
            }
        }
    }

    /**
     * Starts the {@link CassandraMonitor} and then delegates to {@link CassandraDaemon}.
     *
     * @param args the command line arguments.
     * @throws IOException if something goes wrong.
     */
    public static void main(String[] args) throws IOException
    {
        String property = System.getProperty(PORT_PROPERTY_NAME);
        String key = System.getProperty(KEY_PROPERTY_NAME);
        if (property != null && key != null)
        {
            int port = Integer.parseInt(property);
            CassandraMonitor monitor = new CassandraMonitor(port, key);
            monitor.setDaemon(true);
            monitor.start();
        }
        CassandraDaemon.main(args);
    }
}
