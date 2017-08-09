package net.mtabuscis.alerts.scraping;

public enum LanguageCodes {
	English("EN"); 
	//Spanish("ES");
	
	private String langCode;
	
	private LanguageCodes (String langCode) {
		this.langCode = langCode;
	}
	
	public String getCode() {
		return this.langCode;
	}
}
