package org.codehaus.mojo.cassandra;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.cassandra.thrift.SchemaDisagreementException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

/**
 * Some general operations and contract for Mojo implementations that will interact
 * with the system_* API methods. Manages the Thrift connection and delegates to 
 * executeOperation on the implementation. Implementations must still implement
 * the execute method of the parent Mojo.
 * 
 * @author zznate
 */
public abstract class AbstractSchemaCassandraMojo extends AbstractCassandraMojo {
               
    
    protected abstract void parseArguments() throws IllegalArgumentException;

    protected abstract ThriftApiOperation buildOperation();

    protected abstract CqlOperation cqlBuildOperation();
    
    /**
     * Parses the arguments then calls 
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException 
    {
        try 
        {
            parseArguments();
        } catch (IllegalArgumentException iae) 
        {
            throw new MojoExecutionException(iae.getMessage());
        }
        Utils.executeCql(cqlBuildOperation());
    }
}
