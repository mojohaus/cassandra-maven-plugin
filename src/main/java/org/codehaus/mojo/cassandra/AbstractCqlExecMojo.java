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

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.apache.cassandra.cql3.CqlLexer;
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

    /**
     * Should we use the CqlLexer when loading the cql file. This should be better than than the default behaviour
     * which is to just split input on ; since it handles ; in comments and strings.
     *
     * It is not enabled by default since has not been extensively tested.
     *
     * @parameter default-value=false
     * @since 3.6
     */
    protected boolean useCqlLexer = false;

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

    /**
     * Best effort to somewhat parse the cql input instead of just splitting on ; which
     * breaks badly if you have ; in strings or comments.
     * Parsing is done using the CqlLexer class
     */
    protected static String[] splitStatementsUsingCqlLexer(String statements) {
        ANTLRStringStream stream = new ANTLRStringStream(statements);
        CqlLexer lexer = new CqlLexer(stream);
        ArrayList<String> statementList = new ArrayList<String>();
        StringBuffer currentStatement = new StringBuffer();
        // Not the prettiest code i ever wrote, but it gets the job done.
        for (Token token = lexer.nextToken(); token.getType() != Token.EOF; token = lexer.nextToken()) {
            if (token.getText().equals(";")) {
                // when we meet a ; terminate current statement and prepare the next
                currentStatement.append(";");
                statementList.add(currentStatement.toString());
                currentStatement = new StringBuffer();
            } else if (token.getType() == CqlLexer.STRING_LITERAL) {
                // If we meet a string we should quote it and escape any enclosed ' as ''
                currentStatement.append("'");
                // TODO: There must be a cassandra util method somewhere that escapes a string for sql
                currentStatement.append(token.getText().replaceAll("'", "''"));
                currentStatement.append("'");
            } else if (token.getType() == CqlLexer.COMMENT) {
                // skip
            } else {
                currentStatement.append(token.getText());
            }
        }
        if (currentStatement.length() > 0 && currentStatement.toString().trim().length() > 0) {
            statementList.add(currentStatement.toString());
        }
        return statementList.toArray(new String[statementList.size()]);
    }


    private class CqlExecOperation extends ThriftApiOperation
    {
        private final List<CqlResult> results = new ArrayList<CqlResult>();
        private final String[] statements;

        private CqlExecOperation(String statements)
        {
            super(rpcAddress, rpcPort);
            if (useCqlLexer) {
                getLog().warn("********************************************************************************");
                getLog().warn("Using CqlLexer has not been extensively tested");
                this.statements = splitStatementsUsingCqlLexer(statements);
            } else {
                this.statements = statements.split(";");
            }
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
                    if (getLog().isDebugEnabled()) {
                        getLog().debug("Executing cql statement: " + statement);
                    }
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
