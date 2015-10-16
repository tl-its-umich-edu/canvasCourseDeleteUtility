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
	
	public static String getCourseHeader() {
		StringBuilder courseHeader=new StringBuilder();
		courseHeader.append("COURSE_NAME");
		courseHeader.append(',');
		courseHeader.append("COURSE_ID");
		courseHeader.append('\n');
		return courseHeader.toString();
	}
	public String getCourseValues() {
		StringBuilder courseValues=new StringBuilder();
		courseValues.append("\""+getCourseName()+"\"");
		courseValues.append(',');
		courseValues.append(getCourseId());
		courseValues.append('\n');
		return courseValues.toString();
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((courseId == null) ? 0 : courseId.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Course other = (Course) obj;
		if (courseId == null) {
			if (other.courseId != null)
				return false;
		} else if (!courseId.equals(other.courseId))
			return false;
		return true;
	}

}
