package miniJava.SyntacticAnalyzer;

import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.ErrorReporter;
import static miniJava.SyntacticAnalyzer.TokenType.*;

//save type id = expression, ref =exp, ref[exp] = exp, ref (
// ref[exp] if there's something

public class Parser {
	private Scanner _scanner;
	private ErrorReporter _errors;
	private Token _currentToken;
	
	public Parser( Scanner scanner, ErrorReporter errors ) {
		this._scanner = scanner;
		this._errors = errors;
		this._currentToken = this._scanner.scan();
	}
	
	class SyntaxError extends Error {
		private static final long serialVersionUID = -6461942006097999362L;
	}
	
	public AST parse() {
		AST tree = null;
		try {
			tree = parseProgram();
		} catch( SyntaxError e ) {
		}
		return tree;
	}
	
	private Package parseProgram() throws SyntaxError {
		ClassDeclList cdl = new ClassDeclList();
		while (_currentToken.getTokenType() != EOF) {
			cdl.add(parseClassDeclaration());
		}
		return new Package(cdl, null);
	}
	
	private ClassDecl parseClassDeclaration() throws SyntaxError {

		accept(CLASS);
		String id = accept(IDENTIFIER).getTokenText();
		accept(LCURLY);
		FieldDeclList fdl = new FieldDeclList();
		MethodDeclList mdl = new MethodDeclList();
		while(_currentToken.getTokenType() != RCURLY) {
			MemberDecl md = parseMemberDeclaration();
			if(md instanceof FieldDecl) {
				fdl.add((FieldDecl) md);
			} else if(md instanceof MethodDecl) {
				mdl.add((MethodDecl) md);
			}
		}
		accept(RCURLY);
		return new ClassDecl(id, fdl, mdl, null);
	}

	private MemberDecl parseMemberDeclaration() throws SyntaxError {
		boolean canBeField = true;
		boolean isPrivate = false;
		boolean isStatic = false;
		if(_currentToken.getTokenType() == VISIBILITY) {
			String vis = accept(VISIBILITY).getTokenText();
			if(vis.equals("private")) {
				isPrivate = true;
			} else if(vis.equals("public")) {
				isPrivate = false;
			} else {
				_errors.reportError("Syntax Error");
				throw new SyntaxError();
			}
		}
		if(_currentToken.getTokenType() == STATIC) {
			accept(STATIC);
			isStatic = true;
		}

		TypeDenoter type = null;
		if(_currentToken.getTokenType() == VOID) {
			accept(VOID);
			canBeField = false;
			type = new BaseType(TypeKind.VOID, null);
		} else {
			type = parseType();
		}

		String id = accept(IDENTIFIER).getTokenText();
		FieldDecl fd = new FieldDecl(isPrivate, isStatic, type, id, null);

		if((_currentToken.getTokenType() == SEMICOLON) && canBeField) {
			accept(SEMICOLON);
			return fd;
		} else {
			ParameterDeclList params = parseParameters();
			accept(LCURLY);
			StatementList sl = new StatementList();
			while(_currentToken.getTokenType() != RCURLY) {
				sl.add(parseStatement());
			}
			accept(RCURLY);
			return new MethodDecl(fd, params, sl, null);
		}
	}

	private TypeDenoter parseType() throws SyntaxError {
		if(_currentToken.getTokenType() == INT) {
			accept(INT);
			TypeDenoter type = new BaseType(TypeKind.INT, null);
			if(_currentToken.getTokenType() == BRACKETS) {
				accept(BRACKETS);
				return new ArrayType(type, null);
			} else {
				return type;
			}
		} else if (_currentToken.getTokenType() == BOOLEAN) {
			accept(BOOLEAN);
			return new BaseType(TypeKind.BOOLEAN, null);
		} else if (_currentToken.getTokenType() == IDENTIFIER) {
			TypeDenoter type = new ClassType(new Identifier(accept(IDENTIFIER)), null);
			if(_currentToken.getTokenType() == BRACKETS) {
				accept(BRACKETS);
				return new ArrayType(type, null);
			} else {
				return type;
			}
		} else {
			_errors.reportError("Syntax Error - expected valid type");
			throw new SyntaxError();
		}
	}

	private ParameterDeclList parseParameters() throws SyntaxError {
		ParameterDeclList params = new ParameterDeclList();
		accept(LPAREN);
		if(_currentToken.getTokenType() != RPAREN) {
			TypeDenoter type = parseType();
			String id = accept(IDENTIFIER).getTokenText();
			params.add(new ParameterDecl(type, id, null));
		}
		while(_currentToken.getTokenType() == COMMA) {
			accept(COMMA);
			TypeDenoter type = parseType();
			String id = accept(IDENTIFIER).getTokenText();
			params.add(new ParameterDecl(type, id, null));
		}
		accept(RPAREN);
		return params;
	}

	private ExprList parseArguments() throws SyntaxError {
		accept(LPAREN);
		ExprList el = new ExprList();
		if(_currentToken.getTokenType() != RPAREN) {
			el.add(parseExpression());
		}
		while(_currentToken.getTokenType() == COMMA) {
			accept(COMMA);
			el.add(parseExpression());
		}
		accept(RPAREN);
		return el;
	}

	private Statement parseStatement() throws SyntaxError {
		if(_currentToken.getTokenType() == LCURLY) {
			accept(LCURLY);
			StatementList sl = new StatementList();
			while(_currentToken.getTokenType() != RCURLY) {
				sl.add(parseStatement());
			}
			accept(RCURLY);
			return new BlockStmt(sl, null);
		}

		else if(_currentToken.getTokenType() == RETURN) {
			accept(RETURN);
			Expression exp = null;
			if(_currentToken.getTokenType() == SEMICOLON) {
				accept(SEMICOLON);
			} else {
				exp = parseExpression();
				accept(SEMICOLON);
			}
			return new ReturnStmt(exp, null);
		}

		else if(_currentToken.getTokenType() == IF) {
			accept(IF);
			accept(LPAREN);
			Expression exp = parseExpression();
			accept(RPAREN);
			Statement thenst = parseStatement();
			if(_currentToken.getTokenType() == ELSE) {
				accept(ELSE);
				Statement elsest = parseStatement();
				return new IfStmt(exp, thenst, elsest, null);
			}
			return new IfStmt(exp, thenst, null);
		}

		else if(_currentToken.getTokenType() == WHILE) {
			accept(WHILE);
			accept(LPAREN);
			Expression exp = parseExpression();
			accept(RPAREN);
			Statement st = parseStatement();
			return new WhileStmt(exp, st, null);
		}
		else {
			AST next = parseTypeOrRef();
			if (next instanceof TypeDenoter) {
				TypeDenoter type = (TypeDenoter) next;
				String id = accept(IDENTIFIER).getTokenText();
				accept(EQUALS);
				Expression exp = parseExpression();
				accept(SEMICOLON);
				VarDecl vd = new VarDecl(type, id, null);
				return new VarDeclStmt(vd, exp, null);
			} else if(next instanceof Reference) {                            //it was a reference otherwise
				Reference ref = (Reference) next;
				if (_currentToken.getTokenType() == EQUALS) {
					accept(EQUALS);
					Expression exp = parseExpression();
					accept(SEMICOLON);
					return new AssignStmt(ref, exp, null);
				} else if (_currentToken.getTokenType() == LBRACKET) {
					accept(LBRACKET);
					Expression inside = parseExpression();
					accept(RBRACKET);
					accept(EQUALS);
					Expression outside = parseExpression();
					accept(SEMICOLON);
					return new IxAssignStmt(ref, inside, outside, null);
				} else if (_currentToken.getTokenType() == LPAREN) {
					ExprList args = parseArguments();
					accept(SEMICOLON);
					return new CallStmt(ref, args, null);
				} else {
					_errors.reportError("Syntax Error");
					throw new SyntaxError();
				}
			} else {
				_errors.reportError("Syntax Error - Should not be reachable");
				throw new SyntaxError();
			}
		}
	}

	private AST parseTypeOrRef() throws SyntaxError {	// where it could be a type or reference
		if(_currentToken.getTokenType() == INT
				||_currentToken.getTokenType() == BOOLEAN )	{
			return parseType();
		} else if (_currentToken.getTokenType() == THIS) {
			return parseReference();
		} else {
			Identifier id = new Identifier(accept(IDENTIFIER));
			if(_currentToken.getTokenType() == BRACKETS) {
				accept(BRACKETS);
				return new ArrayType(new ClassType(id, null), null);
			} else if (_currentToken.getTokenType() == IDENTIFIER) {
				return new ClassType(id, null);
			}
			else if (_currentToken.getTokenType() == PERIOD) {
				Reference ref = new IdRef(id, null);
				while(_currentToken.getTokenType() == PERIOD) {
					accept(PERIOD);
					ref = new QualRef(ref, new Identifier(accept(IDENTIFIER)), null);
				}
				return ref;
			} else {
				return new IdRef(id,null);
			}
		}
	}

	private Reference parseReference() throws SyntaxError {
		Reference reference;
		if(_currentToken.getTokenType() == THIS) {
			accept(THIS);
			reference = new ThisRef(null);
		} else {
			reference = new IdRef(new Identifier(accept(IDENTIFIER)), null);
		}
		while(_currentToken.getTokenType() == PERIOD) {
			accept(PERIOD);
			reference = new QualRef(reference, new Identifier(accept(IDENTIFIER)), null);
		}
		return reference;
	}

	private Expression parseExpression() {
		return parseDisjunction();
	}

	private Expression parseDisjunction() {
		Expression e1 = parseConjunction();
		while (_currentToken.getTokenText().equals("||")) {
			Operator op = new Operator(accept(BINOP));
			Expression e2 = parseConjunction();
			e1 = new BinaryExpr(op, e1, e2, null);
		}
		return e1;
	}
	private Expression parseConjunction() {
		Expression e1 = parseEquality();
		while (_currentToken.getTokenText().equals("&&")) {
			Operator op = new Operator(accept(BINOP));
			Expression e2 = parseEquality();
			e1 = new BinaryExpr(op, e1, e2, null);
		}
		return e1;
	}
	private Expression parseEquality() {
		Expression e1 = parseRelational();
		while (_currentToken.getTokenText().equals("==")
			|| _currentToken.getTokenText().equals("!="))
		{
			Operator op = new Operator(accept(BINOP));
			Expression e2 = parseRelational();
			e1 = new BinaryExpr(op, e1, e2, null);
		}
		return e1;
	}
	private Expression parseRelational() {
		Expression e1 = parseAdditive();
		while (_currentToken.getTokenText().equals("<")
			|| _currentToken.getTokenText().equals(">")
			|| _currentToken.getTokenText().equals("<=")
			|| _currentToken.getTokenText().equals(">="))

		{
			Operator op = new Operator(accept(BINOP));
			Expression e2 = parseAdditive();
			e1 = new BinaryExpr(op, e1, e2, null);
		}
		return e1;
	}
	private Expression parseAdditive() {
		Expression e1 = parseMultiplicative();
		while (_currentToken.getTokenText().equals("+")
			|| _currentToken.getTokenText().equals("-"))
		{
			Operator op;
			if(_currentToken.getTokenType() == MINUS) {
				op = new Operator(accept(MINUS));
			} else {
				op = new Operator(accept(BINOP));
			}
			Expression e2 = parseMultiplicative();
			e1 = new BinaryExpr(op, e1, e2, null);
		}
		return e1;
	}
	private Expression parseMultiplicative() {
		Expression e1 = parseNonBinaryExpression();
		while (_currentToken.getTokenText().equals("*")
			|| _currentToken.getTokenText().equals("/"))
		{
			Operator op = new Operator(accept(BINOP));
			Expression e2 = parseNonBinaryExpression();
			e1 = new BinaryExpr(op, e1, e2, null);
		}
		return e1;
	}

	private Expression parseNonBinaryExpression() throws SyntaxError {
		if(_currentToken.getTokenType() == BINOP) {
			_errors.reportError("Syntax Error - expected expression");
			throw new SyntaxError();
		}

		if(_currentToken.getTokenType() == UNOP) {
			Operator op = new Operator(accept(UNOP));
			return new UnaryExpr(op, parseNonBinaryExpression(), null);
		}

		else if(_currentToken.getTokenType() == MINUS) {	//Same as UNOP, because it is one
			Operator op = new Operator(accept(MINUS));
			return new UnaryExpr(op, parseNonBinaryExpression(), null);
		}

		else if(_currentToken.getTokenType() == LPAREN) {
			accept(LPAREN);
			Expression exp = parseExpression();
			accept(RPAREN);
			return exp;
		}

		else if(_currentToken.getTokenType() == INTLITERAL) {
			Terminal t = new IntLiteral(accept(INTLITERAL));
			return new LiteralExpr(t, null);
		}

		else if(_currentToken.getTokenType() == BOOLLITERAL) {
			Terminal t = new BooleanLiteral(accept(BOOLLITERAL));
			return new LiteralExpr(t, null);
		}

		else if(_currentToken.getTokenType() == NEW) {
			accept(NEW);

			if(_currentToken.getTokenType() == IDENTIFIER) {
				ClassType type = new ClassType(new Identifier(accept(IDENTIFIER)),null);

				if(_currentToken.getTokenType() == LBRACKET) {
					accept(LBRACKET);
					Expression exp = parseExpression();
					accept(RBRACKET);
					return new NewArrayExpr(type, exp,null);
				}

				else if(_currentToken.getTokenType() == LPAREN) {
					accept(LPAREN);
					accept(RPAREN);
					return new NewObjectExpr(type, null);
				}

				else {
					_errors.reportError("Syntax Error - expected ( or [");
					throw new SyntaxError();
				}
			}

			else if(_currentToken.getTokenType() == INT) {
				accept(INT);
				accept(LBRACKET);
				Expression exp = parseExpression();
				accept(RBRACKET);
				BaseType type = new BaseType(TypeKind.INT, null);
				return new NewArrayExpr(type, exp, null);
			}

			else {
				_errors.reportError("Syntax Error");
				throw new SyntaxError();
			}
		}

		// REF CASES
		else if(_currentToken.getTokenType() == IDENTIFIER
				|| _currentToken.getTokenType() == THIS) {
			Reference ref = parseReference();
			if(_currentToken.getTokenType() == LBRACKET) {
				accept(LBRACKET);
				Expression exp = parseExpression();
				accept(RBRACKET);
				return new IxExpr(ref, exp, null);
			}

			else if(_currentToken.getTokenType() == LPAREN) {
				ExprList args = parseArguments();
				return new CallExpr(ref, args, null);
			} else {
				return new RefExpr(ref, null);
			}
		} else {
			_errors.reportError("Syntax Error");
			throw new SyntaxError();
		}
	}

	// This method will accept the token and retrieve the next token.
	//  Can be useful if you want to error check and accept all-in-one.
	private Token accept(TokenType expectedType) throws SyntaxError {
		Token t = _currentToken;
		if(_currentToken.getTokenType() == expectedType) {
			_currentToken = _scanner.scan();
		} else if(_currentToken.getTokenType() == INVALID_TOKEN) {
			throw new SyntaxError();	// message already reported in scanner
		} else {
			_errors.reportError("Syntax Error - Expected token " + expectedType
					+ " but got " + _currentToken.getTokenType());
			throw new SyntaxError();
		}
		return t;
	}
}
