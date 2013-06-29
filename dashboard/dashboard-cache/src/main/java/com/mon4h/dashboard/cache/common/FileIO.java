package com.mon4h.dashboard.cache.common;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class FileIO {

	public static boolean dirExistCreate( String path ) {
		
		try {
			
			File file = new File(path);
			if(!file .isDirectory())      
			{
			    file .mkdir();
			    return false;
			}
			return true;
		} catch( Exception e ) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean fileExistCreate( String path,String filename ) {
		
		try {
			File file = new File(path);
			if( !file.exists() )      
			{
				file.mkdirs();	
			}
			
			String filePath = path + File.separator + filename;
			File file1 =  new File(filePath);
			if( !file1.exists() ) {
				file1.createNewFile();
				if( !file1.exists() ) {
					return false;
				}
			}
			return true;
			
		} catch( Exception e ) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean isExist( String path ) {
		
		try {
			
			File file = new File(path);
			if( !file.exists() )      
			{
			    return false;
			}
			return true;
		} catch( Exception e ) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static void deleteFile(String fileName) {
		
		try{
			File file = new File(fileName);     
			if(file.isFile() && file.exists()){     
				file.delete();  
			}  
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

     public static void delFolder(String folderPath) {
	     try {
			delAllFile(folderPath);
			String filePath = folderPath;
			filePath = filePath.toString();
			File myFilePath = new File(filePath);
			myFilePath.delete();
	     } catch (Exception e) {
	       e.printStackTrace(); 
	     }
     }

     public static boolean delAllFile(String path) {
    	 
    	 boolean flag = false;
    	 try {
    		 
	    	 File file = new File(path);
	    	 if (!file.exists()) {
	    		 return flag;
	    	 }
	    	 if (!file.isDirectory()) {
	    		 return flag;
	    	 }
	    	 String[] tempList = file.list();
	    	 File temp = null;
	    	 for (int i = 0; i < tempList.length; i++) {
	    		 if (path.endsWith(File.separator)) {
	    			 temp = new File(path + tempList[i]);
	    		 } 	else {
	    			 temp = new File(path + File.separator + tempList[i]);
	    		 }
	    		 if (temp.isFile()) {
	    			 temp.delete();
	    		 }
	    		 if (temp.isDirectory()) {
	    			 delAllFile(path + "/" + tempList[i]);
	    			 delFolder(path + "/" + tempList[i]);
	    			 flag = true;
	    		 }
	    	 }
	    	 return flag;
    	 
     	} catch (Exception e) {
 	       e.printStackTrace(); 
 	    }
		return flag;
     }
     
     public static List<String> getAllDir( String path ) {
    	 
    	 try {
    		 List<String> str = new LinkedList<String>();
    		 
	    	 File file = new File(path);
	    	 if( file == null || file.exists() == false ) {
	    		 return null;
	    	 }
	         File[] array = file.listFiles();   
	           
	         for( int i=0; i<array.length; i++ ) {
	        	 
	             if(array[i].isDirectory()) {
	            	 str.add( array[i].getName() ); 
	             }
	         }
	         return str;
	         
    	 } catch ( Exception e ) {
    		 e.printStackTrace(); 
    	 }
    	 
    	 return null;
     }
	
}
