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
package smoke;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.*;

public class SmokeIT
{
    @Test
    public void connectToKeyspace() throws Exception
    {
        TTransport tr = new TFramedTransport(new TSocket("localhost", Integer.getInteger( "rpcPort", 9160 )));
        TProtocol proto = new TBinaryProtocol(tr);
        Cassandra.Client client = new Cassandra.Client(proto);
        tr.open();
        try
        {
            assertThat(client.describe_keyspace("testkeyspace").getStrategy_options().entrySet(),
                    hasItem((Map.Entry<String, String>)new AbstractMap.SimpleEntry<String,String>("replication_factor","1")));
        } finally
        {
            tr.close();
        }
    }
}
