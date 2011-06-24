package org.codehaus.mojo.cassandra;

import java.io.File;

import org.apache.cassandra.thrift.Compression;
import org.apache.cassandra.thrift.CqlResult;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.cassandra.thrift.SchemaDisagreementException;
import org.apache.cassandra.thrift.TimedOutException;
import org.apache.cassandra.thrift.UnavailableException;
import org.apache.cassandra.thrift.Cassandra.Client;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.thrift.TException;
import org.codehaus.plexus.util.StringUtils;

/**
 * Loads a {@code cassandra-cli} bscript into a Cassandra instance.
 *
 * @author zznate
 * @goal cql-exec
 * @threadSafe
 * @phase pre-integration-test
 */
public class CqlExecCassandraMojo extends AbstractCassandraMojo {

  /**
   * The CQL script which will be executed
   *
   * @parameter expression="${cassandra.cql.script}" default-value="${basedir}/src/cassandra/cql/exec.cql"
   */
  protected File cqlScript;
  
  /**
   * The CQL statement to execute singularly
   * 
   * @parameter expression="${cql.statement}"
   */
  protected String cqlStatement;
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skip)
    {
        getLog().info("Skipping cassandra: cassandra.skip==true");
        return;
    }
    
    // TODO accept keyFormat, columnFormat, valueFormat
    
    // TODO file vs. statement switch 
    
    // TODO validated CQL file location
    
    // load CQL file 
    
    CqlExecOperation cqlOp = new CqlExecOperation(rpcAddress, rpcPort);
    if ( StringUtils.isNotBlank(keyspace)) 
    {
        getLog().error("setting keyspace: " + keyspace);
        cqlOp.setKeyspace(keyspace);
    }
    try 
    {
        Utils.executeThrift(cqlOp);
    } catch (ThriftApiExecutionException taee) 
    {
        throw new MojoExecutionException(taee.getMessage(), taee);
    }
    // TODO iterate results applying formats from user
    getLog().info(cqlOp.result.toString());

  }
  
  class CqlExecOperation extends ThriftApiOperation {

      CqlResult result;

      public CqlExecOperation(String rpcAddress, int rpcPort)
      {
          super(rpcAddress, rpcPort);
      }

      @Override
      void executeOperation(Client client) throws ThriftApiExecutionException
      {
          try 
          {
              result = client.execute_cql_query(ByteBufferUtil.bytes(cqlStatement), Compression.NONE);        
          } catch (Exception e) 
          {
              throw new ThriftApiExecutionException(e);
          }

      }

  }

}
