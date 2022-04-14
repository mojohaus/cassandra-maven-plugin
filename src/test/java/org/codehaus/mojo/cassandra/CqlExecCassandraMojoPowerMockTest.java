package org.codehaus.mojo.cassandra;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.ThrowsException;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.internal.stubbing.answers.DoesNothing.doesNothing;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest({FileReader.class, IOUtil.class, Utils.class})
@PowerMockIgnore({ "org.apache.xerces.*", "javax.xml.parsers.*", "org.xml.sax.*", "org.w3c.dom.*", "javax.management.*" })
public class CqlExecCassandraMojoPowerMockTest {

    private final static String CQL_STATEMENT = "CREATE KEYSPACE identifier WITH replication = {'class': 'SimpleStrategy'}";

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Mock
    private Log log;

    @Mock
    private Cassandra.Client client;

    private CqlExecCassandraMojoBuilder builder;

    @Before
    public void createCqlExecCassandraMojoBuilder() {
        this.builder = new CqlExecCassandraMojoBuilder(log);
    }

    @Test
    public void should_fail_if_file_not_found_occurs_when_reading_cql_script() {
        CqlExecCassandraMojo cqlExecCassandraMojo = builder.cqlScript(file("emptyfile.cql")).build();
        mockToThrows(new FileNotFoundException());

        try {
            cqlExecCassandraMojo.execute();
            fail();
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), allOf(startsWith("Cql file '"), endsWith("emptyfile.cql' was deleted before I could read it")));
        } catch (MojoFailureException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void should_fail_if_io_error_occurs_when_reading_cql_script() {
        CqlExecCassandraMojo cqlExecCassandraMojo = builder.cqlScript(file("emptyfile.cql")).build();
        mockToThrows(new IOException());

        try {
            cqlExecCassandraMojo.execute();
            fail();
        } catch (MojoExecutionException e) {
            assertEquals("Could not parse or load cql file", e.getMessage());
        } catch (MojoFailureException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void should_use_default_cql_version() {
        CqlExecCassandraMojo cqlExecCassandraMojo = builder.cqlStatement(CQL_STATEMENT).build();
        ArgumentCaptor<ThriftApiOperation> operation = mockThriftExecution();

        try {
            cqlExecCassandraMojo.execute();

            assertEquals("3.4.0", operation.getValue().getCqlVersion());
        } catch (MojoExecutionException | MojoFailureException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void should_use_custom_keyspace() {
        CqlExecCassandraMojo cqlExecCassandraMojo = builder.keyspace("identifier").cqlStatement(CQL_STATEMENT).build();
        ArgumentCaptor<ThriftApiOperation> operation = mockThriftExecution();

        try {
            cqlExecCassandraMojo.execute();

            assertEquals("identifier", operation.getValue().getKeyspace());
        } catch (MojoExecutionException | MojoFailureException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void should_fail_when_request_fails() {
        CqlExecCassandraMojo cqlExecCassandraMojo = builder.cqlStatement(CQL_STATEMENT).build();
        mockThriftExecutionWith(new ThrowsException(new ThriftApiExecutionException(new InvalidRequestException("bad statement"))));

        try {
            cqlExecCassandraMojo.execute();
            fail();
        } catch (MojoExecutionException e) {
            assertEquals("There was a problem calling Apache Cassandra's Thrift API. Details: The request was not properly formatted bad statement", e.getMessage());
        } catch (MojoFailureException e) {
            fail(e.getMessage());
        }
    }

    private ArgumentCaptor<ThriftApiOperation> mockThriftExecution() {
        return mockThriftExecutionWith(doesNothing());
    }

    private ArgumentCaptor<ThriftApiOperation> mockThriftExecutionWith(Answer<Object> answer) {
        mockStatic(Utils.class);
        ArgumentCaptor<ThriftApiOperation> operation = ArgumentCaptor.forClass(ThriftApiOperation.class);
        try {
            when(Utils.class, "executeThrift", operation.capture()).thenAnswer(answer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return operation;
    }

    private void mockToThrows(Throwable throwable) {
        try {
            mockStatic(IOUtil.class);
            when(IOUtil.toString(any(InputStreamReader.class))).thenThrow(throwable);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    private File file(String name) {
        try {
            return new File(getClass().getResource(name).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
