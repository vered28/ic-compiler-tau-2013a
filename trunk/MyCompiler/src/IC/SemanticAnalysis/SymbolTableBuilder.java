package IC.SemanticAnalysis;

import IC.LiteralTypes;  

import IC.AST.*;

import IC.SymbolTable.*;

import IC.TypeTable.SemanticError;
import IC.TypeTable.TypeTable;

/**
 * Visitor for building symbol tables.
 * 
 * Performs some semantic checks (for example: correct Library name).
 * 
 */
public class SymbolTableBuilder implements PropagatingVisitor {

	public static GlobalSymbolTable gst;
	
	public SymbolTableBuilder(String icFileName) {
		gst = new GlobalSymbolTable();
		gst.setID(icFileName);
		TypeTable.initializeTypeTable(icFileName);
	}

	/** 
	 * 
	 * Will print thrown Semantic Errors during building of tables. 
	 * 
	 */
	private Object handleSemanticError(SemanticError se, ASTNode astnode) {
		if(se.getLine() == -1) {
			se.setLine(astnode.getLine());
		}
		System.out.println(se);   
		return null;
	}

	
	public Object visit(Program program, Object context) {
		
		//Library class doesn't have correct name.
		if (program.getClasses().get(0).getName().equals("Library") == false) {
			System.out.println(new SemanticError("No Library class found", 1, "program"));
			return null;
		}

		for (ICClass c : program.getClasses()) {
			try {
				TypeTable.addClassType(c);   //adds class types to type table.
			} catch (SemanticError se) {
				return handleSemanticError(se, c);
			}
		}
		
		//visits all classes.
		for (ICClass c : program.getClasses()) {
			if (c.accept(this, gst) == null) {
				return null;
			}
		}
		
		return gst;
	}

	public Object visit(ICClass c, Object global_scope) {
		
		ClassSymbol cs = null;
		
		try {
			//adds class symbol to GST, and creates class sym. table.
			cs = ((GlobalSymbolTable)global_scope).addClassSymbol(c);
		} catch (SemanticError se) {
			return handleSemanticError(se, c);
		}

		//visiting methods.
		for (Method m : c.getMethods()) {
			if (m.accept(this, cs.getClassSymbolTable()) == null) {
				return null;
			}
		}
		
		//visiting fields.
		for (Field fld : c.getFields()) {
			if (fld.accept(this, cs.getClassSymbolTable()) == null) {
				return null;
			}
		}

		return cs;
	}

	//help function for method visiting.
	private Object visitMethod(Method meth, Object scope) {
		
		meth.setEnclosingScope((SymbolTable)scope);
		MethodSymbolTable mst = null;

		try {
			
			//creates method sym. table.
			mst = new MethodSymbolTable(meth, (SymbolTable)scope);  //formal params. are added to the mst.
			((SymbolTable)scope).addChild(mst);
			
			//visit formal.
			for (Formal f : meth.getFormals()) {
				if (f.accept(this, mst) == null)
					return null;
			}
			
			//visit statements.
			for (Statement stmt : meth.getStatements()) {
				if (stmt.accept(this, mst) == null)
					return null;
			}

		} catch (SemanticError se) {
			return handleSemanticError(se, meth);
		}

		return mst;
	}

	public Object visit(Field field, Object scope) {
		field.setEnclosingScope((SymbolTable)scope);
		return true;
	}

	public Object visit(VirtualMethod method, Object scope) {
		return visitMethod(method, scope);
	}

	public Object visit(StaticMethod method, Object scope) {
		return visitMethod(method, scope);
	}

	public Object visit(LibraryMethod method, Object scope) {
		return visitMethod(method, scope);
	}

	public Object visit(Formal formal, Object scope) {
		formal.setEnclosingScope((SymbolTable)scope);
		return true;
	}

	public Object visit(PrimitiveType type, Object scope) {
		type.setEnclosingScope((SymbolTable)scope);
		return true;
	}

	public Object visit(UserType type, Object scope) {
		type.setEnclosingScope((SymbolTable)scope);
		return true;
	}

	public Object visit(Assignment assignment, Object scope) {
		
		assignment.setEnclosingScope((SymbolTable)scope);
		
		if (assignment.getVariable().accept(this, (SymbolTable)scope) == null) {
			return null;
		}

		if (assignment.getAssignment().accept(this, (SymbolTable)scope) == null) {
			return null;
		}

		return true;
	}

	public Object visit(CallStatement callStatement, Object scope) {

		callStatement.setEnclosingScope((SymbolTable)scope);

		if (callStatement.getCall().accept(this, (SymbolTable)scope) == null) {
			return null;
		}
		
		return true;
	}

	public Object visit(Return returnStatement, Object scope) {
		
		returnStatement.setEnclosingScope((SymbolTable)scope);
		
		if (returnStatement.hasValue()) {
			if (returnStatement.getValue().accept(this, (SymbolTable)scope) == null) {
				return null;
			}
		}
		
		return true;
	}
	

	public Object visit(If ifStatement, Object scope) {

		ifStatement.setEnclosingScope((SymbolTable)scope);

		if (ifStatement.getCondition().accept(this, (SymbolTable)scope) == null) {
			return null;
		}

		if (ifStatement.getOperation().accept(this, (SymbolTable)scope) == null) {
			return null;
		}

		if (ifStatement.hasElse()) {
			if (ifStatement.getElseOperation().accept(this, (SymbolTable)scope) == null) {
				return null;
			}
		}
		
		return true;
	}

	
	public Object visit(While whileStatement, Object scope) {
		
		whileStatement.setEnclosingScope((SymbolTable)scope);
		
		if (whileStatement.getCondition().accept(this, (SymbolTable)scope) == null) {
			return null;
		}
		
		if (whileStatement.getOperation().accept(this, (SymbolTable)scope) == null) {
			return null;
		}
		
		return true;
	}

	
	public Object visit(Break breakStatement, Object scope) {
		breakStatement.setEnclosingScope((SymbolTable)scope);
		return true;
	}

	public Object visit(Continue continueStatement, Object scope) {
		continueStatement.setEnclosingScope((SymbolTable)scope);
		return true;
	}

	public Object visit(StatementsBlock statementsBlock, Object scope) {
		
		statementsBlock.setEnclosingScope((SymbolTable)scope);
		
		//creating block sym. table.
		BlockSymbolTable bst = new BlockSymbolTable("statement block in " + ((SymbolTable)scope).getID(), (SymbolTable)scope);
		
		((SymbolTable)scope).addChild(bst);
		
		//visiting statements.
		for (Statement stmt : statementsBlock.getStatements()) {
			if (stmt.accept(this, bst) == null) {
				return null;
			}
		}

		return bst;
	}

	
	public Object visit(LocalVariable localVariable, Object scope) {
		
		localVariable.setEnclosingScope((SymbolTable)scope);
		
		try {
			
			((BlockSymbolTable)scope).addLocalVariable(localVariable);
			
			if (localVariable.hasInitValue()) {
				if (localVariable.getInitValue().accept(this, (SymbolTable)scope) == null) {
					return null;
				}
			}
		} catch (SemanticError se) {
			return handleSemanticError(se, localVariable);
		}
		
		return true;
	}

	
	public Object visit(VariableLocation location, Object scope) {
		
		location.setEnclosingScope((SymbolTable)scope); 
		
		if (!location.isExternal()) {
			//keeping scope of declaration of given variable.
			location.setVarDeclarationScope(location.getEnclosingScope().getEnclosingST(location.getName()));
		}
		
		if (location.isExternal()) {
			if (location.getLocation().accept(this, (SymbolTable)scope) == null) {
				return null;
			}
		} 
		
		return true;
	}

	public Object visit(ArrayLocation location, Object scope) {
		
		location.setEnclosingScope((SymbolTable)scope);
		
		if (location.getArray().accept(this, (SymbolTable)scope) == null) {
			return null;
		}

		if (location.getIndex().accept(this, (SymbolTable)scope) == null) {
			return null;
		}

		return true;
	}
	

	public Object visit(StaticCall call, Object scope) {
		
		call.setEnclosingScope((SymbolTable)scope);

		try {
			ClassSymbolTable cst = gst.lookupCST(call.getClassName());
			cst.getMethod(call.getName());  //checks if method was defined.
			
		} catch (SemanticError se) {
			return handleSemanticError(se, call);
		}
		
		//visit arguments.
		for (Expression argum : call.getArguments()) {
			if (argum.accept(this, (SymbolTable)scope) == null) {
				return null;
			}
		}
		
		return true;
	}

	
	public Object visit(VirtualCall call, Object scope) {
		
		call.setEnclosingScope((SymbolTable)scope);

		if (call.isExternal()) {   //the check if method defined - in semantic checks.
			Expression location = call.getLocation();
			
			if (location.accept(this, (SymbolTable)scope) == null) {
				return null;
			}
		}
		
		//visit arguments.
		for (Expression argum : call.getArguments()) {
			if (argum.accept(this, (SymbolTable)scope) == null) {
				return null; 
			}
		}
		
		return true;
	}

	public Object visit(This thisExpression, Object scope) {
		thisExpression.setEnclosingScope((SymbolTable)scope);
		return true;
	}

	public Object visit(NewClass newClass, Object scope) {
		
		newClass.setEnclosingScope((SymbolTable)scope);
		
		return true;
	}

	public Object visit(NewArray newArray, Object scope) {
		
		newArray.setEnclosingScope((SymbolTable)scope);
		
		if (newArray.getType().accept(this, (SymbolTable)scope) == null) {
			return null;
		}
		
		if (newArray.getSize().accept(this, (SymbolTable)scope) == null) {
			return null;
		}
		
		return true;
	}

	public Object visit(Length length, Object scope) {
		
		length.setEnclosingScope((SymbolTable)scope);
		
		if (length.getArray().accept(this, (SymbolTable)scope) == null) {
			return null;
		}
		
		return true;
	}

	//help method.
	private Object visitBinaryOp(BinaryOp binaryOp, Object scope) {
		
		binaryOp.setEnclosingScope((SymbolTable)scope);
		
		if (binaryOp.getFirstOperand().accept(this, (SymbolTable)scope) == null) {
			return null;
		}
		
		if (binaryOp.getSecondOperand().accept(this, (SymbolTable)scope) == null) {
			return null;
		}
		
		return scope;
	}
	
	//help method.
	private Object visitUnaryOp(UnaryOp unaryOp, Object scope) {
		
		unaryOp.setEnclosingScope((SymbolTable)scope);
		
		if (unaryOp.getOperand().accept(this, (SymbolTable)scope) == null) {
			return null;
		}
		
		return true;
	}

	public Object visit(MathBinaryOp binaryOp, Object scope) {
		return visitBinaryOp(binaryOp, scope);
	}

	public Object visit(LogicalBinaryOp binaryOp, Object scope) {
		return visitBinaryOp(binaryOp, scope);
	}

	public Object visit(MathUnaryOp unaryOp, Object scope) {
		return visitUnaryOp(unaryOp, scope);
	}

	public Object visit(LogicalUnaryOp unaryOp, Object scope) {
		return visitUnaryOp(unaryOp, scope);
	}

	public Object visit(Literal literal, Object scope) {
		
		literal.setEnclosingScope((SymbolTable)scope);
		return true;
	}

	public Object visit(ExpressionBlock expressionBlock, Object scope) {
		expressionBlock.setEnclosingScope((SymbolTable)scope);
		
		if (expressionBlock.getExpression().accept(this, (SymbolTable)scope) == null) {
			return null;
		}
		
		return true;
	}

}
