package com.pt.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MyDBHelper {
	/**
	 * 获取数据库链接对象
	 * @return
	 */
public static Connection getConnection() throws SQLException{
		
		Connection conn=null;
		
		String url="jdbc:mysql://localhost:3306/onlinedisk";
		
		String username="root";
		
		String password="root";
		
			try {
				Class.forName("com.mysql.jdbc.Driver");
				
				conn=DriverManager.getConnection(url,username,password);
				
			} catch (ClassNotFoundException e) {
				
				e.printStackTrace();
				
			}	
		return conn;
	}
	
	/**
	 * 关闭资源
	 * 
	 * @param conn
	 * @param st
	 * @param rs
	 */
	public static void close(Connection conn, PreparedStatement pmt, ResultSet rs) {

		try {
			if (rs != null) {
				rs.close();
			}
			if (pmt != null) {
				pmt.close();
			}
			if (conn != null) {
				conn.close();
			}
		} catch (SQLException e) {

			e.printStackTrace();
		}
	}
}
