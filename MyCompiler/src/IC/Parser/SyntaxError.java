package IC.Parser;

/**
 * Exception class for syntax errors, thrown by Parser in case of a syntax error.
 * This exception class contains line number, the token that caused the error
 * and error message.
 * 
 */

public class SyntaxError extends Exception {
	

    private int line_number;
    private String tok;
    
    private static final long serialVersionUID = 43L;  /* impl. serializable. */ 
    
    
    public SyntaxError(int line, String value) {
        super();
        this.line_number=line;
        this.tok=new String(value);
    }
    
    
    public int getLineNum() {
    	return this.line_number;
    }
    
    public String getTok() {
    	return this.tok;
    }
    
    
    /**
     * Returns error string message with line number and the token that
     * caused the syntax error.
     */
    @Override
    public String toString() {
        return (this.line_number + ": Syntax error: at token " + this.tok);
    }
	
    
}
