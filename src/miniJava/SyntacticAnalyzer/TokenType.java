package miniJava.SyntacticAnalyzer;

public enum TokenType {

    IDENTIFIER,

    CLASS,
    VISIBILITY,
    STATIC,

    INT,
    INTLITERAL,
    BOOLEAN,
    BOOLLITERAL,    // "true", "false"
    VOID,

    THIS,
    IF,
    ELSE,
    WHILE,
    RETURN,
    NEW,

    LPAREN,
    RPAREN,
    LBRACKET,
    RBRACKET,
    BRACKETS,       // Empty brackets - shortcut for parser
    LCURLY,
    RCURLY,
    SEMICOLON,
    COMMA,
    PERIOD,

    UNOP,
    BINOP,
    MINUS,
    EQUALS,         // SINGLE EQUALS - ASSIGNMENT

    EOF,

    NULL,

    INVALID_TOKEN   // Lexical Error
}
