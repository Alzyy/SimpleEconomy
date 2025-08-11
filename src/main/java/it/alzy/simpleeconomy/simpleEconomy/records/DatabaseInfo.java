package it.alzy.simpleeconomy.simpleEconomy.records;

public record DatabaseInfo(String host, String username, String password, int port, String database, int maxPoolSize, String tablePrefix) {

    public String JDBCString() {
        return "jdbc:mysql://" + host + ":" + port + "/" + database;
    }
    
}
