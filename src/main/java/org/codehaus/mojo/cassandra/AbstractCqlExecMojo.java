package org.codehaus.mojo.cassandra;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.cassandra.thrift.Cassandra.Client;
import org.apache.cassandra.thrift.Compression;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.CqlResult;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.IOUtil;

/**
 * Abstract parent class for mojos that need to run CQL statements.
 *
 * @author sparhomenko
 */
public abstract class AbstractCqlExecMojo extends AbstractCassandraMojo
{
    /**
     * Version of CQL to use
     *
     * @parameter property="cql.version"
     * @since 1.2.1-2
     */
    private String cqlVersion = "3.4.0";

    /**
     * Charset used when loading CQL files. If not specified the system default encoding will be used.
     *
     * @parameter property="cql.encoding"
     * @since 3.6
     */
    protected String cqlEncoding = Charset.defaultCharset().name();

    protected String readFile(File file) throws MojoExecutionException
    {
        if (!file.isFile())
        {
            throw new MojoExecutionException("script " + file + " does not exist.");
        }

        InputStreamReader r = null;
        try
        {
            r = new InputStreamReader(new FileInputStream(file), cqlEncoding);
            return IOUtil.toString(r);
        } catch (FileNotFoundException e)
        {
            throw new MojoExecutionException("Cql file '" + file + "' was deleted before I could read it", e);
        } catch (IOException e)
        {
            throw new MojoExecutionException("Could not parse or load cql file", e);
        } finally
        {
            IOUtil.close(r);
        }
    }

    protected List<CqlResult> executeCql(final String statements) throws MojoExecutionException
    {
        final List<CqlResult> results = new ArrayList<CqlResult>();
        if (StringUtils.isBlank(statements))
        {
            getLog().warn("No CQL provided. Nothing to do.");
        } else
        {
            try
            {
                CqlExecOperation operation = new CqlExecOperation(statements);
                Utils.executeThrift(operation);
                results.addAll(operation.results);
            } catch (ThriftApiExecutionException taee)
            {
                throw new MojoExecutionException(taee.getMessage(), taee);
            }
        }
        return results;
    }

    private class CqlExecOperation extends ThriftApiOperation
    {
        private final List<CqlResult> results = new ArrayList<CqlResult>();
        private final String[] statements;

        private CqlExecOperation(String statements)
        {
            super(rpcAddress, rpcPort);
            this.statements = statements.split("(?m);\\s*$");
            if (StringUtils.isNotBlank(keyspace))
            {
                getLog().info("setting keyspace: " + keyspace);
                setKeyspace(keyspace);
            }
            getLog().info("setting cqlversion: " + cqlVersion);
            setCqlVersion(cqlVersion);
        }

        @Override
        void executeOperation(Client client) throws ThriftApiExecutionException
        {
            for (String statement : statements)
            {
                if (StringUtils.isNotBlank(statement))
                {
                    results.add(executeStatement(client, statement));
                }
            }
        }

        private CqlResult executeStatement(Client client, String statement) throws ThriftApiExecutionException
        {
            ByteBuffer buf = ByteBufferUtil.bytes(statement);
            try
            {
                if (cqlVersion.charAt(0) >= '3')
                {
                    return client.execute_cql3_query(buf, Compression.NONE, ConsistencyLevel.ONE);
                } else
                {
                    return client.execute_cql_query(buf, Compression.NONE);
                }
            } catch (Exception e)
            {
                getLog().debug(statement);
                throw new ThriftApiExecutionException(e);
            }
        }
    }

}
