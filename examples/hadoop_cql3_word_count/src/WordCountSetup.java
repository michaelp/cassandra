/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.nio.ByteBuffer;
import java.util.*;

import org.apache.cassandra.thrift.*;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WordCountSetup
{
    private static final Logger logger = LoggerFactory.getLogger(WordCountSetup.class);

    public static final int TEST_COUNT = 6;

    public static void main(String[] args) throws Exception
    {
        Cassandra.Iface client = createConnection();

        setupKeyspace(client);
        client.set_keyspace(WordCount.KEYSPACE);
        setupTable(client);
        insertData(client);

        System.exit(0);
    }

    private static void setupKeyspace(Cassandra.Iface client)  
            throws InvalidRequestException, 
            UnavailableException, 
            TimedOutException, 
            SchemaDisagreementException, 
            TException
    {
        KsDef ks;
        try
        {
            ks = client.describe_keyspace(WordCount.KEYSPACE);
        }
        catch(NotFoundException e)
        {
            logger.info("set up keyspace " + WordCount.KEYSPACE);
            String query = "CREATE KEYSPACE " + WordCount.KEYSPACE +
                              " WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 1}"; 

            client.execute_cql3_query(ByteBufferUtil.bytes(query), Compression.NONE, ConsistencyLevel.ONE);

            int magnitude = client.describe_ring(WordCount.KEYSPACE).size();
            try
            {
                Thread.sleep(1000 * magnitude);
            }
            catch (InterruptedException ie)
            {
                throw new RuntimeException(ie);
            }
        }
    }

    private static void setupTable(Cassandra.Iface client)  
            throws InvalidRequestException, 
            UnavailableException, 
            TimedOutException, 
            SchemaDisagreementException, 
            TException
    {
        String query = "CREATE TABLE " + WordCount.KEYSPACE + "."  + WordCount.COLUMN_FAMILY + 
                          " ( user_id text," +
                          "   category_id text, " +
                          "   sub_category_id text," +
                          "   title  text," +
                          "   body  text," +
                          "   PRIMARY KEY (user_id, category_id, sub_category_id) ) ";

        try
        {
            logger.info("set up table " + WordCount.COLUMN_FAMILY);
            client.execute_cql3_query(ByteBufferUtil.bytes(query), Compression.NONE, ConsistencyLevel.ONE);
        }
        catch (InvalidRequestException e)
        {
            logger.error("failed to create table " + WordCount.KEYSPACE + "."  + WordCount.COLUMN_FAMILY, e);
        }

        query = "CREATE INDEX title on " + WordCount.COLUMN_FAMILY + "(title)";
        try
        {
            logger.info("set up index on title column ");
            client.execute_cql3_query(ByteBufferUtil.bytes(query), Compression.NONE, ConsistencyLevel.ONE);
        }
        catch (InvalidRequestException e)
        {
            logger.error("Failed to create index on title", e);
        }

        query = "CREATE TABLE " + WordCount.KEYSPACE + "."  + WordCount.OUTPUT_COLUMN_FAMILY + 
                " ( row_id text," +
                "   word text, " +
                "   count_num text," +
                "   PRIMARY KEY (row_id, word) ) ";

        try
        {
            logger.info("set up table " + WordCount.OUTPUT_COLUMN_FAMILY);
            client.execute_cql3_query(ByteBufferUtil.bytes(query), Compression.NONE, ConsistencyLevel.ONE);
        }
        catch (InvalidRequestException e)
        {
            logger.error("failed to create table " + WordCount.KEYSPACE + "."  + WordCount.OUTPUT_COLUMN_FAMILY, e);
        }
    }
    
    private static Cassandra.Iface createConnection() throws TTransportException
    {
        if (System.getProperty("cassandra.host") == null || System.getProperty("cassandra.port") == null)
        {
            logger.warn("cassandra.host or cassandra.port is not defined, using default");
        }
        return createConnection(System.getProperty("cassandra.host", "localhost"),
                                Integer.valueOf(System.getProperty("cassandra.port", "9160")));
    }

    private static Cassandra.Client createConnection(String host, Integer port) throws TTransportException
    {
        TSocket socket = new TSocket(host, port);
        TTransport trans = new TFramedTransport(socket);
        trans.open();
        TProtocol protocol = new TBinaryProtocol(trans);

        return new Cassandra.Client(protocol);
    }

    private static void insertData(Cassandra.Iface client) 
            throws InvalidRequestException, 
            UnavailableException, 
            TimedOutException, 
            SchemaDisagreementException, 
            TException
    {
        String query = "INSERT INTO " + WordCount.COLUMN_FAMILY +  
                           "(user_id, category_id, sub_category_id, title, body ) " +
                           " values (?, ?, ?, ?, ?) ";
        CqlPreparedResult result = client.prepare_cql3_query(ByteBufferUtil.bytes(query), Compression.NONE);

        String [] title = titleData();
        String [] body = bodyData();
        for (int i=1; i<5; i++)
        {         
            for (int j=1; j<444; j++) 
            {
                for (int k=1; k<4; k++)
                {
                    List<ByteBuffer> values = new ArrayList<ByteBuffer>();
                    values.add(ByteBufferUtil.bytes(String.valueOf(j)));
                    values.add(ByteBufferUtil.bytes(String.valueOf(i)));
                    values.add(ByteBufferUtil.bytes(String.valueOf(k)));
                    values.add(ByteBufferUtil.bytes(title[i]));
                    values.add(ByteBufferUtil.bytes(body[i]));
                    client.execute_prepared_cql3_query(result.itemId, values, ConsistencyLevel.ONE);
                }
            }
        } 
    }

    private static String[] bodyData()
    {   // Public domain context, source http://en.wikisource.org/wiki/If%E2%80%94
        return new String[]{
                "",
                "If you can keep your head when all about you",
                "Are losing theirs and blaming it on you",
                "If you can trust yourself when all men doubt you,",
                "But make allowance for their doubting too:",
                "If you can wait and not be tired by waiting,"
        };
    }

    private static String[] titleData()
    {   // Public domain context, source http://en.wikisource.org/wiki/If%E2%80%94
        return new String[]{
                "",
                "A",
                "B",
                "C",
                "D",
                "E"            
        };
    }
}
