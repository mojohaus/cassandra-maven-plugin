package org.codehaus.mojo.cassandra;

import org.apache.cassandra.exceptions.UnavailableException;
import org.apache.cassandra.service.StorageProxy;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Executes the truncate command against the specified keyspace. 
 * Currently doing so via StorageProxy as a discussion point. 
 *
 * @author zznate
 */
@Mojo(name = "truncate", threadSafe = true)
public class TruncateCassandraMojo extends AbstractCassandraMojo 
{

    @Parameter(property = "cassandra.keyspace", required = true)
    protected String keyspace;
    
    @Parameter(property="cassandra.columnFamily", required = true)
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
