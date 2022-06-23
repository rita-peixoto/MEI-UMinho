package DAO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class BaseDados {
    private static Connection connection;
    private static String driver = "com.mysql.cj.jdbc.Driver";
    private static String url = "jdbc:mysql://rasbet.ccf0uk6x1mfy.us-east-1.rds.amazonaws.com:1234/RASBET";
    private static String user = "admin";
    private static String password = "?ras2021";

    static {
        BaseDados.startConnection();
        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private BaseDados() {
    }

    public static void startConnection() {
        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(url + "?user=" + user + "&password=" + password);
            connection.setAutoCommit(false);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() {
        try {
            if (connection.isClosed()) {
                BaseDados.startConnection();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public static void stopConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
