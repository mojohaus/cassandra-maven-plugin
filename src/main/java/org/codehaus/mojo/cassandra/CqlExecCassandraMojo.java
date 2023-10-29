package org.codehaus.mojo.cassandra;

import java.io.File;
import java.util.List;

import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.Row;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.TypeParser;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.exceptions.SyntaxException;
import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.CqlResult;
import org.apache.cassandra.thrift.CqlRow;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Executes cql statements from maven.
 *
 * @author zznate
 *
 */
@Mojo(name = "cql-exec", threadSafe = true, defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class CqlExecCassandraMojo extends AbstractCqlExecMojo {

  /**
   * The CQL script which will be executed
   */
  @Parameter(property="cassandra.cql.script", defaultValue="${basedir}/src/cassandra/cql/exec.cql")
  protected File cqlScript;

  /**
   * The CQL statement to execute singularly
   *
   */
  @Parameter(property="cql.statement")
  protected String cqlStatement;

  /**
   * Expected type of the column value
   */
  @Parameter(property="cql.defaultValidator")
  protected String defaultValidator = "BytesType";

  /**
   * Expected type of the key
   *
   */
  @Parameter(property="cql.keyValidator")
  protected String keyValidator = "BytesType";

  /**
   * Expected type of the column name
   *
   */
  @Parameter(property="cql.comparator")
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

      } catch (ConfigurationException | SyntaxException e)
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
  private void printResults(List<Row> results) {
      // TODO fix ghetto formatting
      getLog().info("-----------------------------------------------");
      for (Row result : results) {
          printRow(result);
      }
  }

  private void printRow(Row row) {
      getLog().info("-----------------------------------------------");
      getLog().info(row.getFormattedContents());
      getLog().info("-----------------------------------------------");
  }
}
