package org.shirdrn.common;

public class DBConfig {

	private String host;
	private int port;
	private String dbname;
	private String collectionName;
	public DBConfig() {
		
	}
	public DBConfig(String host, int port, String dbname,
			String collectionName) {
		super();
		this.host = host;
		this.port = port;
		this.dbname = dbname;
		this.collectionName = collectionName;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getDbname() {
		return dbname;
	}
	public void setDbname(String dbname) {
		this.dbname = dbname;
	}
	public String getCollectionName() {
		return collectionName;
	}
	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}
}
