package com.coffee.web.fileupload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.google.gson.Gson;

import com.coffee.web.FormData;


// 请在 web.xml 配置本 Servlet
public class UploadService extends HttpServlet
{
	protected HashMap<String, ConfigItem> configs = new HashMap<String, ConfigItem>();
		
	@Override
	public void init() throws ServletException
	{		
		// 从xml配置文件中读取配置
		try
		{
			loadConfig();
		} catch (Exception e)
		{
			throw new RuntimeException("LW-service.xml: " + e.getMessage());
		}
	}

	protected void doPost(HttpServletRequest request, 
			HttpServletResponse response) throws ServletException, IOException
	{
		Gson jresp = new Gson();
		Map<String, Object> info = new HashMap <String, Object>();
		try
		{
			// 从URL中解析服务的名字
			// servletPath: "/.../hello.up"
			String servletPath = request.getServletPath();
			int p1 = servletPath.lastIndexOf('/');
			int p2 = servletPath.lastIndexOf('.');
			String serviceName = servletPath.substring(p1 + 1, p2);
			
			// 创建服务类的对象, 处理该请求
			ConfigItem cfg = configs.get(serviceName);
			if(cfg == null)
				throw new Exception("上传服务: " + serviceName +"在LW-service.xml里尚未配置!");
			
			UploadHandler instance = null;
			try{
				instance = (UploadHandler) cfg.clazz.newInstance();				
			}catch(InstantiationException e){
				e.printStackTrace();
				throw new Exception(cfg.clazzName + "无法实例化, 请确保构造方法不带参数!");
			}catch(IllegalAccessException e){
				e.printStackTrace();
				throw new Exception(cfg.clazzName + "无法实例化, 请确保构造方法为public!");
			}catch(ClassCastException e){
				e.printStackTrace();
				throw new Exception(cfg.clazzName + "必须是  FileUploadHandler 的子类(或子类的子类)!");
			}catch(Exception e)	{
				e.printStackTrace();
				throw new Exception("在创建 " + cfg.clazzName + "实例的时候出错!请检查构造方法是否有异常!");
			}
			
			// 处理请求
			instance.httpReq = request;
			instance.httpResp = response;
			instance.charset = cfg.charset;			
			Object data = doUpload(request, response, instance);
			info.put("error", 0);
			info.put("reason",  "OK");
			if(data != null)
				info.put("data", data);
			
		} catch (Exception e)
		{
			info.put("error", -1);
			info.put("reason", e.getMessage());
		}
		
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/plain");			
		PrintWriter out = response.getWriter();
		out.write(jresp.toJson(info));
		out.close();
	}
	
	private Object doUpload(HttpServletRequest request, 
			HttpServletResponse response,
			UploadHandler handler) throws Exception
	{		
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);
		if(!isMultipart)
			throw new Exception("请求编码必须为: multipart/form-data !");
			
		// 文件上传的同时，可以上传其他一些表单字段
		FormData formData = new FormData();	
		
		// ServletFileUpload ： commons包里提供的工具类
		ServletFileUpload upload = new ServletFileUpload();			
		FileItemIterator iter = upload.getItemIterator(request);
		while (iter.hasNext()) 
		{
			// 表单域 
		    FileItemStream item = iter.next();
		    String fieldName = item.getFieldName();
		    InputStream fieldStream = item.openStream();
		    if ( item.isFormField())
		    {
		    	String fieldValue = Streams.asString(fieldStream, handler.charset);
		    	formData.put(fieldName, fieldValue);
		    }else 
		    {
		    	// 创建临时目录
		    	File tmpDir = handler.getTmpDir();
		    	if( ! tmpDir.exists())
		    		tmpDir.mkdirs();
		    	handler.tmpDir = tmpDir;
		    	
		      	// 临时文件名
		    	String realName = item.getName(); // 原始文件名
		    	File tmpFile = handler.getTmpFile(tmpDir, realName);
		    	handler.realName = realName;
		    	handler.tmpFile = tmpFile;
		    	
		    	// 保存数据到 tmpFile
		    	OutputStream outputStream = new FileOutputStream(tmpFile);
		    	//printLog("文件上传开始:" + realName );	
		        long fileSize = 0; // 已上传的字节数		
		        try{		        	
		        	fileSize = copy(fieldStream, outputStream, handler);
		        }
		        catch(Exception ex){		        	
		        	throw ex;
		        }
		        finally{
		        	try{ fieldStream.close();}catch(Exception e){}	
		        	try{ outputStream.close();} catch(Exception e){}
		        }
		        
		        //printLog("文件上传完成:" + realName + ", 大小: " + fileSize);		        
		        return handler.complete(fileSize, formData);
		    }
		}	
		
		// 如果请求里根本没有文件域,则提示异常（前台代码漏写了)		
		throw new Exception("请求里没有文件域!");
	}
	
	public long copy(InputStream in, 
			OutputStream out,
			UploadHandler handler) throws Exception
	{
		// 文件大小的限制
		int maxSize = handler.getMaxSize();
		
		// 保存数据到文件
		long count = 0;
		byte[] buf = new byte[8192];
		while (true)
		{
			int n = in.read(buf);
			if (n < 0)
				break;
			if (n == 0)
				continue;
			out.write(buf, 0, n);

			count += n;
			
			if( maxSize >0 && count > maxSize)
				throw new Exception("文件太大 , 不得大于 > " + maxSize + "!");
			
			// 可以适当 sleep 以限制上传速度
		}
		return count;
	}
		
	/////////////////////////////////////
	// LW-service.xml 中的配置项
	// <upload name="CommonFile" class="com.coffee.CommonFileUpload" />
	class ConfigItem
	{
		public String name;       // 服务接口名
		public String clazzName;
		public Class  clazz;      // 类的实体
		public String charset = "UTF-8";
	}
	
	// 从 af-service.xml 中获取配置
	private void loadConfig() throws Exception
	{		
		InputStream stream = this.getClass().getResourceAsStream("/LW-service.xml");
		if(stream == null)
			throw new Exception("找不到 LW-service.xml,请确保有此配置文件!");
		
		// 读取XML
		SAXReader reader = new SAXReader();
		Document doc = reader.read(stream);
		stream.close();
		
		Element root = doc.getRootElement();
		List<Element> xServiceList = root.elements("upload");
		for (Element e : xServiceList)
		{
			ConfigItem cfg = new ConfigItem();
			cfg.name = e.attributeValue("name");
			cfg.clazzName = e.attributeValue("class");	
			 
			try{
				// 加载类的信息
				cfg.clazz  = Class.forName(cfg.clazzName);				
			}catch(Exception ex)
			{
				throw new Exception("类 " + cfg.clazzName + "不存在!");
			}	
			
			// 检查是不是 UploadHandler 的子类
			if(cfg.clazz.isAssignableFrom( UploadHandler.class))
				throw new Exception("类" + cfg.clazzName + "不是UploadHandler的子类!无法加载!");
			
			configs.put(cfg.name, cfg );
		}
	}
}
