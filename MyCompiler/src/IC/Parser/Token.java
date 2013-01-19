package IC.Parser;

import java_cup.runtime.Symbol;

/**
 * This class holds info. for single token, which includes: 
 * token line number, id (from sym.java) and value (if has one).
 */

public class Token extends Symbol {
	
	private int line_num;   
	
	
    public Token(int id, int line) {
        super(id, null); 
        this.line_num=line;
        super.left=line;     /* to get line number in cup files */
    }
    
    public Token(int id, int line, Object val) {
        super(id, val);  
        this.line_num=line;
        super.left=line;     /* to get line number in cup files */
    }
    
    
    public int getId() { 
    	return super.sym;
  
    }
    
    public Object getValue() { 
    	return super.value;
    }
    
    public int getLine() {  
    	return this.line_num;
    }
    
    
    @Override
    public String toString() {    /* making token output string, by given format. */
    	
    	String token_output = this.line_num + ": "; 
    	String token_name="";
    	
    	switch(super.sym) {
    		case IC.Parser.sym.LP: 
    			token_name = "LP";
    			break;
			case IC.Parser.sym.RP:
				token_name = "RP";
				break;
			case IC.Parser.sym.ASSIGN:
				token_name = "ASSIGN";
				break;
			case IC.Parser.sym.BOOLEAN:
				token_name = "BOOLEAN";
				break;
			case IC.Parser.sym.BREAK:
				token_name = "BREAK";
				break;
			case IC.Parser.sym.CLASS:
				token_name = "CLASS";
				break;
			case IC.Parser.sym.CLASS_ID:
				token_name = "CLASS_ID";
				break;
			case IC.Parser.sym.COMMA:
				token_name = "COMMA";
				break;
			case IC.Parser.sym.CONTINUE:
				token_name = "CONTINUE";
				break;
			case IC.Parser.sym.DIVIDE:
				token_name = "DIVIDE";
				break;
			case IC.Parser.sym.DOT:
				token_name = "DOT";
				break;
			case IC.Parser.sym.EQUAL:
				token_name = "EQUAL";
				break;
			case IC.Parser.sym.EXTENDS:
				token_name = "EXTENDS";
				break;
			case IC.Parser.sym.ELSE:
				token_name = "ELSE";
				break;
			case IC.Parser.sym.FALSE:
				token_name = "FALSE";
				break;
			case IC.Parser.sym.GT:
				token_name = "GT";
				break;
			case IC.Parser.sym.GTE:
				token_name = "GTE";
				break;
			case IC.Parser.sym.ID:
				token_name = "ID";
				break;
			case IC.Parser.sym.IF:
				token_name = "IF";
				break;
			case IC.Parser.sym.INT:
				token_name = "INT";
				break;
			case IC.Parser.sym.INTEGER:
				token_name = "INTEGER";
				break;
			case IC.Parser.sym.LAND:
				token_name = "LAND";
				break;
			case IC.Parser.sym.LB:
				token_name = "LB";
				break;
			case IC.Parser.sym.LCBR:
				token_name = "LCBR";
				break;
			case IC.Parser.sym.LENGTH:
				token_name = "LENGTH";
				break;
			case IC.Parser.sym.NEW:
				token_name = "NEW";
				break;
			case IC.Parser.sym.LNEG:
				token_name = "LNEG";
				break;
			case IC.Parser.sym.LOR:
				token_name = "LOR";
				break;
			case IC.Parser.sym.LT:
				token_name = "LT";
				break;
			case IC.Parser.sym.LTE:
				token_name = "LTE";
				break;
			case IC.Parser.sym.MINUS:
				token_name = "MINUS";
				break;
			case IC.Parser.sym.MOD:
				token_name = "MOD";
				break;
			case IC.Parser.sym.MULTIPLY:
				token_name = "MULTIPLY";
				break;
			case IC.Parser.sym.NEQUAL:
				token_name = "NEQUAL";
				break;
			case IC.Parser.sym.NULL:
				token_name = "NULL";
				break;
			case IC.Parser.sym.PLUS:
				token_name = "PLUS";
				break;
			case IC.Parser.sym.RB:
				token_name = "RB";
				break;
			case IC.Parser.sym.RCBR:
				token_name = "RCBR";
				break;
			case IC.Parser.sym.RETURN:
				token_name = "RETURN";
				break;
			case IC.Parser.sym.SEMI:
				token_name = "SEMI";
				break;
			case IC.Parser.sym.STATIC:
				token_name = "STATIC";
				break;
			case IC.Parser.sym.STRING:
				token_name = "STRING";
				break;
			case IC.Parser.sym.QUOTE:
				token_name = "QUOTE";
				break;
			case IC.Parser.sym.THIS:
				token_name = "THIS";
				break;
			case IC.Parser.sym.TRUE:
				token_name = "TRUE";
				break;
			case IC.Parser.sym.VOID:
				token_name = "VOID";
				break;
			case IC.Parser.sym.WHILE:
				token_name = "WHILE";
				break;
			case IC.Parser.sym.EOF:
				token_name = "EOF";
				break;
    			
    		default: 
    			token_name = "Unknown token."; /* should never get here. */
    	}
    	
    	
    	token_output = token_output + token_name;
    	
    	/* token that has value. */
    	if ( (super.sym == IC.Parser.sym.ID) || (super.sym == IC.Parser.sym.CLASS_ID) || 
    			(super.sym == IC.Parser.sym.INTEGER) || (super.sym == IC.Parser.sym.QUOTE) ) {
    		
    		token_output = token_output + "(" + this.value.toString() + ")";
    	}
    	
    	return token_output;
    }

    
}

