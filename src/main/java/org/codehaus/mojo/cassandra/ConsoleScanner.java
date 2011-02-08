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

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 *
 * @author stephenc
 */
public class ConsoleScanner extends Thread
{
    private final Object lock = new Object();

    private boolean finished = false;

    public ConsoleScanner()
    {
        setName("Console scanner");
        setDaemon(true);
    }

    @Override
    public void run()
    {
        try
        {
            while (!isFinished())
            {
                checkSystemInput();
                getSomeSleep();
            }
        } catch (IOException e)
        {
            // ignore
        } finally
        {
            finish();
        }
    }

    public boolean isFinished()
    {
        synchronized (lock)
        {
            return finished;
        }
    }

    private void getSomeSleep()
    {
        try
        {
            Thread.sleep(500);
        } catch (InterruptedException e)
        {
            // ignore
        }
    }

    private void checkSystemInput() throws IOException
    {
        while (System.in.available() > 0)
        {
            int input = System.in.read();
            if (input >= 0)
            {
                char c = (char) input;
                if (c == '\n')
                {
                    finish();
                }
            } else
            {
                finish();
            }
        }
    }

    public void finish()
    {
        synchronized (lock)
        {
            finished = true;
            lock.notifyAll();
        }
    }

    public void waitForFinished() throws InterruptedException
    {
        synchronized (lock)
        {
            while (!finished)
            {
                lock.wait();
            }
        }
    }
}
