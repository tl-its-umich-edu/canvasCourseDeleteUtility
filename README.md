# canvasCourseDeleteUtility
One of the dangers with an auto-provisioning approach is the proliferation of unused course shells. One approach to solving this is deleting course shells at the end of each semester, when it can be deemed that they are unused. The workflow should be 

1. Check a course End date is set after the term end date, in that cases we don't want to delete the course. 
2. if the Course end date is past, check to see if there is content in the course. If there is content of any type that we have access to, do not delete the course. 
3. If there is no content, check the course audit log to see if the course has ever been published manually. 'Published' events from the 'sis' event_source are to be expected for every course, so should not be used as criteria that the course has ever been used. If the course has never been published and has no content, delete the course. 
4. The Utility will send out an email with 2 csv file as attachment containing report of unpublished courses that got deleted and the unpublished course that has content.


## Build Directions

1. CourseDeleteUtility$ `mvn clean install`
2. Add the following properties to `canvasCourseDelete.properties`. 
  * There can be many `term` property like `term1, term2...termN` and should be numerically aligned. The value to the term property formatted as `MM/dd/yyy`
  * The `term.count` property sets the number of term property lines that will be read from the file. This property should match the number of term properties defined.
    
    ```
    canvas.token=canvas token  
    canvas.url=target canvas server e.g. https://umich.test.instructure.com  
    use.test.url=true  
    esb.url=esb server e.g. https://api-qa.its.umich.edu:9443/store/  
    api.call.type=1 or 2  1 means canvas call 2 means canvas call via ESB
    term1=1990;08/12/2014
    term2=2000;08/12/2014
    .....
    termN=2010;01/04/2015
    term.count=N
    canvas.course.delete.mailhost=
    course.delete.report.send.emailaddress=
    # This property controls in real life if you want to delete the courses in a particular environment or not. False indicated not to delete the courses, and may be for testing purposes.
    delete.course=false
    mail.debug=true
    ```
    
    
   
## Run Directions
1. The `run.sh` is used for running the utility and is checked in to the source code. Follow the instruction that is given in the `run.sh` to run the utility successfully. `./run.sh`
2. The delete process may take long time so it is good idea to run it in the background so that it continues after logging out from a machine. So use   below while running on Linux servers.

    `nohup ./run.sh > /path-to-file/logFile.log &` 

## log4j.properties file
1. The logs can be rolled to console or to a file or to both. It might slow down the process if logged to console, if a delete process takes long time it is best to roll the logs to a file. Below configuration is logging to both File and Console.

  	 ```
    log4j.rootLogger=INFO, A1, rollingFile
    #Logging to console
	log4j.appender.A1=org.apache.log4j.ConsoleAppender
	log4j.appender.A1.layout=org.apache.log4j.PatternLayout
	log4j.appender.A1.layout.ConversionPattern=%d [%t] %-5p %c - %m%n
	#Logging to file
	log4j.appender.rollingFile=org.apache.log4j.RollingFileAppender
	log4j.appender.rollingFile.File=<path-to-the file>/ccdulog.log
	log4j.appender.rollingFile.MaxBackupIndex=20
	log4j.appender.rollingFile.layout = org.apache.log4j.PatternLayout
	log4j.appender.rollingFile.layout.ConversionPattern=%d [%t] %-5p %c - %m%n
	log4j.logger.edu.umich.tl=INFO
    ```
  
 