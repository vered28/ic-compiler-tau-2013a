package IC.Parser;

/**
 * Exception class for lexical errors, thrown by Lexer in case of a lexical error.
 * This class contains exception line number and error message fields.
 * 
 */

public class LexicalError extends Exception
{
	private int line_num;
    private String err_msg;
    
    private static final long serialVersionUID = 42L;  /* impl. serializable. */ 
    
    
    public LexicalError(String message) {
    	this.err_msg=new String(message);
    }
    
    
    public LexicalError(String message, int line) {
    	super(line + ": " + message);   /* error line and error message. */
    	this.line_num=line;
     	this.err_msg=new String(message);
    }
    
    
    public int getLineNum() {
    	return this.line_num;
    }
    
    public String getErrMsg() {
    	return this.err_msg;
    }
    
   
    @Override
    public String toString(){
        return super.getMessage();
    }
    
}

