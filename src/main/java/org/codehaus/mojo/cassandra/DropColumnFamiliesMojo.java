package org.codehaus.mojo.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
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
    protected CqlOperation buildOperation() {
        DropTableOperation dropTableOperation = new DropTableOperation(rpcAddress, nativeTransportPort);
        dropTableOperation.setKeyspace(keyspace);
        return dropTableOperation;
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

    class DropTableOperation extends CqlOperation {

        public DropTableOperation(String rpcAddress, int nativeTransportPort) {
            super(rpcAddress, nativeTransportPort);
        }

        @Override
        public void executeOperation(CqlSession cqlSession) throws CqlExecutionException{
            try {
                if (columnFamilyList != null && columnFamilyList.length > 0) {
                    for (String s : columnFamilyList) {
                        dropTable(s).ifExists();
                        getLog().info("Dropped Table \"" + s + "\".");
                    }
                } else {
                    dropKeyspace(keyspace).ifExists();
                    getLog().info("Dropped keyspace \"" + keyspace + "\".");
                }
            } catch (Exception e) {
                throw new CqlExecutionException(e);
            }
        }
    }
}
