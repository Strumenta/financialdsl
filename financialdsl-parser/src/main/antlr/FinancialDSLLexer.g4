lexer grammar FinancialDSLLexer;

channels { WHITESPACE, COMMENTS }

// Comments
LINE_COMMENT       : '//' (~[\r\n])* -> channel(COMMENTS) ;

// Whitespace
NEWLINE            : ('\r\n' | '\r' | '\n') -> channel(WHITESPACE) ;
WS                 : [\t ]+ -> channel(WHITESPACE) ;

// Keywords
COMPANY            : 'company' ;
TYPE               : 'type' ;
TAX                : 'tax' ;
ON                 : 'on';
PERSON             : 'person' ;
IS                 : 'is' ;
AMOUNT             : 'amount' ;
AT                 : 'at' ;
PARAMETER          : 'parameter' ;
SUM                : 'sum' ;
MONTH            : 'january' | 'february' | 'march' | 'april' | 'may' | 'june' | 'july' | 'august' | 'september' | 'october' | 'november' | 'december' ;
BEFORE : 'before' ;
AFTER : 'after' ;
PERIODICITY : 'monthly' | 'weekly' | 'yearly' ;

// Identifiers
ID                 : [A-Za-z][A-Za-z0-9_]* ;

// Literals
INTLIT             : '0'|[1-9][0-9]* ;
DECLIT             : ('0'|[1-9][0-9]*) '.' [0-9]+ ;
PERCLIT            : ('0'|[1-9][0-9]*) ('.' [0-9]+)? '%' ;

// Operators
PLUS               : '+' ;
MINUS              : '-' ;
ASTERISK           : '*' ;
DIVISION           : '/' ;
EQUAL              : '=' ;
LPAREN             : '(' ;
RPAREN             : ')' ;
LBRACE             : '{' ;
RBRACE             : '}' ;
LSQUARE            : '[' ;
RSQUARE            : ']' ;
COLON              : ':' ;
COMMA              : ',' ;
IN_ARROW           : '<-' ;

UNMATCHED          : . ;
