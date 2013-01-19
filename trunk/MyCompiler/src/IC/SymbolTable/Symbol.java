package IC.SymbolTable;

import IC.TypeTable.*;

/**
 * The class for Symbol, a SymbolTable entry.
 *
 */
public class Symbol {
	
	private int line;
	private String id; //symbol name.
	private Kind kind;

	protected Type type = null;

	public Symbol(int line, String id, Kind kind, Type type) {
		this.line = line;
		this.kind = kind;
		this.id = id;
		this.type = type;
	}

	public String getID() {
		return this.id;
	}

	public Kind getKind() {
		return this.kind;
	}

	public Type getType() {
		return this.type;
	}

	@Override
	public String toString() {
		return getID(); //will be overridden.
	}

}
