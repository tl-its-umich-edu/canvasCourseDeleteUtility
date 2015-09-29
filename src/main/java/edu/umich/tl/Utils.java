package edu.umich.tl;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



public class Utils {
private static Log M_log = LogFactory.getLog(Utils.class);
    
	public static Properties getPropertiesFromURL(String propertiesFileURL) {
		Properties props = new Properties();
		InputStream in = null;

		in = getInputStreamFromURL(propertiesFileURL);

		if (in == null) {
			return null;
		}

		//  Got a source of properties.  Try to read them.
		try {
			props.load(in);
		} catch (IOException e) {
			M_log.error("IO exception loading propertiesFileURL: "+propertiesFileURL,e);
			return null;
		}

		return props;
	}
	
	public static InputStream getInputStreamFromURL(String fileURL) {

		InputStream in = null;
		URL url = null;

		// check if some url string was supplied.
		if (fileURL == null || fileURL.length() == 0) {
			M_log.debug("null or zero length fileURL supplied");
			return null;
		}

		// open a connection to the file specified by that url.
		try {
			url = new URL(fileURL);
		} catch (MalformedURLException e) {
			M_log.error("file URL is malformed: "+fileURL,e);
			return null;
		}

		// try to get access to that file.
		try {
			in = url.openStream();
		} catch (IOException e) {
			M_log.error("IO exception opening fileURL: "+fileURL,e);
			return null;
		}

		return in;
	}
	
	public static boolean isEmpty(String value) {
		return (value == null) || (value.trim().equals(""));
	}
	
}
