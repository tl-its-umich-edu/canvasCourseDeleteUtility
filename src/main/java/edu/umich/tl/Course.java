package edu.umich.tl;

import org.joda.time.DateTime;

public class Course {
	private String courseId;
	private String courseName;
	private DateTime startDate;
	private DateTime endDate;
	
	public String getCourseId() {
		return courseId;
	}
	public Course setCourseId(String courseId) {
		this.courseId = courseId;
		return this;
	}
	public String getCourseName() {
		return courseName;
	}
	public Course setCourseName(String courseName) {
		this.courseName = courseName;
		return this;
	}
	public DateTime getStartDate() {
		return startDate;
	}
	public Course setStartDate(DateTime startDate) {
		this.startDate = startDate;
		return this;
	}
	public DateTime getEndDate() {
		return endDate;
	}
	public Course setEndDate(DateTime endDate) {
		this.endDate = endDate;
		return this;
	}

}
