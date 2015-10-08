package edu.umich.tl;


public class Term {
	private String canvasTermsId;
	private String termName;
	private String sisTermId;
	
	public String getCanvasTermId() {
		return canvasTermsId;
	}
	public Term setCanvasTermId(String canvasTermId) {
		this.canvasTermsId = canvasTermId;
		return this;
	}
	public String getTermName() {
		return termName;
	}
	public Term setTermName(String termName) {
		this.termName = termName;
		return this;
	}
	public String getSisTermId() {
		return sisTermId;
	}
	public Term setSisTermId(String sisTermId) {
		this.sisTermId = sisTermId;
		return this;
	}
	
	

}
