package com.coffee.web.fileupload;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class UploadUtils
{

	// 生成一个唯一的ID
	public static String createUUID ()
	{
		 String s = UUID.randomUUID().toString(); 
	     String s2 = s.substring(0,8)+s.substring(9,13)+s.substring(14,18)+s.substring(19,23)+s.substring(24); 
	     return s2.toUpperCase();
	}
	
	// 得到一个保证不重复的临时文件名
	public static String createTmpFileName(String suffix)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
		String datestr = sdf.format(new Date());
		String name = datestr + "-" + createUUID() + "." + suffix;
		return name;
	}
	
	// 得到文件的后缀名
	public static String fileSuffix(String fileName)
	{
		int p = fileName.lastIndexOf('.');
		if(p >= 0)
		{
			return fileName.substring(p+1).toLowerCase();
		}
		return "";
	}
	
	
}
