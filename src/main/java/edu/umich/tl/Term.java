package edu.umich.tl;

import java.util.Date;

public class Term {
	Date endDate;
	Date startDate;
	String canvasTermsId;
	String termName;
	String sisTermId;
	public String getCanvasTermId() {
		return canvasTermsId;
	}
	public void setCanvasTermId(String canvasTermId) {
		this.canvasTermsId = canvasTermId;
	}
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	public Date getEndDate() {
		return endDate;
	}
	public Date getStartDate() {
		return startDate;
	}
	public String getTermName() {
		return termName;
	}
	public void setTermName(String termName) {
		this.termName = termName;
	}
	public String getSisTermId() {
		return sisTermId;
	}
	public void setSisTermId(String sisTermId) {
		this.sisTermId = sisTermId;
	}
	
	

}
