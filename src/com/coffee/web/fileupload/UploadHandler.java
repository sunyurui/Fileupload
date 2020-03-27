package com.coffee.web.fileupload;


import java.io.File;
import java.io.OutputStream;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.coffee.web.FormData;

public abstract class UploadHandler
{
	// httpReq : 请求对象
	public HttpServletRequest httpReq;
	// httpResp : 应答对象
	public HttpServletResponse httpResp;
	// charset： 字符编码
	public String charset ; 
	
	// 上下文
	public File tmpDir; // 上传的临时目录
	public File tmpFile; // 上传的文件
	public String realName; // 文件原来的名称

	public abstract File getTmpDir();
	
	public abstract Object complete(long size, FormData formData);
	
	/**
	 * 获取临时文件
	 * @param tmpDir 临时文件的目录
	 * @param realName 文件的原始文件名
	 * @return
	 */
	public File getTmpFile(File tmpDir, String realName)
	{
		String suffix = UploadUtils.fileSuffix(realName);
		String uuid = UploadUtils.createUUID();
		String fileName = uuid + "." + suffix;
		
		return new File(tmpDir, fileName);
	}
	
	/**
	 * 返回0表示不限制大小
	 */
	public int getMaxSize()
	{
		return 0;
	}
	
}
