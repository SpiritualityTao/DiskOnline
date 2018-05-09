package com.pt.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.pt.utils.MyDBHelper;

public class UserDao {
	
	public static boolean isLoginSuccess(String user,String password,int level){
		boolean success = false;
		String sql = "select * from tb_user where username=? and password=? and level=?";
		Connection conn = null;
		PreparedStatement pmt = null;
		ResultSet rs =null;
		try {
			conn = MyDBHelper.getConnection();
			pmt = conn.prepareStatement(sql);
			pmt.setString(1, user);
			pmt.setString(2, password);
			pmt.setInt(3, level);
			rs = pmt.executeQuery();
			while(rs.next()){
				success = true;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return success;
	}
}
