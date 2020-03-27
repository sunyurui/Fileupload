package com.coffee.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.coffee.web.FormData;
import com.coffee.web.fileupload.UploadHandler;
import com.coffee.web.fileupload.UploadUtils;

/** 
 * 文件上传处理类
 */
public class Uploader extends UploadHandler
{
	String tmpFileName;

	@Override
	public File getTmpDir()
	{
		// httpReq 在父类 AfUploadHandler里定义
		String path = httpReq.getServletContext().getRealPath("/upload");
		return new File( path);
	}
	
	@Override
	public File getTmpFile(File tmpDir, String realName)
	{
		String suffix = UploadUtils.fileSuffix(realName);
		String uuid = UploadUtils.createUUID();
		this.tmpFileName = uuid + "." + suffix;
		
		return new File(tmpDir, tmpFileName);
	}

	@Override
	/**
	 * 如果夹杂表单数据，使用formdata进一步处理
	 */
	public Object complete(long size, FormData formData)
	{
		String storePath = "/upload/" + tmpFileName;
		String contextPath = httpReq.getServletContext().getContextPath();
		String url =  contextPath + storePath;
		
		Map<String, Object> result = new HashMap <String, Object>();
		result.put("storePath", storePath);
		result.put("url", url);		
		return result;
	}
}
