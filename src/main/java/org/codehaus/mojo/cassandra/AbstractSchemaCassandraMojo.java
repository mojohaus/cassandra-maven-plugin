package org.codehaus.mojo.cassandra;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.InvalidRequestException;
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
    
    /**
     * The keyspace against which the system_* operation will be executed
     * @parameter 
     * @required
     */
    protected String keyspace;    
    
    protected abstract void executeOperation(Cassandra.Client client) throws InvalidRequestException, TException;
    
    protected abstract void parseArguments() throws IllegalArgumentException;
    
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
        executeThrift();
    }
    
    /**
     * Call {@link #executeOperation(Cassandra.Client)} on the implementing class
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    protected void executeThrift() throws MojoExecutionException, MojoFailureException 
    {
        TSocket socket = new TSocket(rpcAddress, rpcPort);
        TTransport transport = new TFramedTransport(socket);
        
        TBinaryProtocol binaryProtocol = new TBinaryProtocol(transport, true, true);
        Cassandra.Client cassandraClient = new Cassandra.Client(binaryProtocol);
        
        try 
        {
            transport.open();
            cassandraClient.set_keyspace(keyspace);
            executeOperation(cassandraClient);            
        } catch (TTransportException tte) 
        {
            throw new MojoExecutionException("There was a problemn opening the connection to Apache Cassandra", tte);
        } catch (InvalidRequestException ire) 
        {
            throw new MojoExecutionException("Invalid request returned. Does the Keyspace '" + keyspace + "' exist?", ire);
        } catch (Exception e) 
        {
            // anything paste IRE will not be terribly meaningful for truncate
            throw new MojoExecutionException("General exception executing truncate", e);
        } finally 
        {
            if ( transport != null && transport.isOpen() )
            {
                try 
                {
                    transport.flush();
                    transport.close();
                } catch (Exception e) 
                { 
                    throw new MojoExecutionException("Something went wrong cleaning up", e);
                }
            }
        }
    }

}
