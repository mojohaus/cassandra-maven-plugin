package org.codehaus.mojo.cassandra;

import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.cassandra.thrift.SchemaDisagreementException;
import org.apache.cassandra.thrift.TimedOutException;
import org.apache.cassandra.thrift.UnavailableException;
import org.apache.thrift.TException;

/**
 * Exception to wrap the various Exceptions that can come back 
 * from Apache Cassandra's Trift API
 * 
 * @author zznate
 */
public class ThriftApiExecutionException extends RuntimeException
{

    private static final long serialVersionUID = 5653554393371671913L;

    private static final String ERR_MSG = "There was a problem calling Apache Cassandra's Thrift API. ";

    public ThriftApiExecutionException()
    {
        super(ERR_MSG);
    }
    
    public ThriftApiExecutionException(String msg)
    {
        super(ERR_MSG + msg);
    }
    
    public ThriftApiExecutionException(Throwable t)
    {
        super(ERR_MSG + deduceExceptionMessage(t), t);
    }
    
    
    private static String deduceExceptionMessage(Throwable t)
    {
        String msg = "Details: ";
        if ( t instanceof UnavailableException )
            msg.concat("You do not have enough nodes up to handle the specified consistency level");
        else if ( t instanceof TimedOutException )
            msg.concat("Request timed out - server load may be too high, or you may be requesting too many rows for a single operation");
        else if ( t instanceof InvalidRequestException )
            msg.concat("The request was not properly formatted " + t.getMessage());
        else if ( t instanceof SchemaDisagreementException)
            msg.concat("Schema versions are out of sync");
        else if ( t instanceof TException )
            msg.concat("General Thrift Exception, ensure Apache Cassandra is running and all necessary ports are accessible");
        else
            msg.concat("n/a");
        return msg;
    }
}
