grammar Ruby;

@header {
    package com.sourcegraph.toolchain.ruby.antlr4;
}

program
	: compstmt
	;

compstmt
	: stmt (term+ expr)* term?
	| term?
	;

stmt
	: call 'do' ('|' blockVar? '|')? term? compstmt 'end'
	| 'undef' fname
	| 'alias' fname fname
	| stmt 'if' expr
	| stmt 'while' expr
	| stmt 'unless' expr
	| stmt 'until' expr
	| 'BEGIN' '{' compstmt '}'
	| 'END' '{' compstmt '}'
	| lhs '=' command ('do' ('|' blockVar? '|')? compstmt 'end')?
	| expr
	;

expr
	: mlhs '=' mrhs
	| 'return' callArgs
	| 'yield' callArgs
	| expr 'and' expr
	| expr 'or' expr
	| 'not' expr
	| command
	| '!' command
	| arg
	;

call
	: function
	| command
	;

command
	: commandOrFunctionName callArgs
	;

function
	: commandOrFunctionName ('(' callArgs? ')')?
	;

commandOrFunctionName
	: operation 
	| primary '.' operation
	| primary '::' operation
	| 'super'
	;

arg
	: lhs '=' arg
	| lhs OP_ASGN arg
	| arg '..' arg 
	| arg '...' arg
	| arg '+' arg 
	| arg '-' arg 
	| arg '*' arg 
	| arg '/' arg
	| arg '%' arg 
	| arg '**' arg
	| '+' arg 
	| '-' arg
	| arg '|' arg
	| arg '?' arg 
	| arg '&' arg
	| arg '<=>' arg
	| arg '>' arg 
	| arg '>=' arg 
	| arg '<' arg 
	| arg '<=' arg
	| arg '==' arg 
	| arg '===' arg 
	| arg '!=' arg
	| arg '=?' arg 
	| arg '!?' arg
	| '!' arg 
	| '?' arg
	| arg '<<' arg 
	| arg '>>' arg
	| arg '&&' arg 
	| arg '||' arg
	| 'defined?' arg
	| primary
	| function
	| function '{' ('|' blockVar? '|')? compstmt '}'
	;

primary
	: '(' compstmt ')'
	| literal
	| variable
	| primary '::' IDENTIFIER
	| '::' IDENTIFIER
	| primary '[' args? ']'
	| primary '.' primary '[' args? ']'
	| '[]'
	| '[' (args ','?)? ']'
	| '{' (args | assocs ','?)? '}'
	| 'return' ('(' callArgs? ')')?
	| 'yield' ('(' callArgs? ')')?
	| 'defined?' '(' arg ')'
	| 'if' expr then compstmt ('elsif' expr then compstmt)* ('else' compstmt)? 'end'
	| 'unless' expr then compstmt ('else' compstmt)? 'end'
	| 'while' expr dostmt compstmt 'end'
	| 'until' expr dostmt compstmt 'end'
	| 'case' compstmt 'when' whenArgs then compstmt (whenArgs then compstmt)* ('else' compstmt)? 'end'
 	| 'for' blockVar 'in' expr dostmt compstmt 'end'
	| 'begin' compstmt ('rescue' args dostmt compstmt)* ('else' compstmt)? ('ensure' compstmt)? 'end'
	| 'class' IDENTIFIER ('<' IDENTIFIER)? compstmt 'end'
	| 'module' IDENTIFIER compstmt 'end'
	| 'def' fname argDecl compstmt 'end'
	| 'def' singleton ('.' | '::') fname argDecl compstmt 'end'
	;

whenArgs:
	args (',' '*' arg)?
	| '*' arg
	;

blockVar
	: lhs
	| mlhs
	;


mlhs
	: mlhsItem ',' (mlhsItem (',' mlhsItem)*)? ('*' lhs?)?
	| '*' lhs
	;

mlhsItem
	: lhs
	| '(' mlhs ')'
	;

lhs
	: variable
	| primary '[' args? ']'
	| primary '.' IDENTIFIER
	| function '[' args? ']'
	| function '{' ('|' blockVar? '|')? compstmt '}' '[' args? ']'
	| function '.' IDENTIFIER
	| function '{' ('|' blockVar? '|')? compstmt '}' '.' IDENTIFIER
	;

mrhs
	: args (',' '*' arg)?
	| '*' arg
	;

callArgs
	: args
	| args (',' assocs)? (',' '*' arg)? (',' '&' arg)?
	| assocs (',' '*' arg)? (',' '&' arg)?
	| '*' arg (',' '&' arg)?
	| '&' arg
	| command
	;

args
	: arg (',' arg)*
	;

argDecl
	: '(' argList ')'
	| argList term
	;

argList
	: IDENTIFIER (',' IDENTIFIER)* (',' '*' IDENTIFIER?)? ( ',' '&' IDENTIFIER)?
	| '*' IDENTIFIER (',' '&' IDENTIFIER)?
	| ('&' IDENTIFIER)?
	;

singleton
	: variable
	| '(' expr ')'
	;

assocs
	: assoc (',' assoc )*
	;

assoc
	: arg '=>' arg
	;

variable
	: varname
	| 'nil'
	| 'self'
	;

literal
	: numeric
	| symbol
	| string
// TODO	| string2
// TODO	| heredoc
// TODO	| regexp
	;

symbol
	: ':' fname
	| ':' varname
	;

fname
	: IDENTIFIER
	| '..'
	| '|'
	| '?'
	| '&'
	| '<=>'
	| '=='
	| '==='
	| '=?'
	| '>'
	| '>='
	| '<'
	| '<='
	| '+'
	| '-'
	| '*'
	| '/'
	| '%'
	| '**'
	| '<<'
	| '>>'
	| '?'
	| '+@'
	| '-@'
	| '[]'
	| '[]='
	;

operation
	: IDENTIFIER ('!' | '?')?
	;

varname
	: global
	| '@' IDENTIFIER
	| IDENTIFIER
	;

global
	: '$' IDENTIFIER
// TODO	| '$' ANYCHAR
// TODO	| '$-' ANYCHAR
	;

string
	: StringLiteral
	;

numeric
    : IntegerLiteral
    | FloatingPointLiteral
    ;

// TODO string2
// TODO	: '%' ('Q'|'q'|'x')CHAR ANYCHAR* CHAR
// TODO	;

// TODO heredoc
// TODO	: '<<' (IDENTIFIER | string) ANYCHAR* IDENTIFIER
// TODO	;

// TODO regexp
// TODO	: '/' CHAR* '/' ('i'|'o'|'p')?
// TODO	| '%r' CHAR ANYCHAR* CHAR
// TODO	;

then
	: term
	| 'then'
	| term 'then'
	;

dostmt
	: term
	| 'do'
	| term 'do'
	;

term
	: ';'
	| '\n'
	;

// LEXER

WS  :  [ \r\t\u000C] -> skip;

IDENTIFIER
	: [a-zA-Z_][a-zA-Z_0-9]*
	;

OP_ASGN
	: '+='
	| '-='
	| '*='
	| '/='
	| '%='
	| '**='
	| '&='
	| '|='
	| '?='
	| '<<='
	| '>>='
	| '&&='
	| '||='
	;

SINGLELINE_COMMENT
    :   '#' ~[\r\n]* -> skip
    ;

StringLiteral
	: '"' StringCharactersDQ ? '"'
	| '`' StringCharactersBT ? '`'
	| '\'' StringCharactersSQ ? '\''
	;

fragment
StringCharactersDQ
	:	StringCharacterDQ+
	;

fragment
StringCharactersSQ
	:	StringCharacterSQ+
	;

fragment
StringCharactersBT
	:	StringCharacterBT+
	;

fragment
StringCharacterDQ
	:	~["\\\n]
	|	EscapeSequence
	;

fragment
StringCharacterSQ
	:	~['\\\n]
	|	EscapeSequence
	;

fragment
StringCharacterBT
	:	~[`\\\n]
	|	EscapeSequence
	;

fragment
EscapeSequence
	:	'\\' [btnfr"'\\]
	|	OctalEscape
    |   UnicodeEscape
	;

fragment
OctalEscape
	:	'\\' OctalDigit
	|	'\\' OctalDigit OctalDigit
	|	'\\' ZeroToThree OctalDigit OctalDigit
	;

fragment
ZeroToThree
	:	[0-3]
	;

fragment
UnicodeEscape
    :   '\\' 'u' HexDigit HexDigit HexDigit HexDigit
    ;

IntegerLiteral
	:	DecimalIntegerLiteral
	|	HexIntegerLiteral
	|	OctalIntegerLiteral
	|	BinaryIntegerLiteral
	;

DecimalIntegerLiteral
	:	'0'
	|	NonZeroDigit (Digits? | Underscores Digits)
	;

HexIntegerLiteral
	:	'0' [xX] HexDigits
	;

OctalIntegerLiteral
    : '0' Underscores? OctalDigits
	;

fragment
BinaryIntegerLiteral
	:	'0' [bB] BinaryDigits
	;

fragment
Digits
	:	Digit (DigitsAndUnderscores? Digit)?
	;

fragment
Digit
	:	'0'
	|	NonZeroDigit
	;

fragment
NonZeroDigit
	:	[1-9]
	;

fragment
DigitsAndUnderscores
	:	DigitOrUnderscore+
	;

fragment
DigitOrUnderscore
	:	Digit
	|	'_'
	;

fragment
Underscores
	:	'_'+
	;

fragment
HexDigits
	:	HexDigit (HexDigitsAndUnderscores? HexDigit)?
	;

fragment
HexDigit
	:	[0-9a-fA-F]
	;

fragment
HexDigitsAndUnderscores
	:	HexDigitOrUnderscore+
	;

fragment
HexDigitOrUnderscore
	:	HexDigit
	|	'_'
	;

fragment
OctalDigits
	:	OctalDigit (OctalDigitsAndUnderscores? OctalDigit)?
	;

fragment
OctalDigit
	:	[0-7]
	;

fragment
OctalDigitsAndUnderscores
	:	OctalDigitOrUnderscore+
	;

fragment
OctalDigitOrUnderscore
	:	OctalDigit
	|	'_'
	;

fragment
BinaryDigits
	:	BinaryDigit (BinaryDigitsAndUnderscores? BinaryDigit)?
	;

fragment
BinaryDigit
	:	[01]
	;

fragment
BinaryDigitsAndUnderscores
	:	BinaryDigitOrUnderscore+
	;

fragment
BinaryDigitOrUnderscore
	:	BinaryDigit
	|	'_'
	;

// §3.10.2 Floating-Point Literals

FloatingPointLiteral
	:	DecimalFloatingPointLiteral
	;

fragment
DecimalFloatingPointLiteral
	:	Digits '.' Digits ExponentPart?
	|	'.' Digits ExponentPart?
	|	Digits ExponentPart
	|	Digits
	;

fragment
ExponentPart
	:	ExponentIndicator SignedInteger
	;

fragment
ExponentIndicator
	:	[eE]
	;

fragment
SignedInteger
	:	Sign? Digits
	;

fragment
Sign
	:	[+-]
	;
