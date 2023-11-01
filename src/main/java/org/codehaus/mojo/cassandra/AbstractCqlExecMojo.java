package org.codehaus.mojo.cassandra;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverExecutionException;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.Token;
import org.apache.cassandra.cql3.CqlLexer;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
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
     * @since 1.2.1-2
     */
    @Parameter(property="cql.version", defaultValue = "3.11.12")
    private String cqlVersion = "3.11.12";

    /**
     * Charset used when loading CQL files. If not specified the system default encoding will be used.
     *
     * @since 3.6
     */
    @Parameter(property="cql.encoding")
    protected String cqlEncoding = Charset.defaultCharset().name();

    /**
     * Should we use the CqlLexer when loading the cql file. This should be better than than the default behaviour
     * which is to just split input on ; since it handles ; in comments and strings.
     *
     * It is not enabled by default since has not been extensively tested.
     *
     * @since 3.7
     */
    @Parameter(defaultValue = "false")
    protected boolean useCqlLexer = false;

    protected String readFile(File file) throws MojoExecutionException
    {
        if (!file.isFile() || !file.exists())
        {
            throw new MojoExecutionException("script " + file + " does not exist.");
        }

        try (InputStreamReader r = new InputStreamReader(new FileInputStream(file), cqlEncoding))
        {
            return IOUtil.toString(r);
        } catch (FileNotFoundException e)
        {
            throw new MojoExecutionException("Cql file '" + file + "' was deleted before I could read it", e);
        } catch (IOException e)
        {
            throw new MojoExecutionException("Could not parse or load cql file", e);
        }
    }

    protected List<Row> executeCql(final String statements) throws MojoExecutionException {
        final List<Row> results = new ArrayList<>();
        if (StringUtils.isBlank(statements)) {
            getLog().warn("No CQL provided. Nothing to do.");
        } else {
            try {
                CqlStatementOperation cqlStatementOperation = new CqlStatementOperation(statements);
                Utils.executeCql(cqlStatementOperation);
                results.addAll(cqlStatementOperation.results);
            } catch (DriverExecutionException e) {
                throw new MojoExecutionException(e.getCause().getMessage(), e);
            }
        }
        return results;
    }

    /**
     * Best effort to somewhat parse the cql input instead of just splitting on ; which
     * breaks badly if you have ; in strings or comments.
     * Parsing is done using the CqlLexer class
     */
    protected static List<String> splitStatementsUsingCqlLexer(String statements) {
        ANTLRStringStream stream = new ANTLRStringStream(statements);
        CqlLexer lexer = new CqlLexer(stream);
        List<String> statementList = new ArrayList<String>();
        StringBuilder currentStatement = new StringBuilder();
        // Not the prettiest code i ever wrote, but it gets the job done.
        for (Token token = lexer.nextToken(); token.getType() != Token.EOF; token = lexer.nextToken()) {
            if (token.getText().equals(";")) {
                // when we meet a ; terminate current statement and prepare the next
                currentStatement.append(";");
                statementList.add(currentStatement.toString());
                currentStatement = new StringBuilder();
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
        return statementList;
    }

    private class CqlStatementOperation extends CqlOperation {

        private final List<Row> results = new ArrayList<>();
        private final List<String> statements;

        private CqlStatementOperation(String statements) {
            super(rpcAddress, nativeTransportPort);
            if (useCqlLexer) {
                getLog().warn("Using CqlLexer has not been extensively tested");
                this.statements = splitStatementsUsingCqlLexer(statements);
            } else {
                this.statements = Arrays.asList(statements.split(";"));
            }
            if (StringUtils.isNotBlank(keyspace)) {
                getLog().info("setting keyspace: " + keyspace);
                setKeyspace(keyspace);
            }
            getLog().info("setting cqlversion: " + cqlVersion);
            setCqlVersion(cqlVersion);
        }

        @Override
        void executeOperation(CqlSession cqlSession) throws CqlExecutionException {
            for (String statement : statements) {
                if (StringUtils.isNotBlank(statement)) {
                    if (getLog().isDebugEnabled()) {
                        getLog().debug("Executing cql statement: " + statement);
                    }
                    try {
                        ResultSet resultSet = cqlSession.execute(statement);
                        results.addAll(resultSet.all());
                    } catch (Exception e) {
                        getLog().debug(statement);
                        throw new CqlExecutionException(e);
                    }
                }
            }
        }
    }
}
