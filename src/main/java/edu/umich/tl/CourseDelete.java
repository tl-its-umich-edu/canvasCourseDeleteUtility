package edu.umich.tl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;

import edu.umich.tl.ApiCallHandler.CanvasApiEnum;



public class CourseDelete {
	private static final String CANVAS_URL = "canvas.url";
	private static final String CANVAS_TOKEN = "canvas.token";
	private static final String ESB_URL = "esb.url";
	private static final String API_CALL_TYPE = "api.call.type";
	private static Log M_log = LogFactory.getLog(CourseDelete.class);
	protected static String canvasURL = null;
	protected static String canvasToken = null;
	protected static String esbURL = null;
	//This parameter decide to make canvas or ESB api call
	protected static CanvasCallEnum canvasCall;

	public static void main(String[] args) {
		M_log.debug("main(): called");
		readPropertyFiles(args);
		ApiCallHandler apiHandler=new ApiCallHandler(canvasCall);
		getPreviousTerms(apiHandler);
	}
	

	public enum CanvasCallEnum{
		 API_DIRECT_CANVAS, 
		 API_ESB_CANVAS
	}

	private static void readPropertyFiles(String[] args) {
		M_log.debug("readPropertyFiles(): called");
		Properties properties = null;
		String propFileLocation;
		if(args.length == 0) {
			M_log.error("Command line arguments are not provided");
			System.exit(0);
		}
		propFileLocation = args[0];
		properties = Utils.getPropertiesFromURL(propFileLocation);
		canvasToken = properties.getProperty(CANVAS_TOKEN);
		canvasURL = properties.getProperty(CANVAS_URL);
		esbURL=properties.getProperty(ESB_URL);
		checkForApiType(properties);
	}

	private static void checkForApiType(Properties properties) {
		int apiCallType=Integer.parseInt(properties.getProperty(API_CALL_TYPE));
		if(apiCallType==1) {
			canvasCall=CanvasCallEnum.API_DIRECT_CANVAS;
		}else if(apiCallType==2) {
			canvasCall=CanvasCallEnum.API_ESB_CANVAS;
		}
	}
	
	private static void getPreviousTerms(ApiCallHandler apiHandler) {
		HttpResponse httpResponse = apiHandler.getApiResponse(CanvasApiEnum.TERM);
		int statusCode = httpResponse.getStatusLine().getStatusCode();
		if(statusCode!=200) {
			M_log.error("The api call getting \"Enrollemnt terms\" is UnSuccessfull");
			System.exit(0);
		}
		BufferedReader rd = null;
		try {
			rd=new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
			String line = "";
			StringBuilder sb = new StringBuilder();
			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}
			M_log.info(sb.toString());
		} catch (IllegalStateException e) {
			M_log.error("The api call getting \"Enrollemnt terms\" is UnSuccessfull has IllegalStateException ",e);
		} catch (IOException e) {
			M_log.error("The api call getting \"Enrollemnt terms\" is UnSuccessfull has IOException ",e);
		}
	}
	
	public static boolean isEmpty(String value) {
		return (value == null) || (value.trim().equals(""));
	}

}
