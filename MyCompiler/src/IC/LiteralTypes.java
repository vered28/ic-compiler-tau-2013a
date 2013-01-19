package IC;

/**
 * Enum of the IC language's literal value types. Includes methods for creating
 * a string representation of each type of value.
 * 
 * @author Tovi Almozlino
 */
public enum LiteralTypes {

	INTEGER(DataTypes.INT.getDefaultValue(), "Integer literal"), 
	STRING(DataTypes.STRING.getDefaultValue(), "String literal"), /* we've removed from here unneeded str. formatting */
	TRUE(true, "Boolean literal"),
	FALSE(false, "Boolean literal"),
	NULL(null, "Literal");
	
	private Object value;
	
	private String description;

	private LiteralTypes(Object value, String description) {
		this.value = value;
		this.description = description;
	}

	/**
	 * Returns the intrinsic value of the literal.
	 * 
	 * @return The value.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Returns a formatted string representation of a literal value.
	 * 
	 * @param value
	 *            The value.
	 * @return The string.
	 */
	public String toFormattedString(Object value) {
		return String.valueOf(value);
	}

	/**
	 * Returns a description of the literal type.
	 * 
	 * @return The description.
	 */
	public String getDescription() {
		return description;
	}	
}