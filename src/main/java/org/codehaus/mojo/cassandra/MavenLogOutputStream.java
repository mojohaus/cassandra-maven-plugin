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

import org.apache.commons.exec.LogOutputStream;

import org.apache.maven.plugin.logging.Log;

/**
 * Converts an output stream into a {@link Log}
 *
 * @author stephenc
 */
class MavenLogOutputStream extends LogOutputStream
{
    private final Log outputLog;

    /**
     * Constructs a new {@link LogOutputStream} that sends output to the specified {@link Log}.
     *
     * @param outputLog the {@link Log} to send output.
     */
    public MavenLogOutputStream(Log outputLog)
    {
        this.outputLog = outputLog;
    }

    /**
     * {@inheritDoc}
     */
    protected void processLine(String line, int level)
    {
        outputLog.info(line);
    }
}
