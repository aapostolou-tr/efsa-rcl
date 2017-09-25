package table_database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import app_config.AppPaths;

/**
 * Start the database if present, otherwise create it.
 * @author avonva
 *
 */
public class Database {

	private static final String DB_URL = "jdbc:derby:" + AppPaths.DB_FOLDER;
	private static final String CLOSE_DB_URL = DB_URL + ";shutdown=true";
	
	/**
	 * Connect to the main catalogues database if present, otherwise create it and then connect
	 * @param DBString
	 * @throws IOException 
	 * @throws Exception
	 */
	public void connect() throws IOException {

		try {

			// load the jdbc driver
			System.out.println( "Starting embedded database...");
			
			Class.forName("org.apache.derby.jdbc.EmbeddedDriver");

			// check if the database is present or not
			System.out.println("Testing database connection...");

			Connection con = getConnection();
			con.close();

		} catch (ClassNotFoundException e) {
			
			e.printStackTrace();
			System.err.println ("Cannot start embedded database: embedded driver missing");

		} catch (SQLException e1) {

			System.out.println( "Creating database...");
			
			DatabaseCreator creator = new DatabaseCreator(AppPaths.TABLES_SCHEMA_FILE);
			creator.create(AppPaths.DB_FOLDER);
		}
	}
	
	/**
	 * Shutdown the database
	 * @throws SQLException
	 */
	public void shutdown() {
		try {
			DriverManager.getConnection(CLOSE_DB_URL);
		} catch (SQLException e) {
		}
	}
	
	/**
	 * Get the connection with the database
	 * @return
	 * @throws SQLException
	 */
	public static Connection getConnection() throws SQLException {
		Connection con = DriverManager.getConnection(DB_URL);
		return con;
	}
}
