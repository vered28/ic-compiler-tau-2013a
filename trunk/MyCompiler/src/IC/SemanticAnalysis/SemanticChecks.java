package IC.SemanticAnalysis;

import java.util.*;  

import IC.AST.*;
import IC.SymbolTable.BlockSymbolTable;
import IC.SymbolTable.FieldSymbol;
import IC.SymbolTable.MethodSymbol;
import IC.SymbolTable.GlobalSymbolTable;
import IC.TypeTable.SemanticError;
import IC.TypeTable.TypeTable;

/**
 * Visitor for semantic checks: 
 * - use of undefined symbols
 * - type checks 
 * - "this" scope rules
 * - "break" and "continue" - only inside loops
 * 
 *  All visitors return null if encounter an error, and true otherwise (or some type).
 */
public class SemanticChecks implements Visitor {
	
	private IC.SymbolTable.GlobalSymbolTable GST;
	
	private boolean insideStatic = false;
	private int insideLoop = 0;  //to deal with nested loops.

	/**
	 * Constructor.
	 * 
	 */
	public SemanticChecks(GlobalSymbolTable gst) {
		this.GST = gst;
	}

	
	/**
	 * Program visitor: Recursive visit in all classes of the program.
	 * Checks if class hierarchy has 'main' method.
	 * 
	 */
	public Object visit(Program program) {
		for (ICClass c : program.getClasses()) {
			if (c.accept(this) == null) {
				return null;
			}
		}
		
		//at this stage, due to recursive visits, we've visited everywhere.
		//so, we can check existence of 'main' method.
		if (this.GST.hasMainMethod() == false) {
			System.out.println(new SemanticError("Class hierarchy doesn't have main method",1,"main"));
			return null;
		}
		
		return true;
	}

	
	/**
	 * ICClass visitor: Recursive visit in all methods of icClass. 
	 * 
	 */
	public Object visit(ICClass icClass) {
		for (Method m : icClass.getMethods()) {
			if (m.accept(this) == null) {
				return null;
			}
		}
		
		return true;
	}

	
	/**
	 * Field visitor: Never called.
	 */
	public Object visit(Field field) {
		return true;
	}
	

	/**
	 * visitMethod: Help function to visit methods. 
	 * Recursive visit in all statements.
	 * 
	 */
	private Object visitMethod(Method method) {
		for (Statement s : method.getStatements()) {
			if (s.accept(this) == null) {
				return null;
			}
		}
		return true;
	}

	/**
	 * VirtualMethod visitor.
	 * 
	 */
	public Object visit(VirtualMethod method) {
		return visitMethod(method);
	}

	/**
	 * StaticMethod visitor.
	 * 
	 */
	public Object visit(StaticMethod method) {
		insideStatic = true;
		Object ret = visitMethod(method);
		insideStatic = false;
		return ret;
	}

	/**
	 * LibraryMethod visitor.
	 * 
	 */
	public Object visit(LibraryMethod method) {
		return visitMethod(method);
	}

	/**
	 * Formal visitor: Never called.
	 */
	public Object visit(Formal formal) {
		return true;
	}

	/**
	 * PrimitiveType visitor: Never called.
	 */
	public Object visit(PrimitiveType type) {
		return true;
	}

	/**
	 * UserType visitor: Never called.
	 */
	public Object visit(UserType type) {
		return true;
	}

	/**
	 * Assignment visitor: Visits locations and assignment recursively.
	 * Checks that assignment type fits location type.
	 * 
	 */
	public Object visit(Assignment assignment) {
		
		//gets the type of left-hand side of an assignment. 
		IC.TypeTable.Type loc = (IC.TypeTable.Type)(assignment.getVariable().accept(this)); 
		if (loc == null) {
			return null;
		}
		
		//gets the type of right-hand side of an assignment. 
		IC.TypeTable.Type ass = (IC.TypeTable.Type)(assignment.getAssignment().accept(this));
		if (ass == null) {
			return null;
		}

		//Type check - checks if LHS and RHS types fit.
		if (!ass.subtypeof(loc)) {
			System.out.println(new SemanticError("Types mismatch, not of type " + loc.getName(), assignment.getLine(), ass.getName()));
			return null;
		}

		return true;
	}

	
	/**
	 * CallStatement visitor: Visits call recursively. 
	 * 
	 */
	public Object visit(CallStatement callStatement) {
		if (callStatement.getCall().accept(this) == null) {
			return null;
		}
		
		return true;
	}

	/**
	 * Return visitor: Calls value recursively. 
	 * Checks if return type fits type of enclosing method.
	 * 
	 */
	public Object visit(Return returnStatement) {
		
		IC.TypeTable.Type returnedType = null;
		
		//gets the type of returned expr.
		if (returnStatement.hasValue()) {
			returnedType = (IC.TypeTable.Type)(returnStatement.getValue().accept(this));
			if (returnedType == null) {
				return null;
			}
			
		} else {  
			try {
				returnedType = IC.TypeTable.TypeTable.getType("void");
			} catch (SemanticError se) { //we'll never get here.
				
				System.out.println("Error in the return statement visitor.");
			}
		}

		//Type check - checks if the type of ret. expr. fits to type of the enclosing method's ret. type.
		
		IC.TypeTable.Type methReturnType = ((BlockSymbolTable)(returnStatement.getEnclosingScope())).getReturnType();

		if (!returnedType.subtypeof(methReturnType)) {
			System.out.println(new SemanticError("Types mismatch, not of type " + methReturnType.getName(), 
															returnStatement.getLine(), returnedType.getName()));
			return null;
		}
		
		return true;
	}

	/**
	 * If visitor: Visits condition, operation and elseOperation recursively.
	 * Checks that condition type is boolean. 
	 */
	public Object visit(If ifStatement) {
		
		//gets the cond. type.
		IC.TypeTable.Type condType = (IC.TypeTable.Type)(ifStatement.getCondition().accept(this));
		if (condType == null) {
			return null;
		}

		//Type check - checks that the condition is of type boolean.
		try {
			if (!condType.subtypeof(IC.TypeTable.TypeTable.getType("boolean"))) {
				System.out.println(new SemanticError("Condition in If statement is not of type boolean",
											ifStatement.getCondition().getLine(), condType.getName()));
				return null;
			}
			
		} catch (SemanticError se) {  //we'll never get here.
			System.out.println("Error in the If visitor.");
		}

		//checks operation and elseOperation.
		if (ifStatement.getOperation().accept(this) == null) {
			return null;
		}
		
		if (ifStatement.hasElse() && ifStatement.getElseOperation().accept(this) == null) {
			return null;
		}

		return true;
	}

	
	/**
	 * While visitor: Visits condition and operation recursively. 
	 * Checks that condition type is boolean. 
	 * 
	 */
	public Object visit(While whileStatement) {
		
		//gets the cond. type.
		IC.TypeTable.Type condType = (IC.TypeTable.Type)(whileStatement.getCondition().accept(this));
		if (condType == null) {
			return null;
		}

		//Type check - checks that the condition is of type boolean.
		try {
			if (!condType.subtypeof(IC.TypeTable.TypeTable.getType("boolean"))) {
				System.out.println(new SemanticError("Condition in While statement not of type boolean",
										whileStatement.getLine(), condType.getName()));
				return null;
			}
			
		} catch (SemanticError se) { // will never get here
			System.out.println("Error in the While visitor.");
		} 

		insideLoop++;  //for break/continue checks.
		
		if (whileStatement.getOperation().accept(this) == null) {
			insideLoop--;
			return null;
		}
		
		insideLoop--;

		return true;
	}

	/**
	 * Break visitor: Checks that we are in a while loop (insideLoop>0).
	 * 
	 */
	public Object visit(Break breakStatement) {
		
		if (insideLoop == 0) {  //not inside while.
			System.out.println(new SemanticError("Break statement outside of a loop", breakStatement.getLine(), "break"));
			return null;
		}

		return true;
	}

	
	/**
	 * Continue visitor: Checks that we are in a while loop (insideLoop>0).
	 *
	 */
	public Object visit(Continue continueStatement) {
		
		if (insideLoop == 0) {   //not inside while.
			System.out.println(new SemanticError("Continue statement outside of a loop", continueStatement.getLine(), "continue"));
			return null;
		}

		return true;
	}

	/**
	 * StatementsBlock visitor: Visits all statements recursively.
	 * 
	 */
	public Object visit(StatementsBlock statementsBlock) {
		
		for (Statement s : statementsBlock.getStatements()) {
			if (s.accept(this) == null) {
				return null;
			}
		}

		return true;
	}

	/**
	 * LocalVariable visitor: Visits initValue recursively. 
	 * Checks that the initValue type is a subtype of the local variable's type.
	 * 
	 */
	public Object visit(LocalVariable localVariable) {
		
		if (localVariable.hasInitValue()) {
			
			//gets the type of init. value.
			IC.TypeTable.Type initValueType = (IC.TypeTable.Type)(localVariable.getInitValue().accept(this));
			if (initValueType == null) {
				return null;
			}

			try {
				//Type check - checks that the initValue type fits to variable's type.
				IC.TypeTable.Type localVarType = 
						((BlockSymbolTable)localVariable.getEnclosingScope()).lookupVariable(localVariable.getName()).getType();

				if (!initValueType.subtypeof(localVarType)) {
					System.out.println(new SemanticError("Types mismatch, not of type " + localVarType.getName(),
												localVariable.getLine(), initValueType.getName()));
					
					return null;
				}
				
			} catch (SemanticError se) {  //we'll never get here.
				System.out.println("Error in LocalVariable visitor.");
			}
		}

		return true;
	}


	/**
	 * VariableLocation visitor: Visits location recursively. 
	 * Checks if location class exists and has needed field.
	 * Returns variable's type.
	 * 
	 */
	public Object visit(VariableLocation location) { 
		
		if (location.isExternal()) {
		
			IC.TypeTable.Type locationType = (IC.TypeTable.Type)location.getLocation().accept(this);
            if (locationType == null) {
                return null;
            }
            
			try {
				//if location is class, we'll check that it has field with this name.
				IC.TypeTable.TypeTable.getClassType(locationType.getName());  //if not class, we go to catch. 
				
				IC.SymbolTable.ClassSymbolTable cst = this.GST.lookupCST(locationType.getName());
					
				FieldSymbol fs = (FieldSymbol)(cst.lookup(location.getName()));
				
				if (fs == null) {
					System.out.println(new SemanticError("Symbol cannot be resolved", location.getLine(), location.getName()));						return null;
				} else {
					return fs.getType();   //return the type of the field.
				}
						
			} catch (SemanticError se) {
				System.out.println(new SemanticError("Location of type " + locationType.getName() + " does not have a field",
															location.getLine(), location.getName()));
				return null;
			}
			
		} else { //location is not external.

			//gets the type of variable.
			
			//to deal with shadowing.
			if (location.getVarDeclarationScope() == null) {
				System.out.println(new SemanticError("Symbol cannot be resolved", location.getLine(), location.getName()));
				return null;
			}
			
			IC.TypeTable.Type variableType = location.getVarDeclarationScope().lookup(location.getName()).getType();
			if (variableType == null) {  //never true.
				System.out.println(new SemanticError("Symbol cannot be resolved", location.getLine(), location.getName()));
				return null;
			}

			return variableType;
		}
		
	}

	/**
	 * ArrayLocation visitor: Recursive visit to array and index. 
	 * Checks if index is integer. 
	 * Returns array's elem. type.
	 * 
	 */
	public Object visit(ArrayLocation location) {
		
		IC.TypeTable.Type arrayType = (IC.TypeTable.Type)location.getArray().accept(this);
		if (arrayType == null) {
			return null;
		}
		
		if (!arrayType.getName().endsWith("[]")) {
			System.out.println(new SemanticError("Using [n] syntax on non-array type", 
					location.getLine(), arrayType.getName()));
			return null;
		}

		IC.TypeTable.Type indexType = (IC.TypeTable.Type)location.getIndex().accept(this);
		if (indexType == null) {
			return null;
		}

		//checks that index is integer.
		try {
			if (!indexType.subtypeof(IC.TypeTable.TypeTable.getType("int"))) {
				System.out.println(new SemanticError("The index of array must be of type int", 
											location.getLine(), arrayType.getName()));
				return null;
			}
			
		} catch (SemanticError se) {  //never get here.
			System.out.println("Error in ArrayLocation visitor.");
		}

		return ((IC.TypeTable.ArrayType)arrayType).getElemType(); 
	}
 
	/**
	 * StaticCall visitor: Visit arguments recursively. 
	 * Checks that the method is defined in the enclosing class. 
	 * Checks that all arguments correspond to the method's arguments types. 
	 * Returns called method return type.
	 * 
	 */
	public Object visit(StaticCall call) {
		
		IC.SymbolTable.ClassSymbolTable cst;
		try {  //check if class of the call exists.
			cst = GST.lookupCST(call.getClassName());
		} catch (SemanticError se) {
			se.setLine(call.getLine());
			System.out.println(se);
			return null;
		}
		
		//Check that method is defined in enclosing class.
		try {
			MethodSymbol method = (MethodSymbol)cst.getMethod(call.getName()); //jump to catch if not found.
			
			//check if static.
			if (!method.isStaticMethod()) {
				System.out.println(new SemanticError("Method is not static", call.getLine(), call.getName()));
				return null;
			}
			
			//check arguments.
			
			//types
			List<IC.TypeTable.Type> methodParamsTypes = ((IC.TypeTable.MethodType)method.getType()).getParamsTypes();
			
			//number of parameters and arguments isn't the same.
			if (call.getArguments().size() != methodParamsTypes.size()) {
				System.out.println(new SemanticError("Wrong number of arguments passed to method", 
												call.getLine(), call.getName()));
				return null;
			}
				
			//going through arguments list.
			for (int i=0; i<call.getArguments().size(); i++) {
				
				Expression arg = call.getArguments().get(i);
				IC.TypeTable.Type argType = (IC.TypeTable.Type)arg.accept(this);

				if (argType == null) {
					return null;
				}

				//wrong argument type.
				if (!argType.subtypeof(methodParamsTypes.get(i))) {
					System.out.println(new SemanticError("Wrong argument type passed to method", 
													call.getLine(), argType.getName()));
					return null;
				}
			}
			
			
			//return method's return type.
			return ((IC.TypeTable.MethodType)method.getType()).getReturnType();
			
		} catch (SemanticError se) {  //method not found.
			se.setLine(call.getLine());
			System.out.println(se);
			return null;
			
		} 
		
	}

	/**
	 * VirtualCall visitor: Visits arguments recursively. 
	 * Checks that the method is defined in the enclosing class. 
	 * Checks that all arguments correspond to the method's arguments types. 
	 * Returns called method return type.
	 * 
	 */
	public Object visit(VirtualCall call) {

		IC.SymbolTable.ClassSymbolTable cst = null;
		
		MethodSymbol ms = null;

		if (call.isExternal()) {
			
			IC.TypeTable.Type locType = (IC.TypeTable.Type)call.getLocation().accept(this);
			if (locType == null) {
				return null;
			}

			try {
				cst = GST.lookupCST(locType.getName()); 
			} catch (SemanticError se) {
				System.out.println(new SemanticError("Location is not of user defined type", 
											call.getLine(), locType.getName()));
				return null;
			}
			
		} else {  //not external call.
			cst = ((BlockSymbolTable)call.getEnclosingScope()).getEnclosingCST();
		}

		try {
			ms = (MethodSymbol)cst.getMethod(call.getName());  //jump to catch if not found.
		} catch (SemanticError se) {
			se.setLine(call.getLine());
			System.out.println(se);
			return null;
		}

		if (insideStatic && !(ms.isStaticMethod())) {
			System.out.println(new SemanticError("Calling virtual method from static scope", 
										call.getLine(), call.getName()));
			return null;
		}
		
		if (call.isExternal() && ms.isStaticMethod()) {
			System.out.println(new SemanticError("External call to static method", 
										call.getLine(), call.getName()));
			return null;
		}
		
		//check arguments.
		
		//types
		List<IC.TypeTable.Type> methodParamsTypes = ((IC.TypeTable.MethodType)ms.getType()).getParamsTypes();
		
		//number of parameters and arguments isn't the same.
		if (call.getArguments().size() != methodParamsTypes.size()) {
			System.out.println(new SemanticError("Wrong number of arguments passed to method", 
											call.getLine(), call.getName()));
			return null;
		}
			
		//going through arguments list.
		for (int i=0; i<call.getArguments().size(); i++) {
			
			Expression arg = call.getArguments().get(i);
			IC.TypeTable.Type argType = (IC.TypeTable.Type)arg.accept(this);

			if (argType == null) {
				return null;
			}

			//wrong argument type.
			if (!argType.subtypeof(methodParamsTypes.get(i))) {
				System.out.println(new SemanticError("Wrong argument type passed to method", 
												call.getLine(), argType.getName()));
				return null;
			}
		}
		
		//return method's return type.
		return ((IC.TypeTable.MethodType)ms.getType()).getReturnType();
		
	}

	/**
	 * Visitor for This: 
	 * Checks that it is not referenced inside static method.
	 * Returns this class type.
	 */
	public Object visit(This thisExpression) {
		if (insideStatic) {
			System.out.println(new SemanticError("Cannot use 'this' in a static method", thisExpression.getLine(), "this"));
			return null;
		}

		return ((BlockSymbolTable)thisExpression.getEnclosingScope()).getEnclosingCST().getMyClassSymbol().getType();
	
	}

	/**
	 * Visitor for the NewClass: 
	 * Checks that the class type exists and the class was defined.
	 * Returns class type.
	 * 
	 */
	public Object visit(NewClass newClass) {
		
		IC.TypeTable.ClassType ct;
		
		try {
			ct = IC.TypeTable.TypeTable.getClassType(newClass.getName());
			GST.lookupCST(newClass.getName());  //goes to catch if class not found.
			
		} catch (SemanticError se) {
			se.setLine(newClass.getLine());
			System.out.println(se);
			return null;
		}

		return ct;
	}

	/**
	 * Visitor for NewArray: 
	 * Checks that element type is a legal type. 
	 * Checks that the size of array is of type int. 
	 * Returns array type.
	 * 
	 */
	public Object visit(NewArray newArray) {
		
		IC.TypeTable.Type elemType;

		try {            
			elemType = IC.TypeTable.TypeTable.getType(newArray.getType().toString());
		} catch (SemanticError se) { //not defined type.
			
			se.setLine(newArray.getLine());
			System.out.println(se);
			return null;
		}

		IC.TypeTable.Type sizeType = (IC.TypeTable.Type)newArray.getSize().accept(this);
		if (sizeType == null) {
			return null;
		}
		
		try {
			if (!sizeType.subtypeof(IC.TypeTable.TypeTable.getType("int"))) {
					System.out.println(new SemanticError("The size of array is not an integer", 
							                        newArray.getLine(), sizeType.getName()));
				return null;
			}
			
		} catch (SemanticError se) {
			System.out.println("Error in newArray visitor.");
		}

		
		try {
			return IC.TypeTable.TypeTable.getType(elemType.getName()+"[]");
		} catch (SemanticError se) {
			System.out.println("Error in newArray visitor.");
		}

		return null;
	}

	/**
	 * Visitor for the array.length: 
	 * Checks that the array is really an array.
	 * Returns type int.
	 * 
	 */
	public Object visit(Length length) {
		
		IC.TypeTable.Type arrType = (IC.TypeTable.Type)length.getArray().accept(this);

		if (arrType == null) {
			return null;
		}

		if (!arrType.getName().endsWith("[]")) {
			System.out.println(new SemanticError("Length requested not on array type", length.getLine(), arrType.getName()));
			return null;
		}

		try {
			return IC.TypeTable.TypeTable.getType("int");
		} catch (SemanticError se) {
			System.out.println("Error in length visitor");
		}

		return null;
	}

	/**
	 * Visitor for MathBinaryOp:
	 * Checks that the types are legal. 
	 * int or string for +, int for everything else.
	 * Returns result type.
	 */
	public Object visit(MathBinaryOp binaryOp) {
		
		IC.TypeTable.Type op1Type = (IC.TypeTable.Type)binaryOp.getFirstOperand().accept(this);
		IC.TypeTable.Type op2Type = (IC.TypeTable.Type)binaryOp.getSecondOperand().accept(this);
		
		if ((op1Type == null) || (op2Type == null)) {
			return null;
		}
		
		if (op1Type != op2Type) 
		{
			System.out.println(new SemanticError("Different operand types", 
										binaryOp.getLine(), binaryOp.getOperator().getOperatorString()));
			return null;
		}

	
		if (binaryOp.getOperator() != IC.BinaryOps.PLUS) {
			try {
				if (!op1Type.subtypeof(IC.TypeTable.TypeTable.getType("int"))) {
					System.out.println(new SemanticError("Math operation on non-int type", 
											binaryOp.getLine(), op1Type.getName()));
					return null;
				}
			} catch (SemanticError se) {
				System.out.println("Error in MathBinaryOP visitor.");
			}
			
		} else {  //operator is plus.
			try {
				if (!op1Type.subtypeof(IC.TypeTable.TypeTable.getType("int")) && 
						!op1Type.subtypeof(IC.TypeTable.TypeTable.getType("string"))) {
					
					System.out.println(new SemanticError("Plus operation on illegal types", binaryOp.getLine(), 
																op1Type.getName()));
					return null;
				}
			} catch (SemanticError se) {
				System.out.println("Error in MathBinaryOP visitor.");
			}
		}
		
		return op1Type;
	}

	/**
	 * Visitor for LogicalBinaryOp:
	 * Checks that the operands are of the correct type.
	 * Returns result type.
	 * 
	 */
	public Object visit(LogicalBinaryOp binaryOp) {
		
		IC.TypeTable.Type op1Type = (IC.TypeTable.Type)binaryOp.getFirstOperand().accept(this);
		IC.TypeTable.Type op2Type = (IC.TypeTable.Type)binaryOp.getSecondOperand().accept(this);

		if ((op1Type == null) || (op2Type == null)) {
			return null;
		}

	
		if (!op1Type.subtypeof(op2Type) && !op2Type.subtypeof(op1Type)) {  //error in any case.
			
			if (binaryOp.getOperator() == IC.BinaryOps.LAND || binaryOp.getOperator() == IC.BinaryOps.LOR) {
				System.out.println(new SemanticError("Logical AND/OR on non-boolean types", binaryOp.getLine(),
														    binaryOp.getOperator().getOperatorString()));
				return null;
			}
			
			
			if (binaryOp.getOperator() == IC.BinaryOps.EQUAL || binaryOp.getOperator() == IC.BinaryOps.NEQUAL) {
				System.out.println(new SemanticError("Comparing types that are not subtypes of one another",
														binaryOp.getLine(), binaryOp.getOperator().getOperatorString()));
				return null;
			}
			
			
			//here, operator is "<=", ">=", "<" or ">".
			System.out.println(new SemanticError("Comparing non-int types", binaryOp.getLine(), 
													binaryOp.getOperator().getOperatorString()));
			return null;
			
		}
		
		
		//one of the operands is subtype of the other.
		
		try {
			if (op1Type.subtypeof(IC.TypeTable.TypeTable.getType("void"))) {
				System.out.println(new SemanticError("Cannot perform logical operation on void types",
															binaryOp.getLine(), op1Type.getName()));
				return null;
			}
			
		} catch (SemanticError se) {
			System.out.println("Error in LogicalBinaryOP visitor.");
		}
		
		if ((binaryOp.getOperator() == IC.BinaryOps.LAND) || (binaryOp.getOperator() == IC.BinaryOps.LOR)) {
			try {
				if (!op1Type.subtypeof(IC.TypeTable.TypeTable.getType("boolean"))) {
					System.out.println(new SemanticError("Cannot perform logical operation on non-boolean values",
																binaryOp.getLine(), op1Type.getName()));
					return null;
				}
				
			} catch (SemanticError se) {
				System.out.println("Error in LogicalBinaryOP visitor.");
			}
		}
		
		//operator is "<=", ">=", "<" or ">".
		if ((binaryOp.getOperator() != IC.BinaryOps.EQUAL) && (binaryOp.getOperator() != IC.BinaryOps.NEQUAL)) {
			try {
				if (!op1Type.subtypeof(IC.TypeTable.TypeTable.getType("int"))) {
					System.out.println(new SemanticError("Comparing non-int values", 
												binaryOp.getLine(), op1Type.getName()));
					return null;
				}
				
			} catch (SemanticError se) {
				System.out.println("Error in LogicalBinaryOP visitor.");
			}
		}

		
		//everything is legal.
		IC.TypeTable.Type ret = null;
		try {
			ret = IC.TypeTable.TypeTable.getType("boolean");
		} catch (SemanticError se) {
			System.out.println("Error in LogicalBinaryOP visitor.");
		}

		return ret;
	}

	/**
	 * Visitor for MathUnaryOp:
	 * Checks that the operand is of type int.
	 * Returns result type.
	 *  
	 */
	public Object visit(MathUnaryOp unaryOp) {
		IC.TypeTable.Type uopType = (IC.TypeTable.Type) unaryOp.getOperand().accept(this);
		if (uopType == null) {
			return null;
		}

		try {
			if (!uopType.subtypeof(IC.TypeTable.TypeTable.getType("int"))) {
				System.out.println(new SemanticError("Math unary operation on a non-int type",
											unaryOp.getLine(), uopType.getName()));
				return null;
			}
			
		} catch (SemanticError se) {
			System.out.println("Error in MathUnaryOp visitor.");
		}
		
		return uopType; 
	}
	

	/**
	 * Visitor for LogicalUnaryOp:
	 * Checks that the operand is of type boolean.
	 * Returns result type. 
	 * 
	 */
	public Object visit(LogicalUnaryOp unaryOp) {
		
		IC.TypeTable.Type uopType = (IC.TypeTable.Type)unaryOp.getOperand().accept(this);
		if (uopType == null) {
			return null;
		}
		try {
			if (!uopType.subtypeof(IC.TypeTable.TypeTable.getType("boolean"))) {
				System.out.println(new SemanticError("Logical unary operand is not boolean", unaryOp.getLine(), uopType.getName()));
				return null;
			}
		} catch (SemanticError se) {
			System.out.println("Error in LogicalUnaryOp visitor.");
		}
		
		return uopType;
	}

	/**
	 * Literal visitor.
	 * Returns primitive type.
	 */
	public Object visit(Literal literal) {
		IC.LiteralTypes type = literal.getType();
		
		try {
			switch (type) {
				case STRING:
					return TypeTable.getType("string");
				case INTEGER:
					return TypeTable.getType("int");
				case TRUE:
					return TypeTable.getType("boolean");
				case FALSE:
					return TypeTable.getType("boolean");
				case NULL:
					return TypeTable.getType("null");
			}
			
		} catch (SemanticError se) { //never get here.
			System.out.println("Error in Literal visitor.");
		}
		
		return null;
	}

	/**
	 * ExpressionBlock visitor: Visits expression recursively.
	 * Returns result of visit.
	 */
	public Object visit(ExpressionBlock expressionBlock) {
		return (IC.TypeTable.Type)(expressionBlock.getExpression().accept(this));
	}

}