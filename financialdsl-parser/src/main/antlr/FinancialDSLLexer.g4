lexer grammar FinancialDSLLexer;

channels { WHITESPACE }

// Whitespace
NEWLINE            : '\r\n' | '\r' | '\n' ;
WS                 : [\t ]+ -> channel(WHITESPACE) ;

// Keywords
COMPANY            : 'company' ;
TYPE               : 'type' ;
TAX                : 'tax' ;
ON                 : 'on';
PERSON             : 'person' ;

// Identifiers
ID                 : [_]*[a-z][A-Za-z0-9_]* ;

// Literals
INTLIT             : '0'|[1-9][0-9]* ;
DECLIT             : '0'|[1-9][0-9]* '.' [0-9]+ ;

// Operators
PLUS               : '+' ;
MINUS              : '-' ;
ASTERISK           : '*' ;
DIVISION           : '/' ;
ASSIGN             : '=' ;
LPAREN             : '(' ;
RPAREN             : ')' ;
LBRACE             : '{' ;
RBRACE             : '}' ;
COLON              : ':' ;

UNMATCHED          : . ;
