package edu.umich.tl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;

import edu.umich.tl.ApiCallHandler.RequestTypeEnum;



public class CourseDelete {
	
	private static final String TEXT_CSV = "text/csv";
	private static final String MANUAL = "manual";
	private static final String PUBLISHED = "published";
	private static final String CONFERENCES = "conferences";
	private static final String EVENTS = "events";
	protected static final String MAIL_SMTP_AUTH = "mail.smtp.auth";
	protected static final String MAIL_SMTP_STARTTLS = "mail.smtp.starttls.enable";
	protected static final String MAIL_SMTP_HOST = "mail.smtp.host";
	private static final String FALSE  = Boolean.FALSE.toString();
	//properties from canvasCourseDelete.properties
	private static final String TERM = "term";
	private static final String TERM_COUNT = "term.count";
	private static final String CANVAS_URL = "canvas.url";
	private static final String CANVAS_TOKEN = "canvas.token";
	private static final String ESB_URL = "esb.url";
	private static final String API_CALL_TYPE = "api.call.type";
	private static final String COURSE_DELETE_REPORT_SEND_EMAILADDRESS = "course.delete.report.send.emailaddress";
	private static final String CANVAS_COURSE_DELETE_MAILHOST = "canvas.course.delete.mailhost";
	private static final String MAIL_DEBUG_PROPERTY = "mail.debug";
	private static final String DELETE_COURSE_ENABLED = "delete.course";
	protected static HashMap<String, DateTime> termsInfo = new HashMap<String, DateTime>();
	
	protected static String canvasURL = null;
	protected static String canvasToken = null;
	protected static String esbURL = null;
	private static String mailHost=null;
	private static String toEmailAddress=null;
	private static String mailDebug =FALSE;
	//This parameter decide to make canvas or ESB api call
	protected static CanvasCallEnum canvasCall;
	private static  String deletedCoursesFileName="CanvasCourseDeleteReport%s.csv";
	private static  String contentCoursesFileName="CanvasContentCourseReport%s.csv";
	private static  String emailSubjectText="Report from canvas course delete utility for terms %s";
	
	private static Log M_log = LogFactory.getLog(CourseDelete.class);
	private static boolean isCourseDeleteEnabled=false;
	

	public static void main(String[] args) {
		M_log.debug("main(): called");
		readPropertiesFromFile(args);
		ApiCallHandler apiHandler=new ApiCallHandler(canvasCall);
		ArrayList<Term> terms = getTerms(apiHandler);
		ArrayList<Term> previousTerms = getPreviousTerms(terms);
		if(previousTerms.isEmpty()) {
			M_log.error("No previous terms could be determined for courses deletion");
			System.exit(1);
		}
		/*
		 * Their may be multiple term courses that needs 
		 * to be deleted for a cron job at that point in time. The Unpublished course list can be huge so the 
		 * design would be focusing on one term at a time and verifying for each course in a term if people exist, content added and activity happened for deleting the course. 
		 *  
		 */
		CoursesForDelete coursesForDelete=new CoursesForDelete();
		for (Term previousTerm : previousTerms) {
			Stopwatch stopwatch = Stopwatch.createStarted();
			manageCoursesDeletion(previousTerm,coursesForDelete,apiHandler,null);
			stopwatch.stop();
			M_log.info("The delete process of unused courses for the term "+ previousTerm.getTermName()+" took: "+stopwatch);
		}
		sendAnEmailReport(previousTerms,coursesForDelete);
		M_log.debug("TotalCourses: "+coursesForDelete.getCourses().size());
		M_log.debug("TotalDeletedCourse: "+coursesForDelete.getDeletedUnpublishedCourses().size());
	}




	public enum CanvasCallEnum{
		 API_DIRECT_CANVAS, 
		 API_ESB_CANVAS
	}
     /*
      * exit code '1' implies something I expected could potentially go wrong
      * http://stackoverflow.com/questions/2434592/difference-in-system-exit0-system-exit-1-system-exit1-in-java 
      */
	private static void readPropertiesFromFile(String[] args) {
		M_log.debug("readPropertyFiles(): called");
		Properties properties = null;
		String propFileLocation;
		if(args.length == 0) {
			M_log.error("Command line arguments are not provided");
			System.exit(1);
		}
		propFileLocation = args[0];
		properties = Utils.getPropertiesFromURL(propFileLocation);
		canvasToken = properties.getProperty(CANVAS_TOKEN);
		canvasURL = properties.getProperty(CANVAS_URL);
		esbURL=properties.getProperty(ESB_URL);
		mailHost=properties.getProperty(CANVAS_COURSE_DELETE_MAILHOST);
		toEmailAddress=properties.getProperty(COURSE_DELETE_REPORT_SEND_EMAILADDRESS);
		mailDebug = properties.getProperty(MAIL_DEBUG_PROPERTY);
		isCourseDeleteEnabled = Boolean.parseBoolean(properties.getProperty(DELETE_COURSE_ENABLED,FALSE));
		getTermsInfoFromProperties(properties);
		checkForApiType(properties);
	}
	
	/*
	 * The Terms that needs for course deletion are listed in the properties file. Each term has 2 piece of information 
	 * term1=2030;04/06/15, the sisTermCode and the endDate after which the courses should be considered for deletion.
	 * We will check if the current system date is past the endDate then that term becomes eligible for deleting the 
	 * unused/unpublished courses that are created as apart of auto-provisioning of courses in canvas
	 */
	private static void getTermsInfoFromProperties(Properties properties) {
		M_log.debug("getTermsInfoFromProperties(): called");
		String termCount = properties.getProperty(TERM_COUNT);
		if(termCount.isEmpty()) {
			M_log.error("The property \"term.count\" is not set in the properties file \"canvasCourseDelete.properties\"");
			System.exit(1);
		}
		int termCountInt = Integer.parseInt(termCount);
		DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("MM/dd/yyyy");
		for(int i=1;i<=termCountInt;i++) {
			String termPropertyName = TERM+String.valueOf(i);
			String termPropertyValue = properties.getProperty(termPropertyName);
			if(Utils.isEmpty(termPropertyValue)) {
				M_log.error("The property \""+termPropertyName+"\" don't seems to exist in \"canvasCourseDelete.properties\" "
						+ "please make sure all term properties are numerically aligned and number of properties is equal to \"term.count\" property value");
				System.exit(1);
			}
			String[] termWithDate = termPropertyValue.split(";");
			String sisTermId = termWithDate[0];
			DateTime endDate = dateTimeFormatter.parseDateTime(termWithDate[1]);
			M_log.info("EndDate from propertiesFile: "+endDate);
			termsInfo.put(sisTermId,endDate);
		}

	}
	
     /*
      * This function decide if the api call that need to be made is 'CanvasDirect' or
      * 'CanvasEsb'.we will pass this information to the 'ApiCallHandler'. The 'ApiCallHandler'
      * will always be aware what type of call to make
      */
	private static void checkForApiType(Properties properties) {
		M_log.debug("checkForApiType(): called");
		int apiCallType=Integer.parseInt(properties.getProperty(API_CALL_TYPE));
		if(apiCallType==1) {
			canvasCall=CanvasCallEnum.API_DIRECT_CANVAS;
		}else if(apiCallType==2) {
			canvasCall=CanvasCallEnum.API_ESB_CANVAS;
		}
	}
	/*
	 *Get the enrollment_terms that are available in canvas during that point of the time.
	 */
	private static ArrayList<Term> getTerms(ApiCallHandler apiHandler) {
		M_log.debug("getTerms(): called");
		ArrayList<Term> enrollmentTerms = new ArrayList<Term>();
		HttpResponse httpResponse = apiHandler.getApiResponse(RequestTypeEnum.TERM, null,null,null);
		httpResponseNullCheck(httpResponse,RequestTypeEnum.TERM);
		int statusCode = httpResponse.getStatusLine().getStatusCode();
		if(statusCode!=200) {
			M_log.error(apiCallErrorHandler(httpResponse,RequestTypeEnum.TERM,apiHandler));
			System.exit(1);
		}
		HttpEntity entity = httpResponse.getEntity();
		Map<String,Object> terms = new HashMap<String,Object>();
		ObjectMapper mapper = new ObjectMapper();
		try {
			String jsonResponseString = EntityUtils.toString(entity);

			terms = mapper.readValue(jsonResponseString,new TypeReference<HashMap<String,Object>>(){});
			ArrayList<HashMap<String, Object>> termsList = (ArrayList<HashMap<String, Object>>) terms.get("enrollment_terms");
			for (HashMap<String, Object> eachTerm : termsList) {
				String startDate = (String)eachTerm.get("start_at");
				String endDate = (String)eachTerm.get("end_at");
				if(startDate!=null&&endDate!=null) {
					Term term =new Term()
							.setCanvasTermId(String.valueOf((Integer)eachTerm.get("id")))
							.setSisTermId((String)eachTerm.get("sis_term_id"))
							.setTermName((String)eachTerm.get("name"));
					enrollmentTerms.add(term);
				} 
			}
		} catch (JsonParseException e) {
			M_log.error("getTerms() has JsonParseException",e);
		} catch (JsonMappingException e) {
			M_log.error("getTerms() has JsonMappingException",e);
		} catch (IOException | ParseException e) {
			M_log.error("getTerms() has IOException",e);
		}
		return enrollmentTerms;
	}

	/*
	 * This method returns the List of terms for deletion of unused and unpublished courses. 
	 */
	private static ArrayList<Term> getPreviousTerms(ArrayList<Term> terms) {
		M_log.debug("getPreviousTerms(): called");
		DateTime todaysDate = new DateTime();
		M_log.info("Date the cron job ran: "+todaysDate);
		ArrayList<Term> termsForCoursesDelete = new ArrayList<Term>();
		for (Entry<String, DateTime> term : termsInfo.entrySet()) {
			if(term.getValue().isBefore(todaysDate)) {
				Term termForCourseDelete = getTheTermCanvasTermIdExistForProvidedSisId(terms, term.getKey());
				if(termForCourseDelete==null) {
					M_log.error("Cannot find \"CanvasTermId\" for \"sisTermId = "+term.getKey()+
							"\" provided. Please correct the \"sis.term.id\" property provided from \"canvasCourseDelete.properties\" file.");
					continue;
				}
				termsForCoursesDelete.add(termForCourseDelete);
			}

		}
		return termsForCoursesDelete;
	}
    /*
     * For provided sis_term_id from the properties file 'canvasCourseDelete.properties' we try to
     * find if corresponding canvasTermId exist and return the term
     */
	private static Term getTheTermCanvasTermIdExistForProvidedSisId(ArrayList<Term> terms, String termSisId) {
		M_log.debug("getTheTermCanvasTermIdExistForProvidedSisId(): called");
		for (Term term : terms) {
			if (term.getSisTermId().equalsIgnoreCase(termSisId)) {
				M_log.info("Found canvasTermId for provided SISTermId for the Term:  "+ term.getTermName());
				return term;
			}
		}
		return null;
	}
	/*
	 * Getting all the unpublished course list for a particular term from the ROOT account which is '1'. All the colleges, schools of UOfM fall under ROOT.
	 * This list is huge, canvas only provided 100 result set per api call so we need to get the rest of the courses info from the Pagination links. For Each 
	 * course we will be checking for courseEndDate/content/Activity to determine the course deletion.
	 */
	private static void manageCoursesDeletion(Term previousTerm, CoursesForDelete coursesForDelete, ApiCallHandler apiHandler, String url) {
		M_log.debug("manageCoursesDeletion(): Called");
		HttpResponse httpResponse=null;
		if(url!=null) {
			//sending null for canvasTermId is fine since we get the url framed from the pagination object from the response header. 
			httpResponse=apiHandler.getApiResponse(RequestTypeEnum.UNPUBLISHED_COURSE_LIST_PAGINATION_URL,null,url,null);
		}else {
			httpResponse = apiHandler.getApiResponse(RequestTypeEnum.UNPUBLISHED_COURSE_LIST,previousTerm.getCanvasTermId(),url,null);
		}
		httpResponseNullCheck(httpResponse,RequestTypeEnum.UNPUBLISHED_COURSE_LIST);
		int statusCode = httpResponse.getStatusLine().getStatusCode();
		if(statusCode!=200) {
			M_log.error(apiCallErrorHandler(httpResponse,RequestTypeEnum.UNPUBLISHED_COURSE_LIST,apiHandler));
			System.exit(1);
		}
		HttpEntity entity = httpResponse.getEntity();
		List<HashMap<String, Object>> courseList=new ArrayList<HashMap<String, Object>>();
		ObjectMapper mapper = new ObjectMapper();
		try {
			String jsonResponseString = EntityUtils.toString(entity);
			courseList = mapper.readValue(jsonResponseString,new TypeReference<List<Object>>(){});
			if(courseList.isEmpty()) {
				M_log.info("There are no unpublished courses for the term: "+previousTerm.getTermName());
				return;
			}
			Stopwatch stopwatch = Stopwatch.createStarted();
			for (HashMap<String, Object> course : courseList) {
				Course aCourse=new Course()
						.setCourseId(String.valueOf((Integer)course.get("id")))
						.setCourseName((String)course.get("name"))
						.setStartDate(Utils.changeToDate((String)course.get("start_at")))
						.setEndDate(Utils.changeToDate((String)course.get("end_at")));

				coursesForDelete.addCourse(aCourse);
				checkForEndDateContentActivityInACourse(aCourse, apiHandler,coursesForDelete);
			}
			stopwatch.stop();
			M_log.info("^^^^^ The API call for "+courseList.size()+" Courses took: "+stopwatch);
			String nextPageUrl = getNextPageUrl(httpResponse);
			if(nextPageUrl!=null) {
				manageCoursesDeletion(previousTerm,coursesForDelete,apiHandler,nextPageUrl);
			}
		} catch (JsonParseException e1) {
			M_log.error("JsonParseException occured manageCoursesDeletion() : ",e1);
		} catch(JsonMappingException e1) {
			M_log.error("JsonMappingException occured manageCoursesDeletion() : ",e1);
		}  catch (IOException e) {
			M_log.error("IOException occured manageCoursesDeletion( ):" ,e);
		}
	}
	/*
	 * For deletion of a course we need to check  1) courseEndDate with respect to current system date.If endDate is past the current system
	 * date then course becomes eligible to for delete 2) after passing the endDate check we will be checking if the course has any content in it. We will be querying 
	 * various canvas tool api's  to determine.3)If their is no content exist in the course check should be made for activity. if all these check show the course never been used 
	 * then we will delete the course
	 */
	private static void checkForEndDateContentActivityInACourse(Course course,ApiCallHandler apiHandler,CoursesForDelete coursesForDelete) {
		if(!isCourseEndDateIsPastTheCurrentDate(course)) {
			if(!isThereContentInCourse(course,apiHandler)) {
				if(!isThereActivityInCourse(course,apiHandler,null)) {
					deleteTheCourse(course,apiHandler,coursesForDelete);
				}
			}
		}
		
	}
    /*
     * Some time a course End date may be set after the term end date, in that cases we don't want to delete the course.
     * Course end date can be null as well i.e it might not be set at all.
     */
	private static boolean isCourseEndDateIsPastTheCurrentDate(Course course) {
		M_log.debug("isCourseEndDateIsPastTheCurrentDate: "+course.getCourseId()+" and endDate: "+course.getEndDate());
		if(course.getEndDate()==null || course.getEndDate().isBefore(new DateTime(DateTimeZone.UTC))) {
			M_log.debug("!!! Course eligible for Delete !!!" );
			return false;
		}
		return true;
	}

	private static boolean isThereContentInCourse(Course course, ApiCallHandler apiHandler) {
		M_log.debug("isTheirContentInCourse: "+course.getCourseId());
		if(areThereFiles(course.getCourseId(),apiHandler)) {
			M_log.info("*** Files has content for Course: "+course.getCourseId());
			return true;
		}
		if(areThereAssignments(course.getCourseId(),apiHandler)) {
			M_log.info("*** Assignments has content for Course: "+course.getCourseId());
			return true;
		}
		if(areThereAnnouncements(course.getCourseId(),apiHandler)) {
			M_log.info("*** Announcements has content for Course: "+course.getCourseId());
			return true;
		}
		if(areThereQuizzes(course.getCourseId(),apiHandler)) {
			M_log.info("*** Quizzes has content for Course: "+course.getCourseId());
			return true;
		} 
		if(areThereModules(course.getCourseId(),apiHandler)) {
			M_log.info("*** Modules has content for Course: "+course.getCourseId());
			return true;
		} 
		if(areThereGradeChanges(course.getCourseId(),apiHandler)) {
			M_log.info("*** GradeChanges has content for Course: "+course.getCourseId());
			return true;
		} 
		if(areThereConferences(course.getCourseId(),apiHandler)) {
			M_log.info("*** Conferences has content for Course: "+course.getCourseId());
			return true;
		} 
		if(areThereDiscussionTopics(course.getCourseId(),apiHandler)) {
			M_log.info("*** DiscussionTopics has content for Course: "+course.getCourseId());
			return true;
		} 
		if(areThereGroups(course.getCourseId(),apiHandler)) {
			M_log.info("*** Groups has content for Course: "+course.getCourseId());
			return true;
		} 
		if(areTherePages(course.getCourseId(),apiHandler)) {
			M_log.info("*** Pages has content for Course: "+course.getCourseId());
			return true;
		}
		if(areThereExternalToolsAdded(course.getCourseId(),apiHandler)) {
			M_log.info("*** ExternalTools has content for Course: "+course.getCourseId());
			return true;
		}
		return false;
	}

	private static boolean areThereAssignments(String courseId,ApiCallHandler apiHandler) {
		List<HashMap<String, Object>> assignmentRes = apiResponseTemplate(courseId, apiHandler,RequestTypeEnum.ASSIGNMENT);
		return checkResponseState(assignmentRes);
	}
	private static boolean areThereQuizzes(String courseId, ApiCallHandler apiHandler) {
		List<HashMap<String, Object>> quizzesRes = apiResponseTemplate(courseId, apiHandler,RequestTypeEnum.QUIZZES);
		return checkResponseState(quizzesRes);
	}
	private static boolean areThereAnnouncements(String courseId,ApiCallHandler apiHandler) {
		List<HashMap<String, Object>> announcementRes = apiResponseTemplate(courseId, apiHandler,RequestTypeEnum.ANNOUNCEMENT);
		return checkResponseState(announcementRes);
	}
	private static boolean areThereDiscussionTopics(String courseId, ApiCallHandler apiHandler) {
		List<HashMap<String, Object>> discussionsRes = apiResponseTemplate(courseId, apiHandler,RequestTypeEnum.DISCUSSION_TOPICS);
		return checkResponseState(discussionsRes);
	}
	private static boolean areThereFiles(String courseId, ApiCallHandler apiHandler) {
		List<HashMap<String, Object>> filesRes = apiResponseTemplate(courseId, apiHandler,RequestTypeEnum.FILES);
		return checkResponseState(filesRes);
	}
	private static boolean areThereGroups(String courseId, ApiCallHandler apiHandler) {
		List<HashMap<String, Object>> groupsRes = apiResponseTemplate(courseId, apiHandler,RequestTypeEnum.GROUPS);
		return checkResponseState(groupsRes);
	}
	private static boolean areThereModules(String courseId, ApiCallHandler apiHandler) {
		List<HashMap<String, Object>> modulesRes = apiResponseTemplate(courseId, apiHandler,RequestTypeEnum.MODULES);
		return checkResponseState(modulesRes);
	}
	private static boolean areTherePages(String courseId, ApiCallHandler apiHandler) {
		List<HashMap<String, Object>> pagesRes = apiResponseTemplate(courseId, apiHandler,RequestTypeEnum.PAGES);
		return checkResponseState(pagesRes);
	}
	private static boolean areThereExternalToolsAdded(String courseId, ApiCallHandler apiHandler) {
		List<HashMap<String, Object>> externalTools = apiResponseTemplate(courseId, apiHandler,RequestTypeEnum.EXTERNAL_TOOLS);
		return checkResponseState(externalTools);
	}
	private static boolean areThereGradeChanges(String courseId, ApiCallHandler apiHandler) {
		List<HashMap<String, Object>> gradeChanges = apiResponseTemplate(courseId, apiHandler,RequestTypeEnum.GRADE_CHANGES);
		return checkResponseState(gradeChanges);
	}
	private static boolean areThereConferences(String courseId, ApiCallHandler apiHandler) {
		List<HashMap<String, Object>> conferenceRes = apiResponseTemplate(courseId, apiHandler,RequestTypeEnum.CONFERENCE);
		return checkResponseState(conferenceRes);
	}


	private static List<HashMap<String,Object>> apiResponseTemplate(String courseId, ApiCallHandler apiHandler,RequestTypeEnum requestType) {
		HttpResponse httpResponse = apiHandler.getApiResponse(requestType, null, null, courseId);
		httpResponseNullCheck(httpResponse,requestType);
		List<HashMap<String, Object>> responseList=null;
		int statusCode = httpResponse.getStatusLine().getStatusCode();
		if(statusCode!=200) {
			M_log.error(apiCallErrorHandler(httpResponse,requestType,apiHandler));
			return responseList;
		}
		String jsonResponseString = null;
		ObjectMapper mapper = new ObjectMapper();
		HttpEntity entity = httpResponse.getEntity();
		try {
			jsonResponseString = EntityUtils.toString(entity);
			if(requestType.equals(RequestTypeEnum.CONFERENCE)) {
				responseList = responsePropertyLookUp(jsonResponseString, CONFERENCES);
				return responseList;
			}
			if(requestType.equals(RequestTypeEnum.GRADE_CHANGES)) {
				responseList = responsePropertyLookUp(jsonResponseString, EVENTS);
				return responseList;
			}
			responseList = mapper.readValue(jsonResponseString,new TypeReference<List<Object>>(){});
		} catch (JsonParseException e1) {
			M_log.error("JsonParseException occured apiResponseTemplate() : ",e1);
		} catch(JsonMappingException e1) {
			M_log.error("JsonMappingException occured apiResponseTemplate() : ",e1);
		} catch (ParseException | IOException e1) {
			M_log.error("Exception occured apiResponseTemplate() : ",e1);
		} 
		return responseList;
	}
	
	private static boolean checkResponseState(List<HashMap<String, Object>> response) {
		//response=null is a error condition so we don't want to delete the course just skip the course from the delete determination logic.  
		if(response==null || (!response.isEmpty())) {
			return true;
		}
		return false;
	}

	private static List<HashMap<String, Object>> responsePropertyLookUp(String jsonResponseString, String property)
			throws IOException, JsonParseException, JsonMappingException {
		ObjectMapper mapper = new ObjectMapper();
		List<HashMap<String, Object>> responseList;
		Map<String,Object> resMap = new HashMap<String,Object>();
		resMap = mapper.readValue(jsonResponseString,new TypeReference<HashMap<String,Object>>(){});
		responseList = (List<HashMap<String, Object>>) resMap.get(property);
		return responseList;
	}
    /*
     * This check for activity in a course we are looking for the "manually published" events in the past. Please note that the course we are checking this event
     * is an unpublished course. We don't want to delete "manually published" course in the past as this indicates that the course is still in interest to an instructor.  
     */
	private static boolean isThereActivityInCourse(Course course, ApiCallHandler apiHandler,String paginationUrl) {
		M_log.debug("isTheirActivityInCourse: "+course.getCourseId());
		HttpResponse httpResponse=null;
		if(paginationUrl==null) {
			httpResponse = apiHandler.getApiResponse(RequestTypeEnum.COURSE_AUDIT_LOGS, null, null, course.getCourseId());
		}else {
			httpResponse = apiHandler.getApiResponse(RequestTypeEnum.COURSE_AUDIT_LOGS_PAGINALTION_URL, null, paginationUrl, course.getCourseId());
		}
		httpResponseNullCheck(httpResponse,RequestTypeEnum.COURSE_AUDIT_LOGS);
		String jsonResponseString = null;
		List<HashMap<String, Object>> eventList=null;
		int statusCode = httpResponse.getStatusLine().getStatusCode();
		if(statusCode!=200) {
			M_log.error(apiCallErrorHandler(httpResponse,RequestTypeEnum.COURSE_AUDIT_LOGS,apiHandler));
			return true;
		}
		HttpEntity entity = httpResponse.getEntity();
		try {
			jsonResponseString = EntityUtils.toString(entity);
			eventList = responsePropertyLookUp(jsonResponseString, EVENTS);
			for (HashMap<String, Object> event : eventList) {
				String eventType = (String)event.get("event_type");
				String eventSource = (String) event.get("event_source");
				// This is a case where some time in the past this course got published so we do not want to delete the course.  
				if(eventType.equals(PUBLISHED)&&eventSource.equals(MANUAL)) {
					return true;
				}
			}
			String nextPageUrl = getNextPageUrl(httpResponse);
			if(nextPageUrl!=null) {
				isThereActivityInCourse(course,apiHandler,nextPageUrl);
			}
		} catch (JsonParseException e1) {
			M_log.error("JsonParseException occured isThereActivityInCourse() : ",e1);
		} catch(JsonMappingException e1) {
			M_log.error("JsonMappingException occured isThereActivityInCourse() : ",e1);
		} catch (ParseException | IOException e) {
			M_log.error("Exception occured isThereActivityInCourse() : ",e);
		} 
		return false;
	}
	
	/*
	 * This is actually deleting the course , we have 'delete.course' properties from properties file if true then course will be deleted from canvas instance,
	 * Set it to false for testing purposes
	 */
	private static void deleteTheCourse(Course course, ApiCallHandler apiHandler,CoursesForDelete coursesForDelete) {
		if(isCourseDeleteEnabled) {
			M_log.debug("courses enabled for delete");
			HttpResponse httpResponse = apiHandler.getApiResponse(RequestTypeEnum.COURSE_DELETE, null, null, course.getCourseId());
			httpResponseNullCheck(httpResponse,RequestTypeEnum.COURSE_DELETE);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			if(statusCode!=200) {
				M_log.error(apiCallErrorHandler(httpResponse,RequestTypeEnum.COURSE_DELETE,apiHandler));
				return;
			}
		}
		M_log.info("***** Course Deleted: "+course.getCourseId());	
		coursesForDelete.addDeletedCourse(course);
	}
	
	private static void sendAnEmailReport(ArrayList<Term> previousTerms, CoursesForDelete coursesForDelete) {
		M_log.debug("sendAnEmailReport(): Called");
		Properties properties = System.getProperties();
		properties.put(MAIL_SMTP_AUTH, "false");
		properties.put(MAIL_SMTP_STARTTLS, "true"); 
		properties.put(MAIL_SMTP_HOST, mailHost);
		properties.put(MAIL_DEBUG_PROPERTY, mailDebug); //true=debugEnabled

		Session session = Session.getInstance(properties);
		MimeMessage message = new MimeMessage(session);

		ArrayList<String> termsList = new ArrayList<String>();
		for (Term term : previousTerms) {
			termsList.add(term.getTermName());
		}
		try {
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmailAddress));
			message.setSubject(String.format(emailSubjectText,termsList));

			StringBuilder msgBody=new StringBuilder();
			msgBody.append("Total unpublished courses for terms ");
			msgBody.append(termsList.toString());
			msgBody.append(" =>");
			msgBody.append(coursesForDelete.getCourses().size());
			msgBody.append("\n");
			msgBody.append("Courses Deleted => ");
			msgBody.append(coursesForDelete.getDeletedUnpublishedCourses().size());

			Multipart multipart = new MimeMultipart();
			//msgBody of the email
			BodyPart msgBodyPart = new MimeBodyPart();
			msgBodyPart.setText(msgBody.toString());
			multipart.addBodyPart(msgBodyPart);
			//Attachment with deleted Course list
			msgBodyPart=new MimeBodyPart();
			msgBodyPart.setDataHandler(new DataHandler(new ByteArrayDataSource(generateCSVFileForDeletedCourses(coursesForDelete).getBytes(), TEXT_CSV)));
			msgBodyPart.setFileName(String.format(deletedCoursesFileName, termsList));
			multipart.addBodyPart(msgBodyPart);
			//Attachment with CourseWithContentList
			msgBodyPart=new MimeBodyPart();
			msgBodyPart.setDataHandler(new DataHandler(new ByteArrayDataSource(generateCSVFileForContentCourses(getCoursesWithContentList(coursesForDelete)).getBytes(), TEXT_CSV)));
			msgBodyPart.setFileName(String.format(contentCoursesFileName, termsList));
			multipart.addBodyPart(msgBodyPart);

			message.setContent(multipart);
			M_log.info("Sending mail...");
			Transport.send(message);
		} catch(MessagingException e) {
			M_log.error("Email notification failed, number of courses deleted for terms: "
					+termsList+" are: "+coursesForDelete.getDeletedUnpublishedCourses().size()+"/"+coursesForDelete.getCourses().size(),e);
		}
	}


	private static ArrayList<Course> getCoursesWithContentList(CoursesForDelete coursesForDelete) {
		M_log.debug("getCoursesWithContentList(): Called");
		ArrayList<Course> totalUnpublishedCourses = coursesForDelete.getCourses();
		ArrayList<Course> deletedUnpublishedCourses = coursesForDelete.getDeletedUnpublishedCourses();
		ArrayList<Course> coursesWithContent = new ArrayList<Course>(totalUnpublishedCourses);
		coursesWithContent.removeAll(deletedUnpublishedCourses);
		return coursesWithContent;
	}
	
	private static String generateCSVFileForDeletedCourses(CoursesForDelete coursesForDelete) {
		M_log.debug("generateCSVFileForDeletedCourses(): Called");
		StringBuilder csv = new StringBuilder();
		ArrayList<Course> deletedCourseList = coursesForDelete.getDeletedUnpublishedCourses();
		csv.append(Course.getCourseHeader());
		for (Course course : deletedCourseList) {
			csv.append(course.getCourseValues());
		}
		return csv.toString();
	}
	
	private static String generateCSVFileForContentCourses(ArrayList<Course> coursesWithContentList) {
		M_log.debug("generateCSVFileForContentCourses(): Called");
		StringBuilder csv = new StringBuilder();
		csv.append(Course.getCourseHeader());
		for (Course course : coursesWithContentList) {
			csv.append(course.getCourseValues());
		}
		return csv.toString();
	}

	private static void httpResponseNullCheck(HttpResponse httpResponse, RequestTypeEnum requestType) {
		if(httpResponse==null) {
			M_log.error("Api call "+requestType+" is not successful");
			System.exit(1);
		}
	}

	/*
	 * This helper method pull out the error message that is sent in case of error. Currently code distinguishes error
	 * differently from directCanvas vs esbCanvas. This may not be always the case but ESB has Special Throttling message
	 * than canvas
	 * 
	 * return errMsg=Api call for getting "Enrollments_Terms" has some errors with status code: 401: < Invalid access token. >
	 */
	private static String apiCallErrorHandler(HttpResponse httpResponse, RequestTypeEnum apiText, ApiCallHandler apiHandler) {
		HttpEntity entity = httpResponse.getEntity();
		String jsonErrRes = null;
		String errMsg=null;
		try {
			jsonErrRes = EntityUtils.toString(entity);
		} catch (ParseException | IOException e) {
			M_log.error("Exception occured apiCallErrorHandler() : ",e);
		} 
		String errMsgWithStatus="Api call for getting \""+apiText+"\" has some errors with status code: "+httpResponse.getStatusLine().getStatusCode()+": < ";
		if(apiHandler.getCanvasCall().equals(CanvasCallEnum.API_DIRECT_CANVAS)) {
			errMsg=canvasDirectErrorResponse(jsonErrRes,errMsgWithStatus);
		}else if(apiHandler.getCanvasCall().equals(CanvasCallEnum.API_ESB_CANVAS)) {
			errMsg=canvasEsbErrorResponse(jsonErrRes,errMsgWithStatus);
		}
		return errMsg;
	}

	/*
	 * { "errors": [ { "message": "Invalid access token." } ],
	 * "error_report_id": 545140 }
	 */
	private static String canvasDirectErrorResponse(String jsonErrRes, String apiText) {
		StringBuilder errMsg = new StringBuilder();
		errMsg.append(apiText);
		ObjectMapper mapper = new ObjectMapper();
		Map<String,Object> errorResponse = new HashMap<String,Object>();
		try {
			errorResponse = mapper.readValue(jsonErrRes,new TypeReference<HashMap<String,Object>>(){});
			ArrayList<HashMap<String, String>> error = (ArrayList<HashMap<String, String>>) errorResponse.get("errors");
			for (HashMap<String, String> hashMap : error) {
				errMsg.append((String)hashMap.get("message")+" >");
			}
		} catch (JsonParseException e) {
			M_log.error("JsonParseException occured canvasDirectErrorResponse() : ",e);
		} catch (JsonMappingException e) {
			M_log.error("JsonMappingException occured canvasDirectErrorResponse() : ",e);
		} catch (IOException e) {
			M_log.error("IOException occured canvasDirectErrorResponse() : ",e);
		}
		return errMsg.toString();
	}
	
	private static String canvasEsbErrorResponse(String jsonErrRes,String apiText) {
		//Stub: to be implemented later when ESB to canvas call is ready
		return null;
	}
	
	/* The pagination 'Link' sample from the header 
     * Link → <https://umich.test.instructure.com/api/v1/courses/26164/enrollments?type=TeacherEnrollment&page=1&per_page=100>; rel="current",
     * <https://umich.test.instructure.com/api/v1/courses/26164/enrollments?type=TeacherEnrollment&page=1&per_page=100>; rel="next",
     * <https://umich.test.instructure.com/api/v1/courses/26164/enrollments?type=TeacherEnrollment&page=1&per_page=100>; rel="first",
     * <https://umich.test.instructure.com/api/v1/courses/26164/enrollments?type=TeacherEnrollment&page=1&per_page=100>; rel="last"
     */
	protected static String getNextPageUrl(HttpResponse response) {
		M_log.debug("getNextPageUrl(): Called");
		String result = null;
		if (!response.containsHeader("Link")) {
			return result;
		}
		Header[] linkHeaders = response.getHeaders("Link");
		Header linkHeader = linkHeaders[0];
		M_log.debug("Http response contains the following Link headers: " + linkHeader.getValue());
		// look for the 'rel='next'' header value
		String[] links = linkHeader.getValue().split(",");
		for (int i = 0; i < links.length; i++) {
			String[] linkPart = links[i].split(";");
			if (linkPart[1].indexOf("rel=\"next\"") > 0) {
				result = linkPart[0].trim();
				break;
			}
		}
		if (result != null) {
			if (result.startsWith("<")) {
				result = result.substring(1, result.length() - 1);
			}
		}
		M_log.debug("Returning next page header as: " + (result != null ? result : "NONE"));
		return result;
	}
	
	

	

}
