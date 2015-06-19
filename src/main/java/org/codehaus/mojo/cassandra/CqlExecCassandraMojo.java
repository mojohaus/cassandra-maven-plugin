package org.codehaus.mojo.cassandra;

import java.io.File;
import java.util.List;

import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.TypeParser;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.exceptions.SyntaxException;
import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.CqlResult;
import org.apache.cassandra.thrift.CqlRow;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Executes cql statements from maven.
 *
 * @author zznate
 * @goal cql-exec
 * @threadSafe
 * @phase pre-integration-test
 */
public class CqlExecCassandraMojo extends AbstractCqlExecMojo {

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

  /**
   * Expected type of the column value
   * @parameter expression="${cql.defaultValidator}"
   */
  protected String defaultValidator = "BytesType";

  /**
   * Expected type of the key
   * @parameter expression="${cql.keyValidator}"
   */
  protected String keyValidator = "BytesType";

  /**
   * Expected type of the column name
   * @parameter expression="${cql.comparator}"
   */
  protected String comparator = "BytesType";

  private AbstractType<?> comparatorVal;
  private AbstractType<?> keyValidatorVal;
  private AbstractType<?> defaultValidatorVal;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
      if (skip)
      {
          getLog().info("Skipping cassandra: cassandra.skip==true");
          return;
      }
      try
      {
          comparatorVal = TypeParser.parse(comparator);
          keyValidatorVal = TypeParser.parse(keyValidator);
          defaultValidatorVal = TypeParser.parse(defaultValidator);

      } catch (ConfigurationException e)
      {
          throw new MojoExecutionException("Could not parse comparator value: " + comparator, e);
      } catch (SyntaxException e)
      {
        throw new MojoExecutionException("Could not parse comparator value: " + comparator, e);
      }
      if (cqlScript != null && cqlScript.isFile())
      {
          cqlStatement = readFile(cqlScript);
      }

      printResults(executeCql(cqlStatement));
  }

  /*
   * Encapsulate print of CqlResult. Uses specified configuration options to format results
   */
  private void printResults(List<CqlResult> results)
  {
      // TODO fix ghetto formatting
      getLog().info("-----------------------------------------------");
      for (CqlResult result : results)
      {
          for (CqlRow row : result.getRows())
          {
              getLog().info("Row key: "+keyValidatorVal.getString(row.key));
              getLog().info("-----------------------------------------------");
              for (Column column : row.getColumns() )
              {
                  getLog().info(" name: "+comparatorVal.getString(column.name));
                  getLog().info(" value: "+defaultValidatorVal.getString(column.value));
                  getLog().info("-----------------------------------------------");
              }

          }
      }
  }

}
