package IC.Parser;

/**
 * The lexical analyzer for the IC language.
 * This specification will be the input of JFlex.
 *
 */
 
 
%%

%class Lexer
%public
%function next_token
%type Token
%cup
%line
%scanerror LexicalError


%{
      StringBuffer string = new StringBuffer(); /* to save strings' values. */
     
     /* to save comment and str. init. line, for lexical errors. */
      private int comm_init_line=0; 
      private int str_init_line=0;
      
      /* help functions for tokens return. */
      
      private Token token(int id) {
		  return new Token(id, yyline+1);  /* as counting starts from 0 and we want from 1. */
	  }
		
		
	  private Token token(int id, Object value) {
		  return new Token(id, yyline+1, value);
	  }
%}


%eofval{

	if (yystate() == STRING)   /* EOF inside string. */
		throw new LexicalError("Lexical error: Unclosed string at end of file.",str_init_line+1);
		
 	if (yystate() == REGULAR_COMMENT)  /* EOF inside comment. */
 		throw new LexicalError("Lexical error: Unclosed comment at end of file.",comm_init_line+1);
 		 
  	return token(sym.EOF,"EOF");
  	
%eofval}


%state STRING
%state INLINE_COMMENT
%state REGULAR_COMMENT


/* Macro definitions. */

LCASE=[a-z]
UCASE=[A-Z]
ALPHA=[A-Za-z_]   
DIGIT=[0-9]   
ALPHA_NUMERIC={ALPHA}|{DIGIT}

IDENT={LCASE}({ALPHA_NUMERIC})*
CLASS_IDENT={UCASE}({ALPHA_NUMERIC})*
NUMBER=([0]+|[1-9]({DIGIT})*)
LEADING_ZEROS=([0]+)([1-9]+)({DIGIT})*  /* non-zero integer with leading zeros. */ 

WHITESPACE=[ \t\r\n]


%%

<YYINITIAL> {

		/* parentheses. */
		"(" 			{ return token(sym.LP,yytext()); }
		")" 			{ return token(sym.RP,yytext()); }
		"[" 			{ return token(sym.LB,yytext()); }
		"]" 			{ return token(sym.RB,yytext()); }
		"{" 			{ return token(sym.LCBR,yytext()); }
		"}" 			{ return token(sym.RCBR,yytext()); }
		
		/* comparison operators. */
		"=" 			{ return token(sym.ASSIGN,yytext()); }
		"==" 			{ return token(sym.EQUAL,yytext()); }
		"!=" 			{ return token(sym.NEQUAL,yytext()); }
		"<" 			{ return token(sym.LT,yytext()); }
		"<=" 			{ return token(sym.LTE,yytext()); }
		">" 			{ return token(sym.GT,yytext()); }
		">=" 			{ return token(sym.GTE,yytext()); }
		
		/* boolean operators. */
		"!" 			{ return token(sym.LNEG,yytext()); }
		"||" 			{ return token(sym.LOR,yytext()); }
		"&&" 			{ return token(sym.LAND,yytext()); }
		"true" 			{ return token(sym.TRUE,yytext()); }
		"false" 		{ return token(sym.FALSE,yytext()); }
		
		/* arithmetic operators. */
		"+" 			{ return token(sym.PLUS,yytext()); }
		"-" 			{ return token(sym.MINUS,yytext()); }
		"*" 			{ return token(sym.MULTIPLY,yytext()); }
		"/" 			{ return token(sym.DIVIDE,yytext()); }
		"%" 			{ return token(sym.MOD,yytext()); }
		
		/* punctuation signs. */
		"," 			{ return token(sym.COMMA,yytext()); }
		";" 			{ return token(sym.SEMI,yytext()); }
		"." 			{ return token(sym.DOT,yytext()); }
		
		/* variables' types. */
		"static" 		{ return token(sym.STATIC,yytext()); }
		"int" 			{ return token(sym.INT,yytext()); }
		"boolean" 		{ return token(sym.BOOLEAN,yytext()); }
		"void" 			{ return token(sym.VOID,yytext()); }
		"string" 		{ return token(sym.STRING,yytext()); }
		"null" 			{ return token(sym.NULL,yytext()); }
		"this" 			{ return token(sym.THIS,yytext()); }
		
		/* loops' stuff. */
		"while" 		{ return token(sym.WHILE,yytext()); }
		"break" 		{ return token(sym.BREAK,yytext()); }
		"continue" 		{ return token(sym.CONTINUE,yytext()); }
		"return" 		{ return token(sym.RETURN,yytext()); }
		
		/* misc. */
		"if" 			{ return token(sym.IF,yytext()); }
		"else" 			{ return token(sym.ELSE,yytext()); }
		
		"new" 			{ return token(sym.NEW,yytext()); }
		"length" 		{ return token(sym.LENGTH,yytext()); }
		"class" 		{ return token(sym.CLASS,yytext()); }
		"extends" 		{ return token(sym.EXTENDS,yytext()); }
		
		{WHITESPACE} 	{ /* ignore. */ }
		
		{CLASS_IDENT} 	{ return token(sym.CLASS_ID,yytext()); }
		{IDENT} 		{ return token(sym.ID,yytext()); }
		
		{NUMBER}        { return token(sym.INTEGER, yytext()); }
		
		{LEADING_ZEROS} { throw new LexicalError("Lexical error: integer should not have leading zeros.",yyline+1); }
		
		/* string. */
		"\"" 			{ str_init_line=yyline; string.setLength(0); string.append('\"'); yybegin(STRING); }
		
		/* comments. */
		"//"			{ yybegin(INLINE_COMMENT); }
		"/*"			{ comm_init_line=yyline; yybegin(REGULAR_COMMENT); }
		
		
		/* all the other chars (non of the above). */
		.               { throw new LexicalError("Lexical error: illegal character '" + yytext() + "'",yyline+1); }
				
}
		
	
	
<STRING> {

		/* end of string. */    
		"\"" 			{ yybegin(YYINITIAL); 
						  string.append('\"'); 
		       			  return token(sym.QUOTE,string.toString()); }

		[ !#-\[\]-~]+ 	{ string.append(yytext()); }  /* ASCII chars 32-126 */
		
		/* newline in code file. */
		"\n"            { throw new LexicalError("Lexical error: Unclosed string at end of line.",str_init_line+1); }
		 
		"\\t" 			{ string.append("\\t"); }
		"\\n" 			{ string.append("\\n"); }
		"\\\"" 			{ string.append("\\\""); }
		"\\\\" 			{ string.append("\\\\"); }
		
		"\\"[^ tn\"\\]  { throw new LexicalError("Lexical error: illegal escape sequence inside of a string: '" + yytext() + "'",yyline+1); }
		
		[^ !#-\[\]-~]   { throw new LexicalError("Lexical error: illegal character inside of a string: '" + yytext() + "'",yyline+1); }
			                  
}


<INLINE_COMMENT> {

	[^\n] 	{ /* ignore. */ }
	[\n]  	{ yybegin(YYINITIAL); }
}


<REGULAR_COMMENT> {

	"*/"  	{ yybegin(YYINITIAL); }
	[^*] 	{ /* ignore. */ }
	"*"     { /* ignore. */ }

}



