package org.codehaus.mojo.cassandra;

import java.io.File;

import org.apache.maven.plugin.logging.Log;

class CqlExecCassandraMojoBuilder {
    private final CqlExecCassandraMojo cqlExecCassandraMojo;

    private CqlExecCassandraMojoBuilder() {
        this.cqlExecCassandraMojo = new CqlExecCassandraMojo();
    }

    CqlExecCassandraMojoBuilder(Log log) {
        this();
        this.cqlExecCassandraMojo.setLog(log);
    }

    CqlExecCassandraMojoBuilder skip() {
        cqlExecCassandraMojo.skip = true;
        return this;
    }

    CqlExecCassandraMojoBuilder comparator(String comparator) {
        cqlExecCassandraMojo.comparator = comparator;
        return this;
    }

    CqlExecCassandraMojoBuilder keyValidator(String keyValidator) {
        cqlExecCassandraMojo.keyValidator = keyValidator;
        return this;
    }

    CqlExecCassandraMojoBuilder defaultValidator(String defaultValidator) {
        cqlExecCassandraMojo.defaultValidator = defaultValidator;
        return this;
    }

    CqlExecCassandraMojoBuilder cqlScript(File cqlScript) {
        cqlExecCassandraMojo.cqlScript = cqlScript;
        return this;
    }

    CqlExecCassandraMojoBuilder cqlStatement(String cqlStatement) {
        cqlExecCassandraMojo.cqlStatement = cqlStatement;
        return this;
    }

    CqlExecCassandraMojoBuilder keyspace(String keyspace) {
        cqlExecCassandraMojo.keyspace = keyspace;
        return this;
    }

    CqlExecCassandraMojo build() {
        return cqlExecCassandraMojo;
    }
}
