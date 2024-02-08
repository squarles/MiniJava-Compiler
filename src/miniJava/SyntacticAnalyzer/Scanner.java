package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;
import miniJava.ErrorReporter;
import static miniJava.SyntacticAnalyzer.TokenType.*;

// return scan() to start over
// new stringBuilder to erase current text

//check comments > scan()
//if digit keep taking return intlit
//if AZaz take while AZaz09_, check is reserved - > return correct one
//opers
//easy steps

public class Scanner {
	private InputStream _in;
	private ErrorReporter _errors;
	private StringBuilder _currentText;
	private char _currentChar;
	
	public Scanner( InputStream in, ErrorReporter errors ) {
		this._in = in;
		this._errors = errors;
		this._currentText = new StringBuilder();
		
		nextChar();
	}
	
	public Token scan() {

		while(is_Whitespace(_currentChar)) {
			skipIt();
		}

		if(_currentChar == '/') {
			takeIt();
			if (_currentChar == '/') {
				while(_currentChar != '\n') {
					takeIt();
				}
				_currentText = new StringBuilder();
				return scan();
			}
			if (_currentChar == '*') {
				while(_currentChar != -1) {
					if (_currentChar == '*'){
						takeIt();
						if (_currentChar == '/'){
							takeIt();
							break;
						}
					}
					else {
						takeIt();
					}
				}
				return scan();
			}
			else {
				return makeToken(BINOP);
			}
		}

		else if(is_Digit(_currentChar))  {
			takeIt();
			while(is_Digit(_currentChar)) {
				takeIt();
			}
			return makeToken(TokenType.INTLITERAL);
		}

		else if(is_Letter(_currentChar)) {
			takeIt();
			while (is_AlphaNumeric(_currentChar)) {
				takeIt();
			}
			if (_currentText.toString().equals("class")) {
				return makeToken(CLASS);
			} else if (_currentText.toString().equals("public")
					|| _currentText.toString().equals("private")) {
				return makeToken(VISIBILITY);
			} else if (_currentText.toString().equals("static")) {
				return makeToken(STATIC);
			} else if (_currentText.toString().equals("void")) {
				return makeToken(VOID);
			} else if (_currentText.toString().equals("int")) {
				return makeToken(INT);
			} else if (_currentText.toString().equals("boolean")) {
				return makeToken(BOOLEAN);
			} else if (_currentText.toString().equals("this")) {
				return makeToken(THIS);
			} else if (_currentText.toString().equals("return")) {
				return makeToken(RETURN);
			} else if (_currentText.toString().equals("if")) {
				return makeToken(IF);
			} else if (_currentText.toString().equals("else")) {
				return makeToken(ELSE);
			} else if (_currentText.toString().equals("while")) {
				return makeToken(WHILE);
			} else if (_currentText.toString().equals("true")
					|| _currentText.toString().equals("false")) {
				return makeToken(BOOLLITERAL);
			} else {
				return makeToken(IDENTIFIER);
			}
		}

		else if(_currentChar == '=') {
			takeIt();
			if(_currentChar == '=') {
				takeIt();
				return makeToken(BINOP);
			} else {
				return makeToken(EQUALS);
			}
		}

		else if(_currentChar == '<' || _currentChar == '>') {
			takeIt();
			if(_currentChar == '=') {
				takeIt();
			}
			return makeToken(BINOP);
		}

		else if(_currentChar == '!') {
			takeIt();
			if(_currentChar == '=') {
				takeIt();
				return makeToken(BINOP);
			} else {
				return makeToken(UNOP);
			}
		}

		else if(_currentChar == '&') {
			takeIt();
			if(_currentChar == '&') {
				takeIt();
				return makeToken(BINOP);
			} else {
				System.out.println("Lexical Error");
				return null;
			}
		}

		else if(_currentChar == '|') {
			takeIt();
			if(_currentChar == '|') {
				takeIt();
				return makeToken(BINOP);
			} else {
				System.out.println("Lexical Error");
				return null;
			}
		}

		else if(_currentChar == '+') {
			takeIt();
			return makeToken(BINOP);
		}

		else if(_currentChar == '-') {
			takeIt();
			return makeToken(MINUS);
		}

		else if(_currentChar == '*') {
			takeIt();
			return makeToken(BINOP);
		}

		else if(_currentChar == '(') {
			takeIt();
			return makeToken(LPAREN);
		}

		else if(_currentChar == ')') {
			takeIt();
			return makeToken(RPAREN);
		}

		else if(_currentChar == '[') {
			takeIt();
			while(is_Whitespace(_currentChar)) {
				skipIt();
			}
			if(_currentChar == ']') {
				takeIt();
				return makeToken(BRACKETS);
			}
			return makeToken(LBRACKET);
		}

		else if(_currentChar == ']') {
			takeIt();
			return makeToken(RBRACKET);
		}

		else if(_currentChar == '{') {
			takeIt();
			return makeToken(LCURLY);
		}

		else if(_currentChar == '}') {
			takeIt();
			return makeToken(RCURLY);
		}

		else if(_currentChar == ';') {
			takeIt();
			return makeToken(SEMICOLON);
		}

		else if(_currentChar == ',') {
			takeIt();
			return makeToken(COMMA);
		}

		else if(_currentChar == '.') {
			takeIt();
			return makeToken(PERIOD);
		}

		else if(_currentChar == 0) {
			return null;
		}

		else {
			System.out.println("Lexical Error");
		}

		// TODO: Consider what happens if there is a comment (// or /* */)
		
		// TODO: What happens if there are no more tokens?
		
		// TODO: Determine what the token is. For example, if it is a number
		//  keep calling takeIt() until _currentChar is not a number. Then
		//  create the token via makeToken(TokenType.IntegerLiteral) and return it.
		return null;
	}
	
	private void takeIt() {
		_currentText.append(_currentChar);
		nextChar();
	}
	
	private void skipIt() {
		nextChar();
	}

	private boolean is_Whitespace(char c) {
		return (c == '\n' || c == '\r' || c == '\t' || c == ' ');
	}

	private boolean is_Letter(char c) {
		return ((c >= 'A' && c <= 'Z')
				|| (c >= 'a' && c <= 'z'));
	}

	private boolean is_Digit(char c) {
		return (c >= '0' && c <= '9');
	}

	private boolean is_AlphaNumeric(char c) {
		return ((c >= 'A' && c <= 'Z')
				|| (c >= 'a' && c <= 'z')
				|| (c >= '0' && c <= '9')
				|| c == '_');
	}

	private void nextChar() {
		try {
			int c = _in.read();
			_currentChar = (char)c;
			if (c > 255) {
				System.out.println("Lexical Error");
			}
			// TODO: What happens if c == -1? : EOF, return EOT or null
			if(c == -1) {
				_currentChar = 0;
			}
			
			// TODO: What happens if c is not a regular ASCII character? > 255 : error
			
		} catch( IOException e ) {
			System.out.println("File IO Error");
		}
	}
	
	private Token makeToken( TokenType toktype ) {
		String text = _currentText.toString();
		_currentText = new StringBuilder();
		System.out.print(text + " ");
		return new Token(toktype, text);
	}
}
