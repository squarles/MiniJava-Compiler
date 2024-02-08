package miniJava.SyntacticAnalyzer;

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
	
	public void parse() {
		try {
			parseProgram();
		} catch( SyntaxError e ) {
		}
	}
	
	private void parseProgram() throws SyntaxError {
		while (_currentToken.getTokenType() != EOF) {
			parseClassDeclaration();
		}
	}
	
	private void parseClassDeclaration() throws SyntaxError {

		accept(CLASS);
		accept(IDENTIFIER);
		accept(LCURLY);
		while(_currentToken.getTokenType() != RCURLY) {
			parseFieldOrMethodDeclaration();
		}

		accept(RCURLY);
	}

	private void parseFieldOrMethodDeclaration() throws SyntaxError {
		boolean canBeField = true;
		if(_currentToken.getTokenType() == VISIBILITY) {
			accept(VISIBILITY);
		}
		if(_currentToken.getTokenType() == STATIC) {
			accept(STATIC);
		}
		if(_currentToken.getTokenType() == IDENTIFIER) {
			accept(IDENTIFIER);
		}

		if(_currentToken.getTokenType() == VOID) {
			accept(VOID);
			canBeField = false;
		} else {
			parseType();
		}

		accept(IDENTIFIER);
		if((_currentToken.getTokenType() == SEMICOLON) && canBeField) {
			accept(SEMICOLON);
			return;
		} else {
			parseParameters();
			accept(LCURLY);
			while(_currentToken.getTokenType() != RCURLY) {
				parseStatement();
			}
			accept(RCURLY);
		}
	}

	private void parseType() throws SyntaxError {
		if(_currentToken.getTokenType() == INT) {
			accept(INT);
			if(_currentToken.getTokenType() == BRACKETS) {
				accept(BRACKETS);
			}
		} else if (_currentToken.getTokenType() == BOOLEAN) {
			accept(BOOLEAN);
		} else if (_currentToken.getTokenType() == IDENTIFIER) {
			accept(IDENTIFIER);
			if(_currentToken.getTokenType() == BRACKETS) {
				accept(BRACKETS);
			}
		} else {
			_errors.reportError("Syntax Error - expected valid type");
			throw new SyntaxError();
		}
	}

	private void parseParameters() throws SyntaxError {
		accept(LPAREN);
		if(_currentToken.getTokenType() != RPAREN) {
			parseType();
			accept(IDENTIFIER);
		}
		while(_currentToken.getTokenType() == COMMA) {
			accept(COMMA);
			parseType();
			accept(IDENTIFIER);
		}
		accept(RPAREN);
	}

	private void parseArguments() throws SyntaxError {
		accept(LPAREN);
		if(_currentToken.getTokenType() != RPAREN) {
			parseExpression();
		}
		while(_currentToken.getTokenType() == COMMA) {
			accept(COMMA);
			parseExpression();
		}
		accept(RPAREN);
	}

	private void parseStatement() throws SyntaxError {

		if(_currentToken.getTokenType() == LCURLY) {
			accept(LCURLY);
			parseStatement();
			accept(RCURLY);
		}

		else if(_currentToken.getTokenType() == RETURN) {
			accept(RETURN);
			if(_currentToken.getTokenType() == SEMICOLON) {
				accept(SEMICOLON);
				return;
			} else {
				parseExpression();
				accept(SEMICOLON);
			}
		}

		else if(_currentToken.getTokenType() == IF) {
			accept(IF);
			accept(LPAREN);
			parseExpression();
			accept(RPAREN);
			parseStatement();
			if(_currentToken.getTokenType() == ELSE) {
				accept(ELSE);
				parseStatement();
			}
		}

		else if(_currentToken.getTokenType() == WHILE) {
			accept(WHILE);
			accept(LPAREN);
			parseExpression();
			accept(RPAREN);
			parseStatement();
		}

		else if(parseCheckForType()) {	//parse something and check if it was a type
			accept(IDENTIFIER);
			accept(EQUALS);
			parseExpression();
			accept(SEMICOLON);
		}

		else {							//it was a reference otherwise
			if(_currentToken.getTokenType() == EQUALS) {
				accept(EQUALS);
				parseExpression();
				accept(SEMICOLON);
			} else if (_currentToken.getTokenType() == LBRACKET) {
				accept(LBRACKET);
				parseExpression();
				accept(RBRACKET);
				accept(EQUALS);
				parseExpression();
				accept(SEMICOLON);
			} else if (_currentToken.getTokenType() == LPAREN) {
				parseArguments();
				accept(SEMICOLON);
			} else {
				_errors.reportError("Syntax Error");
				throw new SyntaxError();
			}
		}
	}

	private boolean parseCheckForType() throws SyntaxError {		// parse a type and return true
		if(_currentToken.getTokenType() == INT	// or parse a ref and return false
				||_currentToken.getTokenType() == BOOLEAN )	{
			parseType();
			return true;
		} else if (_currentToken.getTokenType() == THIS) {
			parseReference();
			return false;
		} else {
			accept(IDENTIFIER);
			if(_currentToken.getTokenType() == BRACKETS) {
				accept(BRACKETS);
				return true;
			} else if (_currentToken.getTokenType() == IDENTIFIER) {
				return true;
			}
			else if (_currentToken.getTokenType() == PERIOD) {
				while(_currentToken.getTokenType() == PERIOD) {
					accept(PERIOD);
					accept(IDENTIFIER);
				}
				return false;
			}
		}
		_errors.reportError("Syntax Error - expected type or reference");
		throw new SyntaxError();
	}

	private void parseReference() throws SyntaxError {
		if(_currentToken.getTokenType() == THIS) {
			accept(THIS);
		} else {
			accept(IDENTIFIER);
		}
		while(_currentToken.getTokenType() == PERIOD) {
			accept(PERIOD);
			accept(IDENTIFIER);
		}
	}

	private void parseExpression() throws SyntaxError {
		if(_currentToken.getTokenType() == UNOP) {
			accept(UNOP);
			parseExpression();
		}

		else if(_currentToken.getTokenType() == MINUS) {	//Same as UNOP, because it is one
			accept(MINUS);
			parseExpression();
		}

		else if(_currentToken.getTokenType() == LPAREN) {
			accept(LPAREN);
			parseExpression();
			accept(RPAREN);
		}

		else if(_currentToken.getTokenType() == INTLITERAL) {
			accept(INTLITERAL);
		}

		else if(_currentToken.getTokenType() == BOOLLITERAL) {
			accept(BOOLLITERAL);
		}

		else if(_currentToken.getTokenType() == NEW) {
			accept(NEW);

			if(_currentToken.getTokenType() == IDENTIFIER) {
				accept(IDENTIFIER);

				if(_currentToken.getTokenType() == LBRACKET) {
					accept(LBRACKET);
					parseExpression();
					accept(RBRACKET);
				}

				else if(_currentToken.getTokenType() == LPAREN) {
					accept(LPAREN);
					accept(RPAREN);
				}

				else {
					_errors.reportError("Syntax Error - expected ( or [");
					throw new SyntaxError();
				}
			}

			else if(_currentToken.getTokenType() == INT) {
				accept(INT);
				accept(LBRACKET);
				parseExpression();
				accept(RBRACKET);
			}

			else {
				_errors.reportError("Syntax Error");
				throw new SyntaxError();
			}
		}

		// REF CASES
		else if(_currentToken.getTokenType() == IDENTIFIER
				|| _currentToken.getTokenType() == THIS) {
			parseReference();
			if(_currentToken.getTokenType() == LBRACKET) {
				accept(LBRACKET);
				parseExpression();
				accept(RBRACKET);
			}

			else if(_currentToken.getTokenType() == LPAREN) {
				parseArguments();
			}
		}

		//EXP BINOP EXP
		if(_currentToken.getTokenType() == BINOP) {
			accept(BINOP);
			parseExpression();
		} else if(_currentToken.getTokenType() == MINUS) {
			accept(MINUS);
			parseExpression();
		}
	}

	// This method will accept the token and retrieve the next token.
	//  Can be useful if you want to error check and accept all-in-one.
	private void accept(TokenType expectedType) throws SyntaxError {
		if(_currentToken.getTokenType() == expectedType) {
			_currentToken = _scanner.scan();
			return;
		} else if(_currentToken.getTokenType() == INVALID_TOKEN) {
			throw new SyntaxError();	// message already reported in scanner
		} else {
			_errors.reportError("Syntax Error - Expected token " + expectedType
					+ " but got " + _currentToken.getTokenType());
			throw new SyntaxError();
		}
	}
}
