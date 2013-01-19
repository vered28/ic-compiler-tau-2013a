package IC.LIR;

/**
 * Up type for the translating visitor
 */
public class LIRUpType {

	private String	LIRCode;
	private LIRFlagEnum LIRInstType;
	private String targetRegister;

	/**
	 * constructor for the up-type
	 * @param lIRCode: representation of the LIR code for the current node
	 * @param astType: the return type of AST node translation
	 * @param targetRegister: the address (in registers) for the current node
	 */
	public LIRUpType(String lIRCode, LIRFlagEnum astType, String targetRegister) {
		super();
		this.LIRCode = lIRCode;
		this.LIRInstType = astType;
		this.targetRegister = targetRegister;
	}

	public String getLIRCode() {
		return LIRCode;
	}

	public LIRFlagEnum getLIRInstType() {
		return LIRInstType;
	}
	
	public String getTargetRegister() {
		return targetRegister;
	}
	
	public void setTargetRegister(String targetRegister) {
		this.targetRegister = targetRegister;
	}

	public void setLIRInstType(LIRFlagEnum lIRInstType) {
		LIRInstType = lIRInstType;
	}

}
