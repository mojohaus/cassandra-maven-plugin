package org.codehaus.mojo.cassandra;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.apache.cassandra.service.StorageProxy;
import org.apache.cassandra.thrift.UnavailableException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Executes the truncate command against the specified keyspace. 
 * Currently doing so via StorageProxy as a discussion point. 
 *
 * @author zznate
 * @goal truncate
 * @threadSafe
 */
public class TruncateCassandraMojo extends AbstractCassandraMojo 
{

    /**
     * @parameter expression="${cassandra.keyspace}"
     * @required 
     */
    protected String keyspace;
    
    /**
     * @parameter expression="${cassandra.columnFamily}"
     * @required 
     */
    protected String columnFamily;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException 
    {
        try 
        {      
            createCassandraHome();
            StorageProxy.truncateBlocking(keyspace, columnFamily);
        } catch (UnavailableException ue) 
        {
            throw new MojoExecutionException("Host(s) must be up in order for a truncate operation to be successful.", ue);
        } catch (TimeoutException te) 
        {
            throw new MojoExecutionException("Host did not reply for truncate operation.", te);
        } catch (IOException ioe) 
        {
            // unlikely in our case
            throw new MojoExecutionException("Could not construct truncate message",ioe);
        }
    }
    


}
