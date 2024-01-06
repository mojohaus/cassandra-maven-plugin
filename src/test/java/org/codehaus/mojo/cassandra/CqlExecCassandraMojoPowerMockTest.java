package org.codehaus.mojo.cassandra;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicReference;

import com.datastax.oss.driver.api.core.DriverExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.mockito.internal.stubbing.answers.DoesNothing.doesNothing;

public class CqlExecCassandraMojoPowerMockTest {

    private static final String CQL_STATEMENT =
            "CREATE KEYSPACE identifier WITH replication = {'class': 'SimpleStrategy'}";

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

            builder.cqlScript(file("emptyfile.cql")).build().execute();

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
        CqlExecCassandraMojo cqlExecCassandraMojo =
                builder.cqlScript(file("emptyfile.cql")).build();
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
        final AtomicReference<CqlOperation> cqlOperation = new AtomicReference<>();
        try (MockedStatic<Utils> mocked = mockStatic(Utils.class)) {
            mocked.when(() -> Utils.executeCql(ArgumentMatchers.any(CqlOperation.class)))
                    .thenAnswer(a -> {
                        cqlOperation.set(a.getArgument(0));
                        return doesNothing();
                    });

            builder.cqlStatement(CQL_STATEMENT).build().execute();

            assertThat(cqlOperation.get().getCqlVersion()).isEqualTo("3.11.12");
        }
    }

    @Test
    public void should_use_custom_keyspace() throws MojoExecutionException, MojoFailureException {
        final AtomicReference<CqlOperation> cqlOperation = new AtomicReference<>();
        try (MockedStatic<Utils> mocked = mockStatic(Utils.class)) {
            mocked.when(() -> Utils.executeCql(ArgumentMatchers.any(CqlOperation.class)))
                    .thenAnswer(a -> {
                        cqlOperation.set(a.getArgument(0));
                        return doesNothing();
                    });

            builder.keyspace("identifier").cqlStatement(CQL_STATEMENT).build().execute();

            assertThat(cqlOperation.get().getKeyspace()).isEqualTo("identifier");
        }
    }

    @Test
    public void should_fail_when_request_fails() {
        try (MockedStatic<Utils> mocked = mockStatic(Utils.class)) {
            mocked.when(() -> Utils.executeCql(ArgumentMatchers.any(CqlOperation.class)))
                    .thenThrow(new DriverExecutionException(new Exception("bad statement")));

            builder.cqlStatement(CQL_STATEMENT).build().execute();

        } catch (MojoExecutionException e) {
            assertThat(e).hasMessage("bad statement");
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
