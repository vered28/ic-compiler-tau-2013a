package IC.AST;

/** 
 * Visits a statement node with a given
 * context object (book-keeping) and returns the result
 * of the computation on this node.
 * 
 */ 
public interface PropagatingVisitor {

	public Object visit(Program program, Object context);

	public Object visit(ICClass icClass, Object context);

	public Object visit(Field field, Object context);

	public Object visit(VirtualMethod method, Object context);

	public Object visit(StaticMethod method, Object context);

	public Object visit(LibraryMethod method, Object context);

	public Object visit(Formal formal, Object context);

	public Object visit(PrimitiveType type, Object context);

	public Object visit(UserType type, Object context);

	public Object visit(Assignment assignment, Object context);

	public Object visit(CallStatement callStatement, Object context);

	public Object visit(Return returnStatement, Object context);

	public Object visit(If ifStatement, Object context);

	public Object visit(While whileStatement, Object context);

	public Object visit(Break breakStatement, Object context);

	public Object visit(Continue continueStatement, Object context);

	public Object visit(StatementsBlock statementsBlock, Object context);

	public Object visit(LocalVariable localVariable, Object context);

	public Object visit(VariableLocation location, Object context);

	public Object visit(ArrayLocation location, Object context);

	public Object visit(StaticCall call, Object context);

	public Object visit(VirtualCall call, Object context);

	public Object visit(This thisExpression, Object context);

	public Object visit(NewClass newClass, Object context);

	public Object visit(NewArray newArray, Object context);

	public Object visit(Length length, Object context);

	public Object visit(MathBinaryOp binaryOp, Object context);

	public Object visit(LogicalBinaryOp binaryOp, Object context);

	public Object visit(MathUnaryOp unaryOp, Object context);

	public Object visit(LogicalUnaryOp unaryOp, Object context);

	public Object visit(Literal literal, Object context);

	public Object visit(ExpressionBlock expressionBlock, Object context);


}
