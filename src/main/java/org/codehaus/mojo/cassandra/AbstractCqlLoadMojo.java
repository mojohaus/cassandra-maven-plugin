package org.codehaus.mojo.cassandra;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Abstract parent class for mojos that load and execute CQL statements from a file.
 *
 * @author sparhomenko
 */
public abstract class AbstractCqlLoadMojo extends AbstractCqlExecMojo
{
    /**
     * The CQL file to load.
     *
     * @parameter default-value="${basedir}/src/cassandra/cql/load.cql"
     */
    private File script;

    /**
     * Whether to ignore errors when loading the script.
     *
     * @parameter property="cassandra.load.failure.ignore"
     */
    private boolean loadFailureIgnore;

    protected void execCqlFile() throws MojoExecutionException
    {
        if (script != null)
        {
            getLog().info("Running " + script + "...");
            try
            {
                executeCql(readFile(script));
                getLog().info("Finished " + script + ".");
            } catch (MojoExecutionException e)
            {
                if (loadFailureIgnore)
                {
                    getLog().error("Script execution failed with " + e.getMessage() + ". Ignoring.");
                } else
                {
                    throw e;
                }
            }
        }
    }
}
