package org.codehaus.mojo.cassandra;

import com.datastax.oss.driver.api.core.AllNodesFailedException;
import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.DriverExecutionException;
import com.datastax.oss.driver.api.core.DriverTimeoutException;
import com.datastax.oss.driver.api.core.InvalidKeyspaceException;
import com.datastax.oss.driver.api.core.servererrors.CoordinatorException;

/**
 * Exception to wrap the various Exceptions that can come back
 * from Apache Cassandra's Cql API
 *
 * @author zznate
 */
public class CqlExecutionException extends RuntimeException {

    private static final long serialVersionUID = 5653554393371671913L;

    private static final String ERR_MSG = "There was a problem calling Apache Cassandra's Cql API. ";

    public CqlExecutionException() {
        super(ERR_MSG);
    }

    public CqlExecutionException(String msg) {
        super(ERR_MSG + msg);
    }

    public CqlExecutionException(Throwable t) {
        super(ERR_MSG + deduceExceptionMessage(t), t);
    }

    private static String deduceExceptionMessage(Throwable t) {
        StringBuilder msg = new StringBuilder("Details: ");
        if (t instanceof AllNodesFailedException)
            msg.append("AllNodesFailedException, the query failed on all the coordinators it was tried on. ")
                    .append(((AllNodesFailedException) t).getAllErrors());
        else if (t instanceof CoordinatorException)
            msg.append(
                            "CoordinatorException, got a server-side error thrown by the coordinator node in response to the query. ")
                    .append(t.getCause());
        else if (t instanceof DriverExecutionException)
            msg.append("DriverExecutionException, Query failed due to an underlying checked Exception");
        else if (t instanceof DriverTimeoutException) msg.append("DriverTimeoutException, Query timed out");
        else if (t instanceof InvalidKeyspaceException)
            msg.append("InvalidKeyspaceException, Provided Keyspace is invalid");
        else if (t instanceof DriverException)
            msg.append("Datastax Driver Exception: ").append(t.getCause());
        else msg.append("n/a");
        return msg.toString();
    }
}
