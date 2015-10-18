package com.asu.score.hackslash.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
//import java.sql.Date;
import java.util.Date;
import java.util.Calendar;

import com.asu.score.hackslash.engine.Database;

public class AllocationDAO {
	static Connection con = null;
	
	public static void setAllocation(Connection con, String TaskId, String UserId, java.sql.Timestamp StartDate) throws SQLException
	{
		Statement stmt = null;
		String query = "Insert into Allocation(TaskID, UserID, StartDate) values (\"" + TaskId + "\",\"" + UserId + "\",'" + StartDate + "')";
		System.out.println(query);
		try
		{
			stmt = con.createStatement();
			stmt.executeUpdate(query);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		finally 
		{
	        if (stmt != null) 
	        { 
	        	stmt.close(); 
	        }
	    }
	}
	
	public static void getAllocation(Connection con) throws SQLException
	{
		Statement stmt = null;
		String query = "Select TaskAllocationID, TaskID, UserID from Allocation";
		try
		{
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next())
			{
				System.out.println(rs.getString("TaskAllocationID"));
				System.out.println(rs.getString("TaskID"));
				System.out.println(rs.getString("UserID"));
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		finally 
		{
	        if (stmt != null) 
	        { 
	        	stmt.close(); 
	        }
	    }
	}
	public static void main(String[] args){
		Database db = new Database();
		AllocationDAO t = new AllocationDAO();
		try {
			Calendar calendar = Calendar.getInstance();
		    java.sql.Timestamp startDate = new java.sql.Timestamp(calendar.getTime().getTime());
			t.setAllocation(db.getConnection(),"2", "USER 2", startDate);
			t.getAllocation(db.getConnection());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
