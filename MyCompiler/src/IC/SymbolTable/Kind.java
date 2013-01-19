package IC.SymbolTable;

/**
 * Enum that holds the four symbol kinds.
 *
 */
public enum Kind {
	
	VARIABLE("variable"), 
	FIELD("field"), 
	METHOD("method"), 
	CLASS("class");

	private String theKind;

	private Kind(String k) {
		this.theKind = k;
	}
	    
	public String getKindName() {
		return this.theKind;
	}

}
