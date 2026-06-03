package com.ithows;

import org.apache.commons.fileupload.FileItem;

public class FileInfo
{
	String fileName;
	long fileSize;
        FileItem fileItem;
	public FileInfo(String fileName, long fileSize, FileItem fileItem){
		this.fileName = fileName;
		this.fileSize = fileSize;
                this.fileItem = fileItem;
	}
	public FileInfo(String fileName, long fileSize){
            this(fileName, fileSize, null);            
	}
	public String getFileName(){
		return this.fileName;
	}
	public void setFileName(String fileName){
		this.fileName = fileName;
	}
	public long getFileSize(){
		return this.fileSize;
	}
	public void setFileSize(long fileSize){
		this.fileSize = fileSize;
	}
        public void delete(){
            this.fileItem.delete();
        }
}