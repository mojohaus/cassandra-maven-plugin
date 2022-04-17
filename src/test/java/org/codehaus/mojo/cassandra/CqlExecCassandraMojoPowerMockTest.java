package org.codehaus.mojo.cassandra;

import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.mockito.internal.stubbing.answers.DoesNothing.doesNothing;

public class CqlExecCassandraMojoPowerMockTest {

    private final static String CQL_STATEMENT = "CREATE KEYSPACE identifier WITH replication = {'class': 'SimpleStrategy'}";

    private CqlExecCassandraMojoBuilder builder;

    @Before
    public void createCqlExecCassandraMojoBuilder() {
        this.builder = new CqlExecCassandraMojoBuilder(mock(Log.class));
    }

    @Test
    public void should_fail_if_file_not_found_occurs_when_reading_cql_script() {
        try (MockedStatic<IOUtil> mocked = mockStatic(IOUtil.class)) {
            mocked.when(() -> IOUtil.toString(ArgumentMatchers.any(InputStreamReader.class)))
                    .thenThrow(new FileNotFoundException());

            builder.cqlScript(file("emptyfile.cql")).build()
                    .execute();

            fail();
        } catch (MojoExecutionException e) {
            assertThat(e)
                    .hasMessageStartingWith("Cql file '")
                    .hasMessageEndingWith("emptyfile.cql' was deleted before I could read it");
        } catch (MojoFailureException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void should_fail_if_io_error_occurs_when_reading_cql_script() {
        CqlExecCassandraMojo cqlExecCassandraMojo = builder.cqlScript(file("emptyfile.cql")).build();
        try (MockedStatic<IOUtil> mocked = mockStatic(IOUtil.class)) {
            mocked.when(() -> IOUtil.toString(ArgumentMatchers.any(InputStreamReader.class)))
                    .thenThrow(new IOException());

            cqlExecCassandraMojo.execute();

            fail();
        } catch (MojoExecutionException e) {
            assertThat(e).hasMessage("Could not parse or load cql file");
        } catch (MojoFailureException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void should_use_default_cql_version() throws MojoExecutionException, MojoFailureException {
        final AtomicReference<ThriftApiOperation> thriftApiOperation = new AtomicReference<>();
        try (MockedStatic<Utils> mocked = mockStatic(Utils.class)) {
            mocked.when(() -> Utils.executeThrift(ArgumentMatchers.any(ThriftApiOperation.class)))
                    .thenAnswer( a -> {
                        thriftApiOperation.set(a.getArgument(0));
                        return doesNothing();
                    });

            builder.cqlStatement(CQL_STATEMENT).build().execute();

            assertThat(thriftApiOperation.get().getCqlVersion()).isEqualTo("3.11.12");
        }
    }

    @Test
    public void should_use_custom_keyspace() throws MojoExecutionException, MojoFailureException {
        final AtomicReference<ThriftApiOperation> thriftApiOperation = new AtomicReference<>();
        try (MockedStatic<Utils> mocked = mockStatic(Utils.class)) {
            mocked.when(() -> Utils.executeThrift(ArgumentMatchers.any(ThriftApiOperation.class)))
                    .thenAnswer(a -> {
                        thriftApiOperation.set(a.getArgument(0));
                        return doesNothing();
                    });

            builder.keyspace("identifier").cqlStatement(CQL_STATEMENT).build()
                    .execute();

            assertThat(thriftApiOperation.get().getKeyspace()).isEqualTo("identifier");
        }
    }

    @Test
    public void should_fail_when_request_fails() {
        try (MockedStatic<Utils> mocked = mockStatic(Utils.class)) {
            mocked.when(() -> Utils.executeThrift(ArgumentMatchers.any(ThriftApiOperation.class)))
                    .thenThrow(new ThriftApiExecutionException(new InvalidRequestException("bad statement")));

            builder.cqlStatement(CQL_STATEMENT).build().execute();

        } catch (MojoExecutionException e) {
            assertThat(e).hasMessage("There was a problem calling Apache Cassandra's Thrift API. " +
                    "Details: The request was not properly formatted bad statement");
        } catch (MojoFailureException e) {
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
