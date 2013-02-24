package IC;

import IC.*;    
import IC.LIR.TranslatePropagatingVisitor;
import IC.LIR.OptTranslatePropagatingVisitor;
import IC.LIR.RegCounterVisitor;
import IC.Parser.*;
import IC.AST.*; 
import IC.SemanticAnalysis.SemanticChecks;
import IC.SemanticAnalysis.SymbolTableBuilder;
import IC.SymbolTable.GlobalSymbolTable;
import IC.TypeTable.TypeTable;
 
import java.io.*;

import java_cup.runtime.*;


/**
 * The compiler class, with main function.
 * 
 */

public class Compiler {
    
	private static boolean libic_flag=false;
	private static boolean icfile_flag=false;
	private static boolean printast_flag=false;
	private static boolean symtab_flag=false;
	private static boolean printlir_flag=false;
	private static boolean optlir_flag=false;
	
    
	/** 
     * Reads an IC program, and performs lexical analysis and parsing (+ builds an AST), 
     * and checks for lexical, syntactic and semantic errors.
     * Finally, parses translates to LIR code. 
     * 
     * Optional parameters: library file add, -print-ast commant for AST and printing.
     * @param: input IC program code file path.
     * @param optional: -L<library_path>, path to IC library signature (not a default one). 
     * @param optional: -print-ast, to pretty-print the AST.
     * @param optional: -dump-symtab, to print symbol tables and type table.
     * @param optional: -print-lir to print the LIR translation of the IC code.
     * @param optional: -opt-lir to translate the LIR code with optimizations.
     */
	public static void main(String[] args) {
		
		String libic_path = "libic.sig";  //curr. dir. path - default dir. of library
		String ic_code_path="";
		
		//input check.
		
        if (args.length == 0) {
        	System.out.println("Error: Missing ic input file argument.");
        	System.exit(1);
        }
        
        if (args.length > 4) {
        	System.out.println("Error: Too much arguments.");
        	System.exit(1);
        }
        
        for (int i=0; i<args.length; i++) {
        	
        	String s = args[i];
        	
        	if (s.startsWith("-L")) {   //library class path is given (not a default)
        
                if (libic_flag) {   //already given library path earlier
                	System.out.println("Error: Library path is given more than once.");
                    System.exit(1);
                } else {
                	libic_flag = true; 
                	libic_path = s.substring(2);  //given lib. path
                	continue; 
                }
        	}
        	
        	if (s.equals("-print-ast")) {   //-print-ast is requested
                
                if (printast_flag) {   //already requested -print-ast earlier
                	System.out.println("Error: -print-ast is given more than once.");
                    System.exit(1);
                } else {
                	printast_flag = true;
                	continue;
                }
        	}
        	
        	if (s.equals("-dump-symtab")) {   //-dump-symtab is requested
                
                if (symtab_flag) {   //already requested -dump-symtab earlier
                	System.out.println("Error: -dump-symtab is given more than once.");
                    System.exit(1);
                } else {
                	symtab_flag = true;
                	continue;
                }
        	}
        	
        	if (s.equals("-print-lir")) { //-print-lir requested
        		
        		if (printlir_flag) { //already requested -print-lir earlier
        			System.out.println("Error: -print-lir is given more than once.");
        			System.exit(1);
        		} else {
        			printlir_flag = true;
        			continue;
        		}
        	}
        	
        	if (s.equals("-opt-lir")) { //-opt-lir requested
        		
        		if (optlir_flag){ //already requested -opt-lir earlier
        			System.out.println("Error: -opt-lir is given more than once.");
        			System.exit(1);
        		} else {
        			optlir_flag = true;
        			continue;
        		}
        	}
        	
            
        	//if we've reached here, the param. is ic file path 
            if (icfile_flag) {   //ic file was already given
            	System.out.println("Error: ic file path is given more than once.");
            	System.exit(1);
            } else {
            	icfile_flag = true;
            	ic_code_path=new String(s);   //ic file path
            }
            
        	
        } //end of for.
        
        
        //no path to ic file was given
        if (icfile_flag == false) {
        	System.out.println("Error: Missing ic input file argument.");
        	System.exit(1);
        
        }

    
        /* end of input check. */
        
        //--------------------------------------------
        
        /* lexical and syntax parsing + semantic analysis */
        
        //Parsing the input ic file
       
        Symbol parseSym = new Symbol(1);  //init. to avoid errors.
        
        try {
        	
        	//reader to input code file.
            FileReader codeFile = new FileReader(ic_code_path); //can throw io exception
                
            Lexer scanner = new Lexer(codeFile);
            Parser parser = new Parser(scanner);
       
            parseSym = parser.parse();    //can throw LexicalError or SyntaxError
 
            
        } catch (IOException e1) {  //problem with input file 
	   
	            System.out.println("Error in reading from input code file: " + e1.getMessage());
	            System.exit(1);
	    }
        
        catch (Exception e2) { //lexical, syntax or other exception thrown by parse()

        	System.out.println(e2); //toString of e2
		 	System.exit(0);
        }
		
        
        System.out.println("Parsed " + ic_code_path + " successfully!");
        Program root = (Program)parseSym.value; 
        
        
        //pretty-print the AST of ic file
        if (printast_flag) {
        	
        	PrettyPrinter pPrinter = new PrettyPrinter(ic_code_path);  //input ic code file path
        	System.out.println(root.accept(pPrinter));   //the printing itself
        	System.out.println();
        }
  
        
        //-------------------------------------------
        
        
        Symbol parseLibSym = new Symbol(1);  //init. to avoid errors.
    	ICClass libraryRoot = new ICClass(0,null,null,null);
    	
        //parsing library sig.
    	try {
        	
        	/* reader to lib. sig. file. */
            FileReader libFile = new FileReader(libic_path); //can throw io exception
                
            Lexer scanner = new Lexer(libFile);
            LibraryParser parser = new LibraryParser(scanner);
            
            parseLibSym = parser.parse();    //can throw LexicalError or SyntaxError
            
            
        } catch (IOException e1) {  /* problem with input file */
	   
	            System.out.println("Error in reading from input code file: " + e1.getMessage());
	            System.exit(1);
	    }
        
        catch (Exception e2) { /* lexical, syntax or other exception thrown by parse() */
 
        	System.out.println(e2); //toString of e2
		 	System.exit(0);
        }
			
        System.out.println("Parsed " + libic_path + " successfully!");
        libraryRoot =  (ICClass)parseLibSym.value;
            
        //pretty-print the AST of library sig. file
        if (printast_flag) {
        	
        	PrettyPrinter libPrinter = new PrettyPrinter(libic_path);  //path of lib. sig.
            System.out.println(libraryRoot.accept(libPrinter));   //the printing itself
            System.out.println();
        }
        
        
        System.out.println();
        
        /* semantic analysis phase */
        
        root.insertClass(libraryRoot); //adding Library class as 1st one to list of ic program classes.  
        
        SymbolTableBuilder builder = new SymbolTableBuilder(ic_code_path);
        Object globalSymbolTable = root.accept(builder, null);
        
        //failed to construct GST.
        if (globalSymbolTable == null) {
        	System.out.println("Error while constructing global symbol table.");
            System.exit(0);
        }

        //dump the global symbol table + type table.
        if (symtab_flag) {
        	System.out.println("\n" + globalSymbolTable + "\n" + TypeTable.staticToString());
        }
        
        //semantic checks.
        SemanticChecks sc = new SemanticChecks((GlobalSymbolTable)globalSymbolTable);
        Object semanticChecks = root.accept(sc);
        
		if (semanticChecks == null) {
			System.out.println("Encountered an error during semantic checks.");
			System.exit(0);    //semantic error exit.
		} else {
			System.out.println("Semantic checks passed successfully!");
		}
		
		/* LIR code translation phase */
		
		if (printlir_flag) {
			GlobalSymbolTable global = (GlobalSymbolTable)globalSymbolTable;		
			TranslatePropagatingVisitor translator = optlir_flag ? new OptTranslatePropagatingVisitor(global) : new TranslatePropagatingVisitor(global);
			if (optlir_flag) {
				int progWeight = (Integer)root.accept(new RegCounterVisitor());
			}
			String tr = root.accept(translator, 0).getLIRCode();
			String lirFileName = args[0].substring(0, args[0].length()-2)+"lir";
			try {
				BufferedWriter buff = new BufferedWriter(new FileWriter(lirFileName));
				buff.write(tr);
				buff.flush();
				buff.close();
			} catch (IOException e) {
				System.out.println("Failed writing to file: " + lirFileName);
			}
			
		}
        
		
    } //end of main.
	
	
} //end of Compiler.
