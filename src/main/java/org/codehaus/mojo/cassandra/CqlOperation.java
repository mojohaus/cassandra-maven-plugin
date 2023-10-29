package org.codehaus.mojo.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverException;

public abstract class CqlOperation {

    private String keyspace;
    private final String rpcAddress;
    private final int nativeTransportPort;
    private String cqlVersion = "3.4.0";

    public CqlOperation(String rpcAddress, int nativeTransportPort) {
        this.rpcAddress = rpcAddress;
        this.nativeTransportPort = nativeTransportPort;
    }

    abstract void executeOperation(CqlSession cqlSession) throws DriverException;

    public String getKeyspace() {
        return keyspace;
    }

    public void setKeyspace(String keyspace) {
        this.keyspace = keyspace;
    }

    public String getRpcAddress() {
        return rpcAddress;
    }


    public int getNativeTransportPort() {
        return nativeTransportPort;
    }

    public String getCqlVersion() {
        return cqlVersion;
    }

    public void setCqlVersion(String cqlVersion) {
        this.cqlVersion = cqlVersion;
    }
}
