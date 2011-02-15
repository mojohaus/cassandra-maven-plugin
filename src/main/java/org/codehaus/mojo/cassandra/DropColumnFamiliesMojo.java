package org.codehaus.mojo.cassandra;

import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.cassandra.thrift.Cassandra.Client;
import org.apache.commons.lang.StringUtils;
import org.apache.thrift.TException;

/**
 * Drop the specified ColumnFamilies or, if no arguments are given, 
 * the specified Keyspace 
 * 
 * @author zznate
 * @threadSafe
 * @goal drop
 */
public class DropColumnFamiliesMojo extends AbstractSchemaCassandraMojo {

    /**
     * The one or more comma-delimited ColumnFamilies against to be dropped. 
     * If not specified, the Keyspace will be dropped.
     * @parameter 
     */
    protected String columnFamilies;
    private String[] columnFamilyList;
    

    protected void parseArguments() throws IllegalArgumentException
    {
        if (StringUtils.isNotBlank(keyspace)) 
            throw new IllegalArgumentException("You must provide a value for Keyspace");
    
        columnFamilyList = StringUtils.split(columnFamilies, ',');
    }

    @Override
    protected void executeOperation(Client client) throws InvalidRequestException, TException 
    {
        if ( columnFamilyList != null && columnFamilyList.length > 0 ) 
        {
            for (int i = 0; i < columnFamilyList.length; i++) 
            {
                client.system_drop_column_family(columnFamilyList[i]);
            }
        } 
        else 
        {
            client.system_drop_keyspace(keyspace);
        }
        
    }
    
    

}
