package IC.TypeTable;

/**
 * Exception class for semantic errors, thrown by Visitors in case of a semantic error.
 * This exception class contains line number, the token that caused the error
 * and error message.
 * 
 */

public class SemanticError extends Exception {
	
	private int line_number;
    private String tok;
        
    private static final long serialVersionUID = 44L;  /* impl. serializable. */ 
        
  
    public SemanticError(String message, int line, String token) {
    	super(message);
        this.line_number = line;
        this.tok = token;
    }
        
    public SemanticError(String message, String token) {
    	this(message, -1, token);
    }
          
     
    public void setLine(int line) {
    	this.line_number = line;
    }
    
    public int getLine() {
    	return this.line_number;
    }
    
    
    /**
     * Returns error string message with line number, message and the token that
     * caused the semantic error.
     */
    @Override
    public String toString() {
    	 return ("semantic error at line " + this.line_number + 
    			 	": " + super.getMessage() + ": " + this.tok);
    } 

}
