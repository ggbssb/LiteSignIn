package studio.trc.bukkit.litesignin.database.engine;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import studio.trc.bukkit.litesignin.Main;
import studio.trc.bukkit.litesignin.config.ConfigurationUtil;
import studio.trc.bukkit.litesignin.config.ConfigurationType;

public class MySQLEngine
{
    public static boolean SQLReloading = false;
    
    private static Connection connection = null;
    private static String hostname = "localhost";
    private static String port = "3306";
    private static String username = "root";
    private static String password = "password";
    private static String database = "signin";
    private static String table = "playerdata";
    private static String parameter = "?useUnicode=true&characterEncoding=utf8&useSSL=false";
    
    public static String getDatabase() {
        return database;
    } 
    
    public static String getTable() {
        return table;
    }
   
    public static void reloadConnectionParameters() {
        hostname = ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getString("MySQL-Storage.Hostname");
        port = ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getString("MySQL-Storage.Port");
        username = ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getString("MySQL-Storage.Username");
        password = ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getString("MySQL-Storage.Password");
        database = ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getString("MySQL-Storage.Database");
        parameter = ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getString("MySQL-Storage.Parameter");
        table = ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getString("MySQL-Storage.Table-Name");
        if (connection == null) {
            connectToDatabase();
        } else try {
            SQLReloading = true;
            if (Main.language.get("Reconnect") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("Reconnect").replace("{database}", "MySQL").replace("{prefix}", ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getString("Prefix")).replace("&", "§"));
            Thread closing = new Thread(() -> {
                try {
                    if (!connection.isClosed()) {
                        connection.close();
                    }
                } catch (SQLException ex) {}
            });
            closing.start();
            long time = System.currentTimeMillis();
            while (closing.isAlive()) {
                if (System.currentTimeMillis() - time > 10000) {
                    closing.stop();
                    break;
                }
                Thread.sleep(50);
            }
            SQLReloading = false;
            connectToDatabase();
        } catch (Exception ex) {}
    }
    
    public static void connectToDatabase() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + parameter, username, password);
            if (Main.language.get("SuccessfulConnection") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("SuccessfulConnection").replace("{database}", "MySQL").replace("{prefix}", ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getString("Prefix")).replace("&", "§"));
            connection.prepareStatement("CREATE DATABASE IF NOT EXISTS " + database + "; CREATE TABLE IF NOT EXISTS " + database + "." + table + "(UUID VARCHAR(36) NOT NULL, Name VARCHAR(16), Year INT, Month INT, Day INT, Hour INT, Minute INT, Second INT, Continuous INT, RetroactiveCard INT, History LONGTEXT, PRIMARY KEY (UUID))").executeUpdate();
        } catch (ClassNotFoundException ex) {
            if (Main.language.get("NoDriverFound") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("NoDriverFound").replace("{database}", "MySQL").replace("{prefix}", ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getString("Prefix")).replace("&", "§"));
            ConfigurationUtil.getConfig(ConfigurationType.CONFIG).set("MySQL-Storage.Enabled", false);
        } catch (SQLException ex) {
            if (Main.language.get("ConnectionError") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("ConnectionError").replace("{database}", "MySQL").replace("{prefix}", ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getString("Prefix")).replace("{error}", ex.getLocalizedMessage()).replace("&", "§"));
            ConfigurationUtil.getConfig(ConfigurationType.CONFIG).set("MySQL-Storage.Enabled", false);
        }
    }
    
    private static boolean databaseExist = false;
    
    private static boolean databaseExist() throws SQLException {
        if (databaseExist) return true;
        ResultSet rs = connection.prepareStatement("SHOW DATABASES LIKE '" + database + "'").executeQuery();
        databaseExist = rs.next();
        return databaseExist;
    }
  
    public static void repairConnection() {
        new Thread(() -> {
            int number = 0;
            while (true) {
                try {
                    connection = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + database + parameter, username, password);
                    if (Main.language.get("ConnectionRepair") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("ConnectionRepair").replace("{database}", "MySQL").replace("{prefix}", ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getString("Prefix")).replace("&", "§"));
                    break;
                } catch (SQLException ex) {
                    number++;
                    if (number == ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getInt("MySQL-Storage.Automatic-Repair")) {
                        if (Main.language.get("ConnectionRepairFailure") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("ConnectionRepairFailure").replace("{database}", "MySQL").replace("{prefix}", ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getString("Prefix")).replace("{number}", String.valueOf(number)).replace("&", "§"));
                    } else {
                        if (Main.language.get("BeyondRepair") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("BeyondRepair").replace("{database}", "MySQL").replace("{prefix}", ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getString("Prefix")).replace("&", "§"));
                        break;
                    }
                }
            }
        }, "MySQLRepairConnectionThread").start();
    }
  
    public static Connection getConnection() {
        return connection;
    }
  
    public static void executeUpdate(PreparedStatement statement) {
        while (SQLReloading) {} 
        try {
            while (!databaseExist()) {}
            statement.executeUpdate();
        }  catch (SQLException ex) {
            if (Main.language.get("DataSavingError") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("DataSavingError").replace("{database}", "MySQL").replace("{error}", ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : "null").replace("{prefix}", ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getString("Prefix")).replace("&", "§"));
            try {
                if (getConnection().isClosed()) repairConnection();
            } catch (SQLException ex1) {}
        }
    }
  
    public static ResultSet executeQuery(PreparedStatement statement) {
        while (SQLReloading) {} 
        try {
            while (!databaseExist()) {}
            return statement.executeQuery();
        } catch (SQLException ex) {
            if (Main.language.get("DataReadingError") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("DataReadingError").replace("{database}", "MySQL").replace("{error}", ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : "null").replace("{prefix}", ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getString("Prefix")).replace("&", "§"));
            try {
                if (getConnection().isClosed()) repairConnection();
            } catch (SQLException ex1) {}
        }
        return null;
    }
  
    @Deprecated
    public static void executeUpdate(String sql) {
        while (SQLReloading) {} 
        try {
            while (!databaseExist()) {}
            connection.createStatement().executeUpdate(sql);
        } catch (SQLException ex) {
            if (Main.language.get("DataSavingError") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("DataSavingError").replace("{database}", "MySQL").replace("{error}", ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : "null").replace("{prefix}", ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getString("Prefix")).replace("&", "§"));
            try {
                if (getConnection().isClosed()) repairConnection();
            } catch (SQLException ex1) {}
        }
    }
  
    @Deprecated
    public static ResultSet executeQuery(String sql) {
        while (SQLReloading) {} 
        try {
            while (!databaseExist()) {}
            return connection.createStatement().executeQuery(sql);
        } catch (SQLException ex) {
            if (Main.language.get("DataReadingError") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("DataReadingError").replace("{database}", "MySQL").replace("{error}", ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : "null").replace("{prefix}", ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getString("Prefix")).replace("&", "§"));
            try {
                if (getConnection().isClosed()) repairConnection();
            } catch (SQLException ex1) {}
        }
        return null;
    }
    
    /**
     * Back up all player data.
     * @param filePath Backup file path. 
     * @throws java.sql.SQLException 
     */
    public static void backup(String filePath) throws SQLException {
        try (Connection sqlConnection = DriverManager.getConnection("jdbc:sqlite:" + filePath)) {
            sqlConnection.prepareStatement("CREATE TABLE IF NOT EXISTS PlayerData("
                    + "UUID VARCHAR(36) NOT NULL,"
                    + " Name VARCHAR(16),"
                    + " Year INT,"
                    + " Month INT,"
                    + " Day INT,"
                    + " Hour INT,"
                    + " Minute INT,"
                    + " Second INT,"
                    + " Continuous INT,"
                    + " RetroactiveCard INT,"
                    + " History LONGTEXT,"
                    + " PRIMARY KEY (UUID))").executeUpdate();
            ResultSet rs = executeQuery(connection.prepareStatement("SELECT * FROM " + database + "." + table));
            while (rs.next()) {
                String uuid = rs.getString("UUID");
                String name = rs.getString("Name");
                int year = rs.getInt("Year");
                int month = rs.getInt("Month");
                int day = rs.getInt("Day");
                int hour = rs.getInt("Hour");
                int minute = rs.getInt("Minute");
                int second = rs.getInt("Second");
                int continuous = rs.getInt("Continuous");
                int retroactivecard = rs.getInt("RetroactiveCard");
                String history = rs.getString("History");
                if (name == null) {
                    name = "null";
                }
                if (history == null) {
                    history = "";
                }
                PreparedStatement statement = sqlConnection.prepareStatement("INSERT INTO PlayerData(UUID, Name, Year, Month, Day, Hour, Minute, Second, Continuous, RetroactiveCard, History)"
                        + " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                statement.setString(1, uuid);
                statement.setString(2, name);
                statement.setInt(3, year);
                statement.setInt(4, month);
                statement.setInt(5, day);
                statement.setInt(6, hour);
                statement.setInt(7, minute);
                statement.setInt(8, second);
                statement.setInt(9, continuous);
                statement.setInt(10, retroactivecard);
                statement.setString(11, history);
                statement.executeUpdate();
            }
        }
    }
}