package com.xerox.socialmedia.communityrecommendation.dbmanager;


import java.io.FileInputStream;
import java.sql.*;
import java.util.Properties;

public class SqlControler {
	
   private Connection conn = null;
   private Statement stmt = null;
 
   private static String url = "";
   private static String serverName= "";
   private static String portNumber = "";
   private static String databaseName= "";
   private static String userName = "";
   private static String passWord = "";   
   private static int batchCount=0;
   private static int batchLimit = 10000;
   private static int fetchLimit = 10000;
   private static String propertyFile = "config/database.properties"; 
   private static boolean rollback = false;

   /**
    * Create a connection string in the form "jdbc:mysql://localhost:3306/twitter?UseUnicode=true"
    */
   private String getConnectionUrl(){
	   Properties sqlProperties = new Properties();
	   try {
		   sqlProperties.load(new FileInputStream(propertyFile));
	   } catch (Exception e) {
		   e.printStackTrace();
	   }
	   url = sqlProperties.getProperty("url");
	   serverName = sqlProperties.getProperty("serverName");
	   portNumber = sqlProperties.getProperty("portNumber");
	   databaseName = sqlProperties.getProperty("databaseName");
	   userName = sqlProperties.getProperty("userName");
	   passWord =sqlProperties.getProperty("passWord"); 
	   batchLimit = Integer.parseInt(sqlProperties.getProperty("batchLimit"));
	   fetchLimit = Integer.parseInt(sqlProperties.getProperty("fetchLimit"));
	   return url+serverName+":"+portNumber+"/"+databaseName+"?UseUnicode=true&rewriteBatchedStatements=true";   
	}
   
   
	public boolean createConnection()
	{
		try 
		{
           Class.forName("com.mysql.jdbc.Driver");              
        }
		catch(Exception ex) 
		{
            System.out.println("Cannot Load Driver!");
            ex.printStackTrace();
            return false;
        }
		try
		{		   
			conn = DriverManager.getConnection(getConnectionUrl(),userName,passWord);
			stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,  
	                java.sql.ResultSet.CONCUR_READ_ONLY);
			stmt.setFetchSize(fetchLimit);
		
        	if(stmt == null) 
        	{
        		throw new Exception("Creating statement failed!");
        	}
        	
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return false;
		}
		return true;
	}

	public ResultSet query(String query)
	{
		try 
		{
			ResultSet rs = stmt.executeQuery(query);
			return rs;
		} 
		catch (SQLException e) 
		{
			e.printStackTrace();
			return null;
		}
	}
	
	
	
	public void update(String query)
	{
		try
		{
			stmt.executeUpdate(query);
		}
		catch (Exception ex)
		{
//		   System.out.println(query);
			ex.printStackTrace();
		}
	}
	
	
	public void clearBatch(Statement stmt)
	{
		try
		{
			stmt.clearBatch();
			batchCount = 0;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public PreparedStatement GetPreparedStatement(String sql){
		try {
			return conn.prepareStatement(sql);
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void addBatch(PreparedStatement prepStmt)
	{
		try
		{
			if(batchCount<batchLimit)
			{
				batchCount++;
				prepStmt.addBatch();
			}
			else
			{
				executeBatch(prepStmt);
				prepStmt.addBatch();
				batchCount = 1;
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void addBatch(String query)
	{
		try
		{
			if(batchCount<batchLimit)
			{
				batchCount++;
				stmt.addBatch(query);
			}
			else
			{
				executeBatch(stmt);
				stmt.addBatch(query);
				batchCount = 1;
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public int[] executeBatch(Statement stmt)
	{
		try
		{
			int[] status = stmt.executeBatch();
			clearBatch(stmt);
			return status;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return null;
		}
	}
	
	public void close()
	{
		try
		{
			stmt.close();
			conn.close();
		}
		catch (Exception ex)
		{
			
		}
	}

   public Connection getConn() {
      return conn;
   }

   public Statement getStmt() {
      return stmt;
   }

   public static String getUrl() {
      return url;
   }

   public static String getServerName() {
      return serverName;
   }

   public static String getPortNumber() {
      return portNumber;
   }

   public static String getDatabaseName() {
      return databaseName;
   }

   public static String getUSERNAME() {
      return userName;
   }

   public static String getPASSWORD() {
      return passWord;
   }

   public static int getBatchCount() {
      return batchCount;
   }

   public static int getBatchLimit() {
      return batchLimit;
   }

   public static String getProperties() {
      return propertyFile;
   }

   public static void setPropertyFile(String propertyFileName){
	   propertyFile = propertyFileName;
   }
   public static boolean isRollback() {
      return rollback;
   }

   public static void setRollback(boolean rollback) {
      SqlControler.rollback = rollback;
   }
	
	

}
