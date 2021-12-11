package org.codehaus.mojo.cassandra;

import org.apache.cassandra.exceptions.UnavailableException;
import org.apache.cassandra.service.StorageProxy;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Executes the truncate command against the specified keyspace. 
 * Currently doing so via StorageProxy as a discussion point. 
 *
 * @author zznate
 * @non-goal truncate
 * @threadSafe
 */
public class TruncateCassandraMojo extends AbstractCassandraMojo 
{

    /**
     * @parameter property="cassandra.keyspace"
     * @required 
     */
    protected String keyspace;
    
    /**
     * @parameter property="cassandra.columnFamily"
     * @required 
     */
    protected String columnFamily;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException 
    {
        try 
        {      
            createCassandraHome();
            getLog().debug("Truncating Column Family \"" + columnFamily + "\" in Keyspace \"" + keyspace + "\"...");
            StorageProxy.truncateBlocking(keyspace, columnFamily);
            getLog().info("Truncated Column Family \"" + columnFamily + "\" in Keyspace \"" + keyspace + "\"...");
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
