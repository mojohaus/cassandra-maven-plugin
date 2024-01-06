package org.codehaus.mojo.cassandra;

import java.util.List;
import java.util.Map;

import com.datastax.oss.driver.api.core.AllNodesFailedException;
import com.datastax.oss.driver.api.core.DriverExecutionException;
import com.datastax.oss.driver.api.core.DriverTimeoutException;
import com.datastax.oss.driver.api.core.InvalidKeyspaceException;
import com.datastax.oss.driver.api.core.UnsupportedProtocolVersionException;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.servererrors.ServerError;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CqlExecutionExceptionTest {
    @Test
    public void should_not_give_any_details_if_there_is_no_cause() {
        assertThat(new CqlExecutionException())
                .hasMessage("There was a problem calling Apache Cassandra's Cql API. ")
                .hasCause(null);
    }

    @Test
    public void should_not_give_some_details_if_there_is_a_message() {
        CqlExecutionException exception = new CqlExecutionException("additional message");

        assertThat(exception)
                .hasMessage("There was a problem calling Apache Cassandra's Cql API. additional message")
                .hasCause(null);
    }

    @Test
    public void should_not_give_any_details_if_cause_is_not_handled() {
        Throwable cause = new Exception();

        CqlExecutionException exception = new CqlExecutionException(cause);

        assertThat(exception)
                .hasMessage("There was a problem calling Apache Cassandra's Cql API. Details: n/a")
                .hasCause(cause);
    }

    @Test
    public void should_give_details_if_cause_is_all_nodes_failed_exception() {
        Throwable cause = AllNodesFailedException.fromErrors((List<Map.Entry<Node, Throwable>>) null);

        CqlExecutionException exception = new CqlExecutionException(cause);

        assertThat(exception)
                .hasMessage(
                        "There was a problem calling Apache Cassandra's Cql API. Details: AllNodesFailedException, the query failed on all the coordinators it was tried on. {}")
                .hasCause(cause);
    }

    @Test
    public void should_give_details_if_cause_is_coordinator_exception() {
        Throwable cause = new ServerError(null, "Server Error");
        CqlExecutionException exception = new CqlExecutionException(cause);
        assertThat(exception)
                .hasMessage(
                        "There was a problem calling Apache Cassandra's Cql API. Details: CoordinatorException, got a server-side error thrown by the coordinator node in response to the query. null")
                .hasCause(cause);
    }

    @Test
    public void should_give_details_if_cause_is_driver_execution_exception() {
        Throwable cause = new DriverExecutionException(null);

        CqlExecutionException exception = new CqlExecutionException(cause);
        assertThat(exception)
                .hasMessage(
                        "There was a problem calling Apache Cassandra's Cql API. Details: DriverExecutionException, Query failed due to an underlying checked Exception")
                .hasCause(cause);
    }

    @Test
    public void should_give_details_if_cause_is_driver_timeout_exception() {
        Throwable cause = new DriverTimeoutException("Time out");

        CqlExecutionException exception = new CqlExecutionException(cause);
        assertThat(exception)
                .hasMessage(
                        "There was a problem calling Apache Cassandra's Cql API. Details: DriverTimeoutException, Query timed out")
                .hasCause(cause);
    }

    @Test
    public void should_give_details_if_cause_is_invalid_keyspace_exception() {
        Throwable cause = new InvalidKeyspaceException("Invalid Keyspace");

        CqlExecutionException exception = new CqlExecutionException(cause);
        assertThat(exception)
                .hasMessage(
                        "There was a problem calling Apache Cassandra's Cql API. Details: InvalidKeyspaceException, Provided Keyspace is invalid")
                .hasCause(cause);
    }

    @Test
    public void should_give_details_if_cause_is_datastax_driver_exception() {
        Throwable cause = new UnsupportedProtocolVersionException(null, null, null);

        CqlExecutionException exception = new CqlExecutionException(cause);
        assertThat(exception)
                .hasMessage(
                        "There was a problem calling Apache Cassandra's Cql API. Details: Datastax Driver Exception: null")
                .hasCause(cause);
    }
}
