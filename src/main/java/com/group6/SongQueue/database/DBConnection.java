package com.group6.SongQueue.database;

import org.sqlite.JDBC;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.springframework.stereotype.Repository;
import jakarta.annotation.PreDestroy;

@Repository
public class DBConnection {
	private Connection connection;
	protected Connection getConnection() {
		if (connection == null) {
			throw new RuntimeException("Connection not opened");
		}
		return connection;
	}

	/**
	 * @brief Create connection to SQLite database
	 */
	public DBConnection() {
		System.out.println("Creating SQLite connection");
   		try {
			connection = JDBC.createConnection("jdbc:sqlite:database.db", new Properties());
			Setup();
	 	} catch(SQLException e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * @brief Setup database tables
	 */
	private void Setup() {
		//TODO: Create database setup scripts/system
		//Example:
 		try {
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(10);  // set timeout to 10 sec.

			statement.executeUpdate("drop table if exists person");
			statement.executeUpdate("create table person (id integer, name string)");
			statement.executeUpdate("insert into person values(1, 'leo')");
			statement.executeUpdate("insert into person values(2, 'yui')");
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * @brief Close connection to SQLite database
	 */
	@PreDestroy
	public void Close() {
		System.out.println("Closing SQLite connection");
		try {
			if(connection != null)
				connection.close();
			}
		catch(SQLException e)
		{
			// connection close failed.
			System.err.println(e);
		}
	}

}