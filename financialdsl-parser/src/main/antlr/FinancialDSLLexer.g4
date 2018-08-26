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
MONTH              : 'january' | 'february' | 'march' | 'april' | 'may' | 'june' | 'july' | 'august' | 'september' | 'october' | 'november' | 'december' ;
BEFORE             : 'before' ;
AFTER              : 'after' ;
SINCE              : 'since' ;
PERIODICITY        : 'monthly' | 'weekly' | 'yearly' ;
CONTRIBUTES        : 'contributes' ;
TO                 : 'to' ;
OF                 : 'of' ;
SHARE              : 'share' ;
FOR                : 'for' ;
WHEN               : 'when' ;
THEN               : 'then' ;
COUNTRIES          : 'countries' ;
REGIONS            : 'regions' ;
CITIES             : 'cities' ;
EU                 : 'EU' ;
BRACKETS           : 'brackets' ;
ABOVE              : 'above' ;
BY                 : 'by' ;
OWNERS             : 'owners' ;
CITY               : 'city' ;

// Identifiers
ID                 : [A-Za-z][A-Za-z0-9_]* ;

// Literals
INTLIT             : ENTIRE_INT ('K'|'M')? ;
DECLIT             : ENTIRE_INT '.' [0-9]+('K'|'M')? ;
PERCLIT            : ('0'|[1-9][0-9]*) ('.' [0-9]+)? '%' ;

// Operators
PLUS               : '+' ;
MINUS              : '-' ;
ASTERISK           : '*' ;
DIVISION           : '/' ;
EQUAL              : '=' ;
LPAREN             : '(' ;
RPAREN             : ')' ;
TIMEOPEN           : '@{' ;
LBRACE             : '{' ;
RBRACE             : '}' ;
LSQUARE            : '[' ;
RSQUARE            : ']' ;
COLON              : ':' ;
COMMA              : ',' ;
IN_ARROW           : '<-' ;
OUT_ARROW           : '->' ;

UNMATCHED          : . ;

fragment ENTIRE_DIGIT_BLOCK : [0-9][0-9][0-9] ;
fragment PARTIAL_DIGIT_BLOCK :  '0'|([1-9][0-9]?) ;
fragment PARTIAL_OR_ENTIRE_BLOCK : ENTIRE_DIGIT_BLOCK | PARTIAL_DIGIT_BLOCK ;
fragment ENTIRE_INT_USING_BLOCKS : PARTIAL_OR_ENTIRE_BLOCK (',' ENTIRE_DIGIT_BLOCK)* ;
fragment ENTIRE_INT : ('0'|[1-9][0-9]*) | ENTIRE_INT_USING_BLOCKS ;