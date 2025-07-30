package it.alzy.simpleeconomy.records;

public record DatabaseInfo(String host, String username, String password, int port, String database, int maxPoolSize) {

    public String JDBCString() {
        return "jdbc:mysql://" + host + ":" + port + "/" + database;
    }
    
}
