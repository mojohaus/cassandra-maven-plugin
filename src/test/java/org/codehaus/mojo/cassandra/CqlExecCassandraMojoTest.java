package org.codehaus.mojo.cassandra;

import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.exceptions.SyntaxException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.logging.Level;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;

@RunWith( MockitoJUnitRunner.class )
public class CqlExecCassandraMojoTest {

    @Mock
    private Log log;

    private CqlExecCassandraMojoBuilder builder;

    @Before
    public void createCqlExecCassandraMojoBuilder() {
        this.builder = new CqlExecCassandraMojoBuilder(log);
    }

    @Test
    public void should_do_nothing_when_skip() {
        CqlExecCassandraMojo cqlExecCassandraMojo = builder.skip().build();

        try {
            cqlExecCassandraMojo.execute();

            assertLog(INFO, "Skipping cassandra: cassandra.skip==true");
        } catch (MojoExecutionException | MojoFailureException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void should_fail_when_comparator_type_parser_class_does_not_exist() {
        CqlExecCassandraMojo cqlExecCassandraMojo = builder.comparator("NonExistingComparator").build();

        try {
            cqlExecCassandraMojo.execute();
        } catch (MojoExecutionException e) {
            assertEquals("Could not parse comparator value: NonExistingComparator", e.getMessage());
            assertEquals(ConfigurationException.class, e.getCause().getClass());
        } catch (MojoFailureException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void should_fail_when_comparator_type_parser_leads_to_syntax_error() {
        CqlExecCassandraMojo cqlExecCassandraMojo = builder.comparator("BytesType(`").build();

        try {
            cqlExecCassandraMojo.execute();
        } catch (MojoExecutionException e) {
            assertEquals("Could not parse comparator value: BytesType(`", e.getMessage());
            assertEquals(SyntaxException.class, e.getCause().getClass());
        } catch (MojoFailureException e) {
            fail();
        }
    }

    @Test
    public void should_give_comparator_error_message_when_key_validator_fails() { // TODO should be should_fail_when_key_validator_type_parser_class_does_not_exist
        CqlExecCassandraMojo cqlExecCassandraMojo = builder.keyValidator("NonExistingComparator").build();

        try {
            cqlExecCassandraMojo.execute();
        } catch (MojoExecutionException e) {
            assertEquals("Could not parse comparator value: BytesType", e.getMessage()); // TODO should be: assertEquals("Could not parse key validator value: NonExistingComparator", e.getMessage());
        } catch (MojoFailureException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void should_give_comparator_error_message_when_default_validator_fails() { // TODO should be should_fail_when_default_comparator_type_parser_leads_to_syntax_error
        CqlExecCassandraMojo cqlExecCassandraMojo = builder.defaultValidator("BytesType(`").build();

        try {
            cqlExecCassandraMojo.execute();
        } catch (MojoExecutionException e) {
            assertEquals("Could not parse comparator value: BytesType", e.getMessage()); // TODO should be: assertEquals("Could not parse default validator value: BytesType(`", e.getMessage());
        } catch (MojoFailureException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void should_do_nothing_with_empty_cql_statement() {
        CqlExecCassandraMojo cqlExecCassandraMojo = builder.cqlStatement("").build();

        try {
            cqlExecCassandraMojo.execute();

            assertLog(WARNING, "No CQL provided. Nothing to do.");
        } catch (MojoExecutionException | MojoFailureException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void should_do_nothing_when_no_cql_provided() {
        CqlExecCassandraMojo cqlExecCassandraMojo = builder.build();

        try {
            cqlExecCassandraMojo.execute();

            assertLog(WARNING, "No CQL provided. Nothing to do.");
        } catch (MojoExecutionException | MojoFailureException e) {
            fail(e.getMessage());
        }
    }

    private void assertLog(Level level, String expectedContent) {
        ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
        if (level == INFO) {
            verify(log).info(content.capture());
        } else if (level == WARNING) {
            verify(log).warn(content.capture());
        }
        assertEquals(expectedContent, content.getValue());
    }

}