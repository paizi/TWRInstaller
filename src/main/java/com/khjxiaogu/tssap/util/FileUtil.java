package com.khjxiaogu.tssap.util;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class FileUtil {
	public static void transfer(InputStream i,OutputStream os) throws IOException {
		int nRead;
		byte[] data = new byte[4096];
	
		try {
			while ((nRead = i.read(data, 0, data.length)) != -1) { os.write(data, 0, nRead); }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw e;
		}
	}
	public static void transferWithListener(InputStream i,OutputStream os,Consumer<Long> readed) throws IOException {
		int nRead;
		byte[] data = new byte[16384];
		long tread=0;
		try {
			while ((nRead = i.read(data, 0, data.length)) != -1) { os.write(data, 0, nRead);tread+=nRead;readed.accept(tread); }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw e;
		}
		readed.accept(tread); 
	}
	public static void transfer(File i,OutputStream os) throws IOException {
		try (FileInputStream fis=new FileInputStream(i)){
			transfer(fis,os);
		}
	}
	public static void transfer(InputStream i,File f) throws IOException {
		try (FileOutputStream fos=new FileOutputStream(f)){
			transfer(i,fos);
		}
	}
	public static void transfer(String i,File os) throws IOException {
		try (FileOutputStream fos=new FileOutputStream(os)){
			fos.write(i.getBytes(StandardCharsets.UTF_8));
		}
	}
	public static void transfer(byte[] i,File os) throws IOException {
		try (FileOutputStream fos=new FileOutputStream(os)){
			fos.write(i);
		}
	}
	public static byte[] readAll(InputStream i) throws IOException {
		ByteArrayOutputStream ba = new ByteArrayOutputStream(16384);
		int nRead;
		byte[] data = new byte[4096];
	
		try {
			while ((nRead = i.read(data, 0, data.length)) != -1) { ba.write(data, 0, nRead); }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw e;
		}
	
		return ba.toByteArray();
	}
	public static String readString(InputStream i) throws IOException {
		return new String(readAll(i),StandardCharsets.UTF_8);
	}
	public static String readStringOrEmpty(InputStream i){
		try {
			return readString(i);
		} catch (Throwable e) {
		}
		return "";
	}
	public static String readStringOrEmpty(File f){
		try {
			return readString(f);
		} catch (Throwable e) {
		}
		return "";
	}
	public static String readString(File f) throws IOException {
		return new String(readAll(f),StandardCharsets.UTF_8);
	}

	public static byte[] readAll(File f) throws IOException {
		try(FileInputStream fis=new FileInputStream(f)){
			return readAll(fis);
		}
	}
	public static byte[] readIgnoreSpace(InputStream i) throws IOException {
		ByteArrayOutputStream ba = new ByteArrayOutputStream(16384);
		int nRead;
		byte[] data = new byte[4096];

		try {
			while ((nRead = i.read(data, 0, data.length)) != -1) {
				for(int j=0;j<nRead;j++) {
					byte b=data[j];
					if (!(b == 9 || b == 10 || b == 13 || b == 32)) {
						ba.write(b);
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw e;
		}

		return ba.toByteArray();
	}
	public static byte[] readIgnoreSpace(File f) throws IOException {
		try (FileInputStream fis = new FileInputStream(f)) {
			return readIgnoreSpace(fis);
		}
	}
	public static InputStream fetch(String url) throws IOException {
		HttpURLConnection huc2 = (HttpURLConnection) new URL(url).openConnection();
		huc2.setRequestMethod("GET");
		huc2.setDoOutput(true);
		huc2.setDoInput(true);
		huc2.connect();
		if(huc2.getResponseCode()==200)
			return huc2.getInputStream();
		throw new IOException("HTTP"+huc2.getResponseCode()+" "+huc2.getResponseMessage()+" got while fetching "+url);
	}
	public static InputStream fetchWithRetry(String url,int maxRetry) throws IOException {
		int cRetry=maxRetry;
		do {
			try {
				return fetch(url);
			}catch(IOException ex) {
				LogUtil.addError("fetch "+url+" failed, retries "+(maxRetry-cRetry)+"/"+maxRetry, ex);
			}
		}while(--cRetry>0);
		throw new IOException("fetch "+url+" failed "+maxRetry+" times, no more tries.");
	}
	public static HttpURLConnection fetchWithSize(String url) throws IOException {
		HttpURLConnection huc2 = (HttpURLConnection) new URL(url).openConnection();
		huc2.setRequestMethod("GET");
		huc2.setDoOutput(true);
		huc2.setDoInput(true);
		huc2.connect();
		long ctl=huc2.getContentLengthLong();
		if(huc2.getResponseCode()==200)
			return huc2;
		throw new IOException("HTTP"+huc2.getResponseCode()+" "+huc2.getResponseMessage()+" got while fetching "+url);
	}
	public static HttpURLConnection fetchWithRetryAndSize(String url,int maxRetry) throws IOException {
		int cRetry=maxRetry;
		do {
			try {
				return fetchWithSize(url);
			}catch(IOException ex) {
				LogUtil.addError("fetch "+url+" failed, retries "+(maxRetry-cRetry)+"/"+maxRetry, ex);
			}
		}while(--cRetry>0);
		throw new IOException("fetch "+url+" failed "+maxRetry+" times, no more tries.");
	}
}