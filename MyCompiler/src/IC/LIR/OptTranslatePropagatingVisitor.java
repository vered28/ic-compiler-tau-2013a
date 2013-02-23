package IC.LIR;

import IC.BinaryOps;
import IC.AST.*;
import IC.SymbolTable.*;
import IC.SemanticAnalysis.SemanticChecks;
import IC.LIR.LIRFlagEnum;
import java.util.*;

/**
 * Translating visitor to LIR
 * with optimizations
 */
public class OptTranslatePropagatingVisitor extends TranslatePropagatingVisitor{
	
	/**
	 * constructor
	 * @param global
	 */
	public OptTranslatePropagatingVisitor(GlobalSymbolTable global){
		super(global);
	}
	
	private ClassLayout libicLayout = null;
	
	
	/**
	 * Program propagating visitor:
	 * - recursive calls to all classes in program
	 * - returns the LIR representation of the IC program ordered by:
	 * 		- string literals
	 * 		- class dispatch tables
	 * 		- methods
	 * 		- main method
	 * @param program
	 * @param d
	 * @return
	 */
	public LIRUpType visit(Program program, Integer d){
		
		for(ICClass c: program.getClasses()){
			// skip library method
			if (c.getName().equals("Library")) {
				libicLayout = new ClassLayout(c);
				continue;
			}
			
			// create class layout
			ClassLayout classLayout;
			if (c.hasSuperClass()){
				// already have super-class layout at this point
				classLayout = new ClassLayout(c, classLayouts.get(c.getSuperClassName()));
			} else {
				classLayout = new ClassLayout(c);
			}
			// insert to classLayouts
			classLayouts.put(c.getName(), classLayout);
			// insert class dispatch table representation
			classDispatchTable.add(classLayout.getDispatchTable());

		}
		
		// visit all classes recursively
		for(ICClass c: program.getClasses()){
			if (!c.getName().equals("Library"))
				c.accept(this, 0);
		}
		
		// return LIR representation for the IC program
		String lirBuffer = "";
		
		lirBuffer += "# this is the optimized lir translation\n";
		
		// (1) insert all string literals
		lirBuffer += "# string literals\n";
		// insert error messages strings
		lirBuffer += "str_null_ref: \"Runtime Error: Null pointer dereference!\"\n";
		lirBuffer += "str_array_access: \"Runtime Error: Array index out of bounds!\"\n";
		lirBuffer += "str_size: \"Runtime Error: Array allocation with negative array size!\"\n";
		lirBuffer += "str_zero: \"Runtime Error: Division by zero!\"\n";
		
		for (String strLiteral: this.stringLiterals.keySet()){
			lirBuffer += this.getStringLiterals().get(strLiteral)+": "+strLiteral+"\n";
		}
		lirBuffer += "\n";
		
		// (2) insert class dispatch tables
		lirBuffer += "# class dispatch tables\n";
		for (String classDisTab: this.classDispatchTable){
			lirBuffer += classDisTab+"\n";
		}
		lirBuffer += "\n";
		
		// (3) insert all methods
		// insert runtime check methods
		lirBuffer += runtimeChecks;
		// insert all user methods
		lirBuffer += "# methods\n";
		for (String methodStr: this.methods){
			lirBuffer += methodStr+"\n";
		}
		
		// (4) insert main method
		lirBuffer += "# main method\n";
		lirBuffer += this.mainMethod;
		
		// (5) insert error exit label
		lirBuffer += "\n_error_exit:\n";
		
		return new LIRUpType(lirBuffer, LIRFlagEnum.EXPLICIT,"");
	}

	/**
	 * ICClass propagating visitor:
	 * - updates class dispatch tables
	 * - recursive calls to all methods in the class
	 * @param icClass
	 * @param d
	 * @return
	 */
	public LIRUpType visit(ICClass icClass, Integer d){
		// set current class name
		currClassName = icClass.getName();
		
		// recursive calls to methods
		for(Method m: icClass.getMethods()){
			m.accept(this,0);
			// each method will be responsible to insert its string rep. to the methods list
		}

		// fields: no need for recursive calls
		
		return new LIRUpType("", LIRFlagEnum.EXPLICIT,"");
	}

	/**
	 * Field propagating visitor: never called
	 */
	public LIRUpType visit(Field field, Integer d){
		return new LIRUpType("", LIRFlagEnum.EXPLICIT,"");
	}

	/**
	 * VirtualMethod propagating visitor:
	 * see methodVisitHelper documentation
	 * @param method
	 * @param d
	 * @return
	 */
	public LIRUpType visit(VirtualMethod method, Integer d){
		methodVisitHelper(method, d, false);
		return new LIRUpType("", LIRFlagEnum.EXPLICIT,"");
	}

	/**
	 * StaticMethod propagating visitor:
	 * see methodVisitHelper documentation
	 * @param method
	 * @param d
	 * @return
	 */
	public LIRUpType visit(StaticMethod method, Integer d){
		// check if this method is the program's main method
		boolean isMain = method.getName().equals("main") &&
						 method.getType().getName().equals("void") &&
						 method.getFormals().size() == 1 &&
						 method.getFormals().get(0).getType().toString().equals("string[]");
		methodVisitHelper(method, d, isMain);
		return new LIRUpType("", LIRFlagEnum.EXPLICIT,"");
	}
	
	/**
	 * Virtual / Static method visitor helper
	 * - creates LIR representation for the method code and updates methods list
	 * - includes recursive calls to all method's statements
	 * @param method
	 * @param d
	 * @return
	 */
	public LIRUpType methodVisitHelper(Method method, Integer d, boolean isMain){
		String methodLIRCode = "";
		
		// create method label
		String methodLabel = "_";
		methodLabel += isMain ? "ic" : ((ClassSymbolTable) method.getEnclosingScope()).getMyClassSymbol().getID();
		methodLabel += "_"+method.getName();
		
		methodLIRCode += methodLabel+":\n";
		
		// insert method's code recursively
		for (Statement s: method.getStatements()){
			methodLIRCode += s.accept(this,0).getLIRCode();
		}
		
		// if method is void (but not main), concatenate a "return 9999"
		if(method.getType().getName().equals("void") && !isMain){
			methodLIRCode += "Return 9999\n";
		}
		
		// update methods list / main method
		if (isMain){
			mainMethod = methodLIRCode;
		} else {
			methods.add(methodLIRCode);
		}
		
		return new LIRUpType("", LIRFlagEnum.EXPLICIT,"");
	}

	/**
	 * LibraryMethod propagating visitor:
	 * does nothing since its LIR implementation is provided externally
	 */
	public LIRUpType visit(LibraryMethod method, Integer d){
		return new LIRUpType("", LIRFlagEnum.EXPLICIT,"");
	}

	/**
	 * Formal propagating visitor: never called
	 */
	public LIRUpType visit(Formal formal, Integer d){
		return new LIRUpType("", LIRFlagEnum.EXPLICIT,"");
	}

	/**
	 * PrimitiveType propagating visitor: never called
	 */
	public LIRUpType visit(PrimitiveType type, Integer d){
		return new LIRUpType("", LIRFlagEnum.EXPLICIT,"");
	}

	/**
	 * UserType propagating visitor: never called
	 */
	public LIRUpType visit(UserType type, Integer d){
		return new LIRUpType("", LIRFlagEnum.EXPLICIT,"");
	}

	/**
	 * Assignment propagating visitor:
	 * - translate recursively the variable and the assignment
	 * - concatenate the translations to the LIR assignment instruction
	 */
	public LIRUpType visit(Assignment assignment, Integer d){
		String tr = "";
		
		LIRUpType assign = assignment.getAssignment().accept(this, d);
		LIRUpType var = assignment.getVariable().accept(this, d+1);
		
		// translate assignment
		tr += assign.getLIRCode();
		if (assign.getLIRInstType() != LIRFlagEnum.REGISTER &&
				assign.getLIRInstType() != LIRFlagEnum.LITERAL){
			if (!(assign.getLIRInstType() == LIRFlagEnum.LOC_VAR_LOCATION && 
					var.getLIRInstType() == LIRFlagEnum.REGISTER)){
				tr += getMoveCommand(assign.getLIRInstType());
				tr += assign.getTargetRegister()+",R"+d+"\n";
				assign.setTargetRegister("R"+d);
				assign.setLIRInstType(LIRFlagEnum.REGISTER);
			}
		}
		
		// translate variable
		tr += var.getLIRCode();
				
		// handle all variable cases
		tr += getMoveCommand(var.getLIRInstType());
		tr += assign.getTargetRegister()+","+var.getTargetRegister()+"\n";
		
		return new LIRUpType(tr, LIRFlagEnum.STATEMENT,"");
	}

	/**
	 * returns the ASTNode Field for the given field name,
	 * starting the search from the given ICClass.
	 * returns null if didn't find it (not supposed to happen).
	 * @param icClass
	 * @param fieldName
	 * @return
	 */
	public Field getFieldASTNodeRec(ICClass icClass, String fieldName){
		for(Field f: icClass.getFields()){
			if (f.getName().equals(fieldName))
				return f;
		}
		if (icClass.hasSuperClass()){
			return getFieldASTNodeRec(global.getClass(icClass.getSuperClassName()).getIcClass(), fieldName);
		} else
			System.err.println("error in getFieldASTNodeRec");
		return null;
	}
	
	/**
	 * VariableLocation propagating visitor:
	 * - translate recursively the location
	 * - concatenate the translations to the LIR location update instruction
	 */
	public LIRUpType visit(VariableLocation location, Integer d){
		String tr = "";
		
		if (location.isExternal()){
			// translate the location
			LIRUpType loc = location.getLocation().accept(this, d);
			// add code to translation
			tr += loc.getLIRCode();
			
			// get the ClassLayout for the location
			IC.TypeTable.Type locationClassType = 
				(IC.TypeTable.Type)location.getLocation().accept(new IC.SemanticAnalysis.SemanticChecks(global));
			ClassLayout locationClassLayout = classLayouts.get(locationClassType.getName());
			
			// get the field offset for the variable
			Field f = getFieldASTNodeRec(locationClassLayout.getICClass(), location.getName());
			
			// get the field offset
			int fieldOffset = locationClassLayout.getFieldOffset(f);
			
			// translate this step
			String locReg = "R"+d;
			if (loc.getLIRInstType() != LIRFlagEnum.REGISTER){
				tr += getMoveCommand(loc.getLIRInstType());
				tr += loc.getTargetRegister()+","+locReg+"\n";
				loc.setTargetRegister(locReg);
				loc.setLIRInstType(LIRFlagEnum.REGISTER);
			}
			
			// check external location null reference
			tr += "StaticCall __checkNullRef(a=R"+d+"),Rdummy\n";
			
			return new LIRUpType(tr, LIRFlagEnum.EXT_VAR_LOCATION, locReg+"."+fieldOffset);
		}else{
			// check if the variable is a field
			if (((BlockSymbolTable)location.getEnclosingScope()).isVarField(location.getName())){
				String thisClassName = ((BlockSymbolTable)location.getEnclosingScope()).getEnclosingCST().getMyClassSymbol().getID();
				
				ClassLayout locationClassLayout = classLayouts.get(thisClassName);
				
				// get the field offset for the variable
				Field f = getFieldASTNodeRec(locationClassLayout.getICClass(), location.getName());
				
				// get the field offset
				int fieldOffset = locationClassLayout.getFieldOffset(f);
				
				tr += "Move this,R"+d+"\n";
				String tgtLoc = "R"+d+"."+fieldOffset;
				
				// translate only the variable name
				return new LIRUpType(tr,LIRFlagEnum.EXT_VAR_LOCATION,tgtLoc);

			} else {
				// translate only the variable name
				return new LIRUpType("",LIRFlagEnum.LOC_VAR_LOCATION,location.getNameDepth());
			}
		}
	}

	/**
	 * ArrayLocation propagating visitor:
	 * - translate recursively the array and the index
	 * - concatenate the translations to the LIR array location update instruction
	 */
	public LIRUpType visit(ArrayLocation location, Integer d){
		String tr = "";
		
		// decide which block will be first by the Setti Ullman method
		boolean suBool = location.getArray().getRequiredRegs() >= location.getIndex().getRequiredRegs();  
		
		if (suBool){
			// translate array
			LIRUpType array = location.getArray().accept(this, d);
			tr += array.getLIRCode();

			// move result to a single register
			if (array.getLIRInstType() != LIRFlagEnum.REGISTER){
				tr += getMoveCommand(array.getLIRInstType());
				tr += array.getTargetRegister()+",R"+d+"\n";
				array.setTargetRegister("R"+d);
				array.setLIRInstType(LIRFlagEnum.REGISTER);
			}

			// check array null reference
			tr += "StaticCall __checkNullRef(a=R"+d+"),Rdummy\n";

			// translate index
			LIRUpType index = location.getIndex().accept(this, d+1);
			tr += index.getLIRCode();

			// move result to a single register
			if (index.getLIRInstType() != LIRFlagEnum.REGISTER){
				tr += getMoveCommand(index.getLIRInstType());
				tr += index.getTargetRegister()+",R"+(d+1)+"\n";
				index.setTargetRegister("R"+(d+1));
				index.setLIRInstType(LIRFlagEnum.REGISTER);
			}

			// check array access
			tr += "StaticCall __checkArrayAccess(a=R"+d+",i=R"+(d+1)+"),Rdummy\n";
		} else {
			// translate index
			LIRUpType index = location.getIndex().accept(this, d);
			tr += index.getLIRCode();

			// move result to a single register
			if (index.getLIRInstType() != LIRFlagEnum.REGISTER){
				tr += getMoveCommand(index.getLIRInstType());
				tr += index.getTargetRegister()+",R"+d+"\n";
				index.setTargetRegister("R"+d);
				index.setLIRInstType(LIRFlagEnum.REGISTER);
			}

			// check array access
			tr += "StaticCall __checkArrayAccess(a=R"+d+",i=R"+d+"),Rdummy\n";

			// translate array
			LIRUpType array = location.getArray().accept(this, d+1);
			tr += array.getLIRCode();

			// move result to a single register
			if (array.getLIRInstType() != LIRFlagEnum.REGISTER){
				tr += getMoveCommand(array.getLIRInstType());
				tr += array.getTargetRegister()+",R"+(d+1)+"\n";
				array.setTargetRegister("R"+(d+1));
				array.setLIRInstType(LIRFlagEnum.REGISTER);
			}

			// check array null reference
			tr += "StaticCall __checkNullRef(a=R"+(d+1)+"),Rdummy\n";

		}
		
		return new LIRUpType(tr, LIRFlagEnum.ARR_LOCATION,"R"+d+"[R"+(d+1)+"]");
	}

	/**
	 * CallStatement propagating visitor:
	 * - translate recursively the call expression and return its translation
	 */
	public LIRUpType visit(CallStatement callStatement, Integer d){
		return callStatement.getCall().accept(this, d);
	}

	/**
	 * Return propagating visitor:
	 * - translate recursively the returned expression
	 * - concatenate the translations to the LIR return statement update instruction
	 */
	public LIRUpType visit(Return returnStatement, Integer d){
		String tr = "";
		if (returnStatement.hasValue()){
			LIRUpType returnVal = returnStatement.getValue().accept(this, d);
			tr += returnVal.getLIRCode();
			tr += "Return "+returnVal.getTargetRegister()+"\n";
		} else {
			tr += "Return 9999\n";
		}
		
		return new LIRUpType(tr, LIRFlagEnum.STATEMENT, "");
	}

	/**
	 * If propagating visitor:
	 * - translate recursively the condition, then statement and else statement
	 * - concatenate the translations to the LIR if statement update instruction
	 */
	public LIRUpType visit(If ifStatement, Integer d){
		String tr = "";
		String falseLabel = "_false_label"+labelCounter;
		String endLabel = "_end_label"+(labelCounter++);
		
		// recursive call the condition expression
		LIRUpType condExp = ifStatement.getCondition().accept(this, d);
		tr += condExp.getLIRCode();
		if (condExp.getLIRInstType() != LIRFlagEnum.REGISTER){
			tr += getMoveCommand(condExp.getLIRInstType());
			tr += condExp.getTargetRegister()+",R"+d+"\n";
			condExp.setTargetRegister("R"+d);
			condExp.setLIRInstType(LIRFlagEnum.REGISTER);
		}
		
		// check condition
		tr += "Compare 0,R"+d+"\n";
		if (ifStatement.hasElse()) tr += "JumpTrue "+falseLabel+"\n";
		else tr += "JumpTrue "+endLabel+"\n";
		
		// recursive call to the then statement
		LIRUpType thenStat = ifStatement.getOperation().accept(this, d);
		tr += thenStat.getLIRCode();
		
		if (ifStatement.hasElse()){
			tr += "Jump "+endLabel+"\n";

			// recursive call to the else statement
			tr += falseLabel+":\n";
			LIRUpType elseStat = ifStatement.getElseOperation().accept(this, d);
			tr += elseStat.getLIRCode();
		}
		
		tr += endLabel+":\n";
		
		return new LIRUpType(tr, LIRFlagEnum.STATEMENT,"");
	}

	/**
	 * While propagating visitor:
	 * - translate recursively the condition, and then statement
	 * - concatenate the translations to the LIR while statement update instruction
	 */
	public LIRUpType visit(While whileStatement, Integer d){
		// save while id previous value and set current
		int prevWhileID = currWhileID;
		currWhileID = labelCounter;
		
		String tr = "";
		String whileLabel = "_while_cond_label"+labelCounter;
		String endLabel = "_end_label"+(labelCounter++);
		
		tr += whileLabel+":\n";
		// recursive call to condition
		LIRUpType condExp = whileStatement.getCondition().accept(this, d);
		tr += condExp.getLIRCode();
		if (condExp.getLIRInstType() != LIRFlagEnum.REGISTER){
			tr += getMoveCommand(condExp.getLIRInstType());
			tr += condExp.getTargetRegister()+",R"+d+"\n";
			condExp.setTargetRegister("R"+d);
			condExp.setLIRInstType(LIRFlagEnum.REGISTER);
		}
		
		// check condition
		tr += "Compare 0,R"+d+"\n";
		tr += "JumpTrue "+endLabel+"\n";
		
		// recursive call to operation statement
		tr += whileStatement.getOperation().accept(this,d).getLIRCode();
		tr += "Jump "+whileLabel+"\n";
		tr += endLabel+":\n";
		
		// set while id back to previous value
		currWhileID = prevWhileID;
		return new LIRUpType(tr, LIRFlagEnum.STATEMENT,"");
	}

	/**
	 * Break propagating visitor:
	 * - return the break statement
	 */
	public LIRUpType visit(Break breakStatement, Integer d){
		String tr = "Jump _end_label"+currWhileID+"\n";
		return new LIRUpType(tr, LIRFlagEnum.STATEMENT,"");
	}

	/**
	 * Continue propagating visitor:
	 * - return the continue statement
	 */
	public LIRUpType visit(Continue continueStatement, Integer d){
		String tr = "Jump _while_cond_label"+currWhileID+"\n";
		return new LIRUpType(tr, LIRFlagEnum.STATEMENT,"");
	}

	/**
	 * StatementsBlock propagating visitor:
	 * - translate recursively all statements in the block
	 * - concatenate the translations to the LIR code
	 */
	public LIRUpType visit(StatementsBlock statementsBlock, Integer d){
		String tr = "";
		
		// recursive call to all statements in the block
		for (Statement s: statementsBlock.getStatements()){
			tr += s.accept(this, d).getLIRCode();
		}
		
		return new LIRUpType(tr, LIRFlagEnum.STATEMENT,"");
	}

	/**
	 * LocalVariable propagating visitor:
	 * - translate recursively the init value
	 * - concatenate the translations to the LIR local variable statement instruction
	 */
	public LIRUpType visit(LocalVariable localVariable, Integer d){
		String tr = "";
		
		if (localVariable.hasInitValue()){
			LIRUpType initVal = localVariable.getInitValue().accept(this, d);
			tr += initVal.getLIRCode();
			if (initVal.getLIRInstType() != LIRFlagEnum.REGISTER){
				tr += getMoveCommand(initVal.getLIRInstType());
				tr += initVal.getTargetRegister()+",R"+d+"\n";
				initVal.setTargetRegister("R"+d);
				initVal.setLIRInstType(LIRFlagEnum.REGISTER);
			}
			// move register into the local var name
			tr += "Move "+initVal.getTargetRegister()+","+localVariable.getNameDepth()+"\n";
		}
		
		return new LIRUpType(tr, LIRFlagEnum.STATEMENT,"");
	}

	/**
	 * StaticCall propagating visitor:
	 * - translate recursively the list of arguments
	 * - concatenate the translations to the LIR static call statement instruction
	 */
	public LIRUpType visit(StaticCall call, Integer d){
		String tr = "";
		
		// recursive calls to all arguments
		int i = d;
		for (Expression arg: call.getArguments()){
			LIRUpType argExp = arg.accept(this, i);
			tr += "# argument #"+(i-d)+":\n";
			tr += argExp.getLIRCode();
			if (argExp.getLIRInstType() != LIRFlagEnum.REGISTER){
				tr += getMoveCommand(argExp.getLIRInstType());
				tr += argExp.getTargetRegister()+",R"+i+"\n";
				argExp.setTargetRegister("R"+i);
				argExp.setLIRInstType(LIRFlagEnum.REGISTER);
			}
			// increment registers count
			i++;
		}
		
		// check if the call is to a library (static) method
		if (call.getClassName().equals("Library")){
			return libraryCallVisit(tr,call,d);
		}
		
		// call statement
		ClassLayout thisClassLayout = classLayouts.get(call.getClassName());
		Method thisMethod = thisClassLayout.getMethodFromName(call.getName());
		tr += "# call statement:\n";
		// construct method label
		String methodName = "_"+((ClassSymbolTable) thisMethod.getEnclosingScope()).getMyClassSymbol().getID()+
							"_"+call.getName();
		tr += "StaticCall "+methodName+"(";
		// insert <formal>=<argument register>
		for(i = 0; i < call.getArguments().size(); i++){
			tr += thisMethod.getFormals().get(i).getNameDepth()+"=R"+(d+i)+",";
		}
		// remove last comma
		if (tr.endsWith(",")) tr = tr.substring(0, tr.length()-1);
		tr += "),R"+d+"\n";
		
		return new LIRUpType(tr, LIRFlagEnum.REGISTER,"R"+d);
	}
	
	/**
	 * Visitor for LIBRARY static call
	 * called by StaticCall visitor if the call is for a library method
	 * @param call
	 * @param d
	 * @return
	 */
	public LIRUpType libraryCallVisit(String argsTr, StaticCall call, Integer d){
		String tr = argsTr; 
		tr += "Library __"+call.getName()+"(";
		// iterate over values (registers)
		for(int i = 0; i < call.getArguments().size(); i++){
			tr += "R"+(i+d)+",";
		}
		// remove last comma
		if (tr.endsWith(",")) tr = tr.substring(0, tr.length()-1);
		tr += "),R"+d+"\n";
		
		return new LIRUpType(tr, LIRFlagEnum.REGISTER,"R"+d);
	}

	/**
	 * VirtualCall propagating visitor:
	 * - translate recursively the list of arguments
	 * - concatenate the translations to the LIR virtual call statement instruction
	 */
	public LIRUpType visit(VirtualCall call, Integer d){
		String tr = "# virtual call location:\n";;
		
		// recursive call to call location
		if (call.isExternal()){
			LIRUpType location = call.getLocation().accept(this, d);
			tr += location.getLIRCode();
			if (location.getLIRInstType() != LIRFlagEnum.REGISTER){
				tr += getMoveCommand(location.getLIRInstType());
				tr += location.getTargetRegister()+",R"+d+"\n";
				location.setTargetRegister("R"+d);
				location.setLIRInstType(LIRFlagEnum.REGISTER);
			}
			
			// check location null reference
			tr += "StaticCall __checkNullRef(a=R"+d+"),Rdummy\n";
		} else {
			tr += "Move this,R"+d+"\n";
		}
		
		// recursive call to all arguments
		int i = d+1;
		for (Expression arg: call.getArguments()){
			LIRUpType argExp = arg.accept(this, i);
			tr += "# argument #"+(i-d-1)+":\n";
			tr += argExp.getLIRCode();
			if (argExp.getLIRInstType() != LIRFlagEnum.REGISTER){
				tr += getMoveCommand(argExp.getLIRInstType());
				tr += argExp.getTargetRegister()+",R"+i+"\n";
				argExp.setTargetRegister("R"+i);
				argExp.setLIRInstType(LIRFlagEnum.REGISTER);
			}
			// increment registers count
			i++;
		}
		
		// call statement
		tr += "VirtualCall R"+d+".";
		String className = !call.isExternal() ? currClassName :
			((IC.TypeTable.ClassType) call.getLocation().accept(new SemanticChecks(global))).getName();
		ClassLayout thisClassLayout = classLayouts.get(className);
		Method thisMethod = thisClassLayout.getMethodFromName(call.getName());
		int offset = thisClassLayout.getMethodOffset(thisMethod);
		
		tr += offset+"(";
		// insert <formal>=<argument register>
		for(i = 0; i < call.getArguments().size(); i++){
			tr += thisMethod.getFormals().get(i).getNameDepth()+"=R"+(d+i+1)+",";
		}
		// remove last comma
		if (tr.endsWith(",")) tr = tr.substring(0, tr.length()-1);
		tr += "),R"+d+"\n";
		
		return new LIRUpType(tr, LIRFlagEnum.REGISTER,"R"+d);
	}

	/**
	 * This propagating visitor:
	 * - translate this reference: get dispatch vector
	 * - return translation
	 */
	public LIRUpType visit(This thisExpression, Integer d){
		String tr = "Move this,R"+d+"\n";
		return new LIRUpType(tr, LIRFlagEnum.REGISTER,"R"+d);
	}

	/**
	 * NewClass propagating visitor:
	 * - translate new expression
	 * - return LIR code
	 */
	public LIRUpType visit(NewClass newClass, Integer d){
	
		ClassLayout thisClassLayout = classLayouts.get(newClass.getName());
		String tr = "Library __allocateObject("+thisClassLayout.getAllocSize()+"),R"+d+"\n";
		tr += "MoveField _DV_"+thisClassLayout.getClassName()+",R"+d+".0\n";
		
		return new LIRUpType(tr, LIRFlagEnum.REGISTER,"R"+d);
	}

	/**
	 * NewArray propagating visitor:
	 * - translate new expression
	 * - return LIR code
	 */
	public LIRUpType visit(NewArray newArray, Integer d){
		String tr = "";
		
		// recursive call to size
		LIRUpType size = newArray.getSize().accept(this, d);
		tr += size.getLIRCode();
		if (size.getLIRInstType() != LIRFlagEnum.REGISTER){
			tr += getMoveCommand(size.getLIRInstType());
			tr += size.getTargetRegister()+",R"+d+"\n";
			size.setTargetRegister("R"+d);
			size.setLIRInstType(LIRFlagEnum.REGISTER);
		}
		
		// multiply by 4
		tr += "Mul 4,R"+d+"\n";
		
		// check given size n
		tr += "StaticCall __checkSize(n=R"+d+"),Rdummy\n";
		
		// allocate memory
		tr += "Library __allocateArray(R"+d+"),R"+d+"\n";
		
		return new LIRUpType(tr, LIRFlagEnum.REGISTER,"R"+d);
	}

	/**
	 * Length propagating visitor:
	 * - translate the length expression
	 * - return LIR code
	 */
	public LIRUpType visit(Length length, Integer d){
		String tr = "";
		
		// recursive call to array expression
		LIRUpType array = length.getArray().accept(this, d);
		tr += array.getLIRCode();
		if (array.getLIRInstType() != LIRFlagEnum.REGISTER){
			tr += getMoveCommand(array.getLIRInstType());
			tr += array.getTargetRegister()+",R"+d+"\n";
			array.setTargetRegister("R"+d);
			array.setLIRInstType(LIRFlagEnum.REGISTER);
		}
		
		// check array null reference
		tr += "StaticCall __checkNullRef(a=R"+d+"),Rdummy\n";
		
		// get length
		tr += "ArrayLength R"+d+",R"+d+"\n";
		
		return new LIRUpType(tr, LIRFlagEnum.REGISTER,"R"+d);
	}

	/**
	 * MathBinaryOp propagating visitor:
	 * - translate recursively the operator and operands
	 * - return the LIR code
	 */
	public LIRUpType visit(MathBinaryOp binaryOp, Integer d){
		String tr = "";
		// decide which block will be first by the Setti-Ullman algorithm
		boolean suBool = binaryOp.getFirstOperand().getRequiredRegs() >= binaryOp.getSecondOperand().getRequiredRegs(); 
		LIRUpType operand1 = null;
		LIRUpType operand2 = null;
		
		if (suBool){
			// recursive call to operands
			operand1 = binaryOp.getFirstOperand().accept(this, d);
			tr += operand1.getLIRCode();
			if (operand1.getLIRInstType() != LIRFlagEnum.REGISTER){
				tr += getMoveCommand(operand1.getLIRInstType());
				tr += operand1.getTargetRegister()+",R"+d+"\n";
				// update the operand register
				operand1.setTargetRegister("R"+d);
				operand1.setLIRInstType(LIRFlagEnum.REGISTER);
			}
			
			operand2 = binaryOp.getSecondOperand().accept(this, d+1);
			tr += operand2.getLIRCode();
			if (operand2.getLIRInstType() == LIRFlagEnum.ARR_LOCATION ||
					operand2.getLIRInstType() == LIRFlagEnum.EXT_VAR_LOCATION){
				tr += getMoveCommand(operand2.getLIRInstType());
				tr += operand2.getTargetRegister()+",R"+(d+1)+"\n";
				// update the operand register
				operand2.setTargetRegister("R"+(d+1));
				operand2.setLIRInstType(LIRFlagEnum.REGISTER);
			}
		} else {
			// recursive call to operands
			operand2 = binaryOp.getSecondOperand().accept(this, d);
			tr += operand2.getLIRCode();
			if (operand2.getLIRInstType() == LIRFlagEnum.ARR_LOCATION ||
					operand2.getLIRInstType() == LIRFlagEnum.EXT_VAR_LOCATION){
				tr += getMoveCommand(operand2.getLIRInstType());
				tr += operand2.getTargetRegister()+",R"+d+"\n";
				operand2.setTargetRegister("R"+d);
				operand2.setLIRInstType(LIRFlagEnum.REGISTER);
			}
			
			operand1 = binaryOp.getFirstOperand().accept(this, d+1);
			tr += operand1.getLIRCode();
			if (operand1.getLIRInstType() != LIRFlagEnum.REGISTER){
				tr += getMoveCommand(operand1.getLIRInstType());
				tr += operand1.getTargetRegister()+",R"+(d+1)+"\n";
				operand1.setTargetRegister("R"+(d+1));
				operand1.setLIRInstType(LIRFlagEnum.REGISTER);
			}
		}
		
		// operation
		switch (binaryOp.getOperator()){
		case PLUS:
			// check if operation is on strings or on integers
			IC.TypeTable.Type operandsType = (IC.TypeTable.Type) binaryOp.getFirstOperand().accept(new SemanticChecks(global));
			if (operandsType.subtypeof(IC.TypeTable.TypeTable.getUniquePrimitiveTypes().get("int"))){
				tr += "Add "+operand2.getTargetRegister()+","+operand1.getTargetRegister()+"\n";
				if (!operand1.getTargetRegister().equals("R"+d)){
					// put result in Rd anyway
					tr += "Move "+operand1.getTargetRegister()+",R"+d+"\n"; 
				}
			} else { // strings - always at this order
				tr += "Library __stringCat("+operand1.getTargetRegister()+","+operand2.getTargetRegister()+"),R"+d+"\n";
			}
			break;
		case MINUS:
			tr += "Sub "+operand2.getTargetRegister()+","+operand1.getTargetRegister()+"\n";
			if (!suBool) tr += "Move R"+(d+1)+",R"+d+"\n";
			break;
		case MULTIPLY:
			tr += "Mul "+operand2.getTargetRegister()+","+operand1.getTargetRegister()+"\n";
			if (!operand1.getTargetRegister().equals("R"+d)){
				// put result in Rd anyway
				tr += "Move "+operand1.getTargetRegister()+",R"+d+"\n"; 
			}
			break;
		case DIVIDE:
			// check division by zero
			if (operand2.getLIRInstType() != LIRFlagEnum.REGISTER){ // operand2 must be in a register
				String targetReg = operand1.getTargetRegister().equals("R"+d) ?	"R"+(d+1) : "R"+d;
				// move it into the register
				tr += getMoveCommand(operand2.getLIRInstType());
				tr += operand2.getTargetRegister()+","+targetReg+"\n";
				// update target register
				operand2.setTargetRegister(targetReg);
				operand2.setLIRInstType(LIRFlagEnum.REGISTER);
			}
			tr += "StaticCall __checkZero(b="+operand2.getTargetRegister()+"),Rdummy\n";
			
			tr += "Div "+operand2.getTargetRegister()+","+operand1.getTargetRegister()+"\n";
			if (!operand1.getTargetRegister().equals("R"+d)){
				// put result in Rd anyway
				tr += "Move "+operand1.getTargetRegister()+",R"+d+"\n"; 
			}
			break;
		case MOD:
			// check division by zero
			if (operand2.getLIRInstType() != LIRFlagEnum.REGISTER){ // operand2 must be in a register
				String targetReg = operand1.getTargetRegister().equals("R"+d) ?	"R"+(d+1) : "R"+d;
				// move it into the register
				tr += getMoveCommand(operand2.getLIRInstType());
				tr += operand2.getTargetRegister()+","+targetReg+"\n";
				// update target register
				operand2.setTargetRegister(targetReg);
				operand2.setLIRInstType(LIRFlagEnum.REGISTER);
			}
			tr += "StaticCall __checkZero(b="+operand2.getTargetRegister()+"),Rdummy\n";
			
			tr += "Mod "+operand2.getTargetRegister()+","+operand1.getTargetRegister()+"\n";
			if (!operand1.getTargetRegister().equals("R"+d)){
				// put result in Rd anyway
				tr += "Move "+operand1.getTargetRegister()+",R"+d+"\n"; 
			}
			break;
		default:
			System.err.println("error in translate");
		}
		
		return new LIRUpType(tr, LIRFlagEnum.REGISTER,"R"+d);
	}

	/**
	 * LogicalBinaryOp propagating visitor:
	 * - translate recursively the operator and operands
	 * - return the LIR code
	 */
	public LIRUpType visit(LogicalBinaryOp binaryOp, Integer d){
		String trueLabel = "_true_label"+labelCounter;
		String falseLabel = "_false_label"+labelCounter;
		String endLabel = "_end_label"+(labelCounter++);
		String tr = "";
		// decide which block will be first by the Setti-Ullman algorithm
		boolean suBool = binaryOp.getFirstOperand().getRequiredRegs() >= binaryOp.getSecondOperand().getRequiredRegs(); 
		LIRUpType operand1 = null;
		LIRUpType operand2 = null;
		
		if (suBool){
			// recursive call to operands
			operand1 = binaryOp.getFirstOperand().accept(this, d);
			tr += operand1.getLIRCode();
			// if the received value is not already a register, place it into one
			if (operand1.getLIRInstType() != LIRFlagEnum.REGISTER){
				tr += getMoveCommand(operand1.getLIRInstType());
				tr += operand1.getTargetRegister()+",R"+d+"\n";
				// update target register
				operand1.setTargetRegister("R"+d);
				operand1.setLIRInstType(LIRFlagEnum.REGISTER);
			};
			
			operand2 = binaryOp.getSecondOperand().accept(this, d+1);
			tr += operand2.getLIRCode();
			if (operand2.getLIRInstType() == LIRFlagEnum.ARR_LOCATION ||
					operand2.getLIRInstType() == LIRFlagEnum.EXT_VAR_LOCATION){			
				tr += getMoveCommand(operand2.getLIRInstType());
				tr += operand2.getTargetRegister()+",R"+(d+1)+"\n";
				// update target register
				operand2.setTargetRegister("R"+(d+1));
				operand2.setLIRInstType(LIRFlagEnum.REGISTER);
			}
		} else {
			// recursive call to operands
			operand2 = binaryOp.getSecondOperand().accept(this, d);
			tr += operand2.getLIRCode();
			if (operand2.getLIRInstType() == LIRFlagEnum.ARR_LOCATION ||
					operand2.getLIRInstType() == LIRFlagEnum.EXT_VAR_LOCATION){
				tr += getMoveCommand(operand2.getLIRInstType());
				tr += operand2.getTargetRegister()+",R"+d+"\n";
				// update target register
				operand2.setTargetRegister("R"+d);
				operand2.setLIRInstType(LIRFlagEnum.REGISTER);
			}
			
			operand1 = binaryOp.getFirstOperand().accept(this, d+1);
			tr += operand1.getLIRCode();
			// if the received value is not already a register, place it into one
			if (operand1.getLIRInstType() != LIRFlagEnum.REGISTER){
				tr += getMoveCommand(operand1.getLIRInstType());
				tr += operand1.getTargetRegister()+",R"+(d+1)+"\n";
				// update target register
				operand1.setTargetRegister("R"+(d+1));
				operand1.setLIRInstType(LIRFlagEnum.REGISTER);
			}
		}
		
		
		// operation
		if (binaryOp.getOperator() != BinaryOps.LAND && binaryOp.getOperator() != BinaryOps.LOR){
			tr += suBool ?
					"Compare "+operand2.getTargetRegister()+","+operand1.getTargetRegister()+"\n":
					"Compare "+operand2.getTargetRegister()+","+operand1.getTargetRegister()+"\n";
		}
		switch (binaryOp.getOperator()){
		case EQUAL:
			tr += "JumpTrue "+trueLabel+"\n";
			break;
		case NEQUAL:
			tr += "JumpFalse "+trueLabel+"\n";
			break;
		case GT:
			tr += "JumpG "+trueLabel+"\n";
			break;
		case GTE:
			tr += "JumpGE "+trueLabel+"\n";
			break;
		case LT:
			tr += "JumpL "+trueLabel+"\n";
			break;
		case LTE:
			tr += "JumpLE "+trueLabel+"\n";
			break;
		case LAND:
			tr += "Compare 0,"+operand1.getTargetRegister()+"\n";
			tr += "JumpTrue "+falseLabel+"\n";
			// if operand2 not in register, put it in the free one
			if (operand2.getLIRInstType() != LIRFlagEnum.REGISTER){
				String targetReg = operand1.getTargetRegister().equals("R"+d) ?	"R"+(d+1) : "R"+d;
				// move it into the register
				tr += getMoveCommand(operand2.getLIRInstType());
				tr += operand2.getTargetRegister()+","+targetReg+"\n";
				// update target register
				operand2.setTargetRegister(targetReg);
				operand2.setLIRInstType(LIRFlagEnum.REGISTER);
			}
			tr += "Compare 0,"+operand2.getTargetRegister()+"\n";
			tr += "JumpTrue "+falseLabel+"\n";
			tr += "Jump "+trueLabel+"\n";
			tr += falseLabel+":\n"; 
			break;
		case LOR:
			tr += "Compare 0,"+operand1.getTargetRegister()+"\n";
			tr += "JumpFalse "+trueLabel+"\n";
			// if operand2 not in register, put it in the free one
			if (operand2.getLIRInstType() != LIRFlagEnum.REGISTER){
				String targetReg = operand1.getTargetRegister().equals("R"+d) ?	"R"+(d+1) : "R"+d;
				// move it into the register
				tr += getMoveCommand(operand2.getLIRInstType());
				tr += operand2.getTargetRegister()+","+targetReg+"\n";
				// update target register
				operand2.setTargetRegister(targetReg);
				operand2.setLIRInstType(LIRFlagEnum.REGISTER);
			}
			tr += "Compare 0,"+operand2.getTargetRegister()+"\n";
			tr += "JumpFalse "+trueLabel+"\n"; 
			break;
		default:
			System.err.println("error in translate");	
		}
		tr += "Move 0,R"+d+"\n";
		tr += "Jump "+endLabel+"\n";
		tr += trueLabel+":\n";
		tr += "Move 1,R"+d+"\n";
		tr += endLabel+":\n";
		
		return new LIRUpType(tr, LIRFlagEnum.REGISTER,"R"+d);
	}

	/**
	 * MathUnaryOp propagating visitor:
	 * - translate the operation
	 * - return the LIR code
	 */
	public LIRUpType visit(MathUnaryOp unaryOp, Integer d){
		String tr = "";
		
		// recursive call to operand
		LIRUpType operand = unaryOp.getOperand().accept(this, d);
		tr += operand.getLIRCode();
		
		// if the received value is not already a register, place it into one
		if (operand.getLIRInstType() != LIRFlagEnum.REGISTER){
			tr += getMoveCommand(operand.getLIRInstType());
			tr += operand.getTargetRegister()+",R"+d+"\n";
		} // else it is already in register Rd
		
		tr += "Neg R"+d+"\n";
		return new LIRUpType(tr, LIRFlagEnum.REGISTER,"R"+d);
	}

	/**
	 * LogicalUnaryOp propagating visitor:
	 * - translate the operation
	 * - return the LIR code
	 */
	public LIRUpType visit(LogicalUnaryOp unaryOp, Integer d){
		String tr = "";
		String trueLabel = "_true_label"+labelCounter;
		String endLabel = "_end_label"+(labelCounter++);
		
		// recursive call to operand
		LIRUpType operand = unaryOp.getOperand().accept(this, d);
		tr += operand.getLIRCode();
		
		// if the received value is not already a register, place it into one
		if (operand.getLIRInstType() != LIRFlagEnum.REGISTER){
			tr += getMoveCommand(operand.getLIRInstType());
			tr += operand.getTargetRegister()+",R"+d+"\n";
		} // else it is already in register Rd
		
		tr += "Compare 0,R"+d+"\n";
		tr += "JumpTrue "+trueLabel+"\n";
		tr += "Move 0,R"+d+"\n";
		tr += "Jump "+endLabel+"\n";
		tr += trueLabel+":\n";
		tr += "Move 1,R"+d+"\n";
		tr += endLabel+":\n";
		
		return new LIRUpType(tr, LIRFlagEnum.REGISTER,"R"+d);
	}

	/**
	 * Literal propagating visitor:
	 * - translate the literal expression
	 * - return the LIR code
	 */
	public LIRUpType visit(Literal literal, Integer d){
		String litStr = "";
		
		switch (literal.getType()){
		case STRING:
			String strVal = ((String) literal.getValue()).replaceAll("\n", "\\\\n");
			if (!stringLiterals.containsKey(strVal))
				stringLiterals.put(strVal, "str"+(stringLiteralsCounter++));
			litStr = stringLiterals.get(strVal);
			break;
		case INTEGER:
			litStr = literal.getValue().toString();
			break;
		case NULL:
			litStr = "0";
			break;
		case FALSE:
			litStr = "0";
			break;
		case TRUE:
			litStr = "1";
		}
		
		return new LIRUpType("", LIRFlagEnum.LITERAL,litStr);
	}

	/**
	 * ExpressionBlock propagating visitor:
	 * translate the expression in the block
	 * - return the LIR code
	 */
	public LIRUpType visit(ExpressionBlock expressionBlock, Integer d){
		return expressionBlock.accept(this, d);
	}
	
	// getters and setters
	//////////////////////

	public int getStringLiteralsCounter() {
		return stringLiteralsCounter;
	}

	public void setStringLiteralsCounter(int stringLiteralsCounter) {
		this.stringLiteralsCounter = stringLiteralsCounter;
	}

	public Map<String,String> getStringLiterals() {
		return stringLiterals;
	}

	public void setStringLiterals(HashMap<String,String> stringLiterals) {
		this.stringLiterals = stringLiterals;
	}

	public List<String> getClassDispatchTable() {
		return classDispatchTable;
	}

	public void setClassDispatchTable(List<String> classDispatchTable) {
		this.classDispatchTable = classDispatchTable;
	}

	public List<String> getMethods() {
		return methods;
	}

	public void setMethods(List<String> methods) {
		this.methods = methods;
	}

	public String getMainMethod() {
		return mainMethod;
	}

	public void setMainMethod(String mainMethod) {
		this.mainMethod = mainMethod;
	}
	
}
