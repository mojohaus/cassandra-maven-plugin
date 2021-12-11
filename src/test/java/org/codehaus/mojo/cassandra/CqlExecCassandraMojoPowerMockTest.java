package org.codehaus.mojo.cassandra;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.DoesNothing;
import org.mockito.internal.stubbing.answers.ThrowsException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class CqlExecCassandraMojoPowerMockTest {

    private final static String CQL_STATEMENT = "CREATE KEYSPACE identifier WITH replication = {'class': 'SimpleStrategy'}";

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
        try ( MockedStatic<Files> ioutil = Mockito.mockStatic( Files.class ) )
        {
            ioutil.when( () -> Files.readAllLines( Mockito.any( Path.class ), Mockito.any( Charset.class ) ) )
                    .thenThrow( new FileNotFoundException() );

            cqlExecCassandraMojo.execute();
            fail();
        }
        catch ( MojoExecutionException e )
        {
            MatcherAssert.assertThat( e.getMessage(), allOf( startsWith( "script " ),
                    endsWith( "emptyfile.cql does not exist." ) ) );
        }
        catch ( MojoFailureException e )
        {
            fail( e.getMessage() );
        }
    }

    @Test
    public void should_fail_if_io_error_occurs_when_reading_cql_script() {
        CqlExecCassandraMojo cqlExecCassandraMojo = builder.cqlScript(file("emptyfile.cql")).build();
        try ( MockedStatic<Files> ioutil = Mockito.mockStatic( Files.class ) )
        {
            ioutil.when( () -> Files.readAllLines( Mockito.any( Path.class ),  Mockito.any( Charset.class ) ) )
                    .thenThrow( new IOException() );
            cqlExecCassandraMojo.execute();
            fail();
        } catch (MojoExecutionException e) {
            MatcherAssert.assertThat( e.getMessage(), allOf( startsWith( "script " ),
                    endsWith( "emptyfile.cql does not exist." ) ) );
//            assertEquals("Could not parse or load cql file", e.getMessage());
        } catch (MojoFailureException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void should_use_default_cql_version() throws MojoExecutionException, MojoFailureException
    {
        CqlExecCassandraMojo cqlExecCassandraMojo = builder.cqlStatement( CQL_STATEMENT ).build();
        ArgumentCaptor<ThriftApiOperation> operation = ArgumentCaptor.forClass( ThriftApiOperation.class );
        try ( MockedStatic<Utils> utils = Mockito.mockStatic( Utils.class ) )
        {
            utils.when( () -> Utils.executeThrift( operation.capture() ) )
                    .thenAnswer( DoesNothing.doesNothing() );

            cqlExecCassandraMojo.execute();

            assertEquals( "3.11.11", operation.getValue().getCqlVersion() );
        }
    }

    @Test
    public void should_use_custom_keyspace() throws MojoExecutionException, MojoFailureException
    {
        CqlExecCassandraMojo cqlExecCassandraMojo = builder.keyspace("identifier")
                .cqlStatement(CQL_STATEMENT).build();

        ArgumentCaptor<ThriftApiOperation> operation = ArgumentCaptor.forClass( ThriftApiOperation.class );
        try ( MockedStatic<Utils> utils = Mockito.mockStatic( Utils.class ) )
        {
            utils.when( () -> Utils.executeThrift( operation.capture() ) )
                    .thenAnswer( DoesNothing.doesNothing() );

            cqlExecCassandraMojo.execute();

            assertEquals("identifier", operation.getValue().getKeyspace());
        }
    }

    @Test
    public void should_fail_when_request_fails() {
        CqlExecCassandraMojo cqlExecCassandraMojo = builder.cqlStatement(CQL_STATEMENT).build();
        ArgumentCaptor<ThriftApiOperation> operation = ArgumentCaptor.forClass( ThriftApiOperation.class );
        try ( MockedStatic<Utils> utils = Mockito.mockStatic( Utils.class ) )
        {
            utils.when( () -> Utils.executeThrift( operation.capture() ) )
                    .thenAnswer( new ThrowsException(new ThriftApiExecutionException(new InvalidRequestException("bad statement"))) );

           cqlExecCassandraMojo.execute();
            fail();
        } catch (MojoExecutionException e) {
            assertEquals("There was a problem calling Apache Cassandra's Thrift API. Details: The request was not properly formatted bad statement", e.getMessage());
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
