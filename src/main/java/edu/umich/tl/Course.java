package edu.umich.tl;

public class Course {
	private int id;
	private String courseName;
	private String startDate;
	private String endDate;
	
	public int getId() {
		return id;
	}
	public Course setId(int id) {
		this.id = id;
		return this;
	}
	public String getCourseName() {
		return courseName;
	}
	public Course setCourseName(String courseName) {
		this.courseName = courseName;
		return this;
	}
	public String getStartDate() {
		return startDate;
	}
	public Course setStartDate(String startDate) {
		this.startDate = startDate;
		return this;
	}
	public String getEndDate() {
		return endDate;
	}
	public Course setEndDate(String endDate) {
		this.endDate = endDate;
		return this;
	}

}
