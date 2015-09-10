package edu.umich.tl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;

import edu.umich.tl.ApiCallHandler.ApiTypes;



public class CourseDelete {
	private static final String CANVAS_URL = "canvas.url";
	private static final String CANVAS_TOKEN = "canvas.token";
	private static final String ESB_URL = "esb.url";
	private static Log M_log = LogFactory.getLog(CourseDelete.class);
	protected static String canvasURL = null;
	protected static String canvasToken = null;
	protected static String esbURL = null;
	//This parameter decide to make canvas or ESB api call
	private static int typeOfApiCall;

	public static void main(String[] args) {
		M_log.info("main(): called");
		readPropertyFiles(args);
		ApiCallHandler apiHandler=new ApiCallHandler();
		apiCurrentTerm(apiHandler);
	}

	private static void readPropertyFiles(String[] args) {
		M_log.debug("readPropertyFiles(): called");
		Properties properties = null;
		String propFileLocation;
		 if(args.length == 0) {
			 M_log.error("Command line arguments are not provided");
			 System.exit(0);
		 }
		    propFileLocation = args[1];
		    typeOfApiCall=Integer.parseInt(args[0]);
			properties = DeleteUtils.getPropertiesFromURL(propFileLocation);
			canvasToken = properties.getProperty(CANVAS_TOKEN);
			canvasURL = properties.getProperty(CANVAS_URL);
			esbURL=properties.getProperty(ESB_URL);
	}
	
	private static void apiCurrentTerm(ApiCallHandler apiHandler) {
		 HttpResponse httpResponse = apiHandler.getApiResponse(ApiTypes.Term,typeOfApiCall);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			if(statusCode!=200) {
				M_log.error("The api call getting \"Enrollemnt terms\" is UnSuccessfull");
				 System.exit(0);
			}
			BufferedReader rd = null;
			long startTime = System.nanoTime();
			try {
				rd=new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
				long stopTime = System.nanoTime();
				long elapsedTime = stopTime - startTime;
				M_log.info(String.format("Api call to get \"Enrollment terms\" took %snano sec",elapsedTime));
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
