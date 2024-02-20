package miniJava.SyntacticAnalyzer;

public class Token {
	private TokenType _type;
	private String _text;
	
	public Token(TokenType type, String text) {
		_type = type;
		_text = text;
	}

	public SourcePosition getTokenPosition() {
		return null;
	}
	public TokenType getTokenType() {
		return _type;
	}
	
	public String getTokenText() {
		return _text;
	}
}
