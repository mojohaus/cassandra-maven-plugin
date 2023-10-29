package org.codehaus.mojo.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import org.apache.cassandra.thrift.Cassandra.Client;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static com.datastax.oss.driver.api.querybuilder.SchemaBuilder.dropKeyspace;
import static com.datastax.oss.driver.api.querybuilder.SchemaBuilder.dropTable;

/**
 * Drop the specified ColumnFamilies or, if no arguments are given, 
 * the specified Keyspace 
 * 
 * @author zznate
 *
 */
@Mojo(name = "drop", threadSafe = true)
public class DropColumnFamiliesMojo extends AbstractSchemaCassandraMojo {

    /**
     * The one or more comma-delimited ColumnFamilies against to be dropped. 
     * If not specified, the Keyspace will be dropped.
     */
    @Parameter(property="cassandra.columnFamilies")
    protected String columnFamilies;


    @Override
    protected ThriftApiOperation buildOperation()
    {
        DropCfOperation dropCfOp = new DropCfOperation(rpcAddress, rpcPort);
        dropCfOp.setKeyspace(keyspace);
        return dropCfOp;
    }

    @Override
    protected CqlOperation cqlBuildOperation() {
        DropTableCqlOperation dropTableCqlOperation = new DropTableCqlOperation(rpcAddress, nativeTransportPort);
        dropTableCqlOperation.setKeyspace(keyspace);
        return dropTableCqlOperation;
    }

    private String[] columnFamilyList;

    protected void parseArguments() throws IllegalArgumentException
    {
        if (StringUtils.isNotBlank(keyspace)) 
        {
            // keyspace is a required parameter but somebody could provide -Dkeyspace=
            // which would cause issues
            throw new IllegalArgumentException("The keyspace to drop column families from cannot be empty");
        }
    
        columnFamilyList = StringUtils.split(columnFamilies, ',');
    }
        

    class DropCfOperation extends ThriftApiOperation 
    {

        public DropCfOperation(String rpcAddress, int rpcPort)
        {
            super(rpcAddress, rpcPort);
        }

        @Override
        public void executeOperation(Client client) throws ThriftApiExecutionException
        {
            try {
                if ( columnFamilyList != null && columnFamilyList.length > 0 ) 
                {
                    for (String s : columnFamilyList)
                    {
                        client.system_drop_column_family(s);
                        getLog().info("Dropped column family \"" + s + "\".");
                    }
                } 
                else 
                {
                    client.system_drop_keyspace(keyspace);
                    getLog().info("Dropped keyspace \"" + keyspace + "\".");
                }
            } catch (Exception e) 
            {
                throw new ThriftApiExecutionException(e);
            }
        }
    }

    class DropTableCqlOperation extends CqlOperation {

        public DropTableCqlOperation(String rpcAddress, int nativeTransportPort) {
            super(rpcAddress, nativeTransportPort);
        }

        @Override
        void executeOperation(CqlSession cqlSession) {
            if (columnFamilyList != null && columnFamilyList.length > 0) {
                for (String s : columnFamilyList) {
                    dropTable(s).ifExists();
                    getLog().info("Dropped Table \"" + s + "\".");
                }
            } else {
                dropKeyspace(keyspace).ifExists();
                getLog().info("Dropped keyspace \"" + keyspace + "\".");
            }
        }
    }
}
