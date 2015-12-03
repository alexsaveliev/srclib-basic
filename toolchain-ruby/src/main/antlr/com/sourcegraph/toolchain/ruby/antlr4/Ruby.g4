grammar Ruby;

@header {
    package com.sourcegraph.toolchain.ruby.antlr4;
}

program
	: compstmt
	;

compstmt
	: stmt (t expr)* t?
	;

stmt
	: call 'do' ('|' blockVar? '|')? compstmt 'end'
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
	;

primary
	: '(' compstmt ')'
	| literal
	| variable
	| primary '::' IDENTIFIER
	| '::' IDENTIFIER
	| primary '[' args? ']'
	| '[' (args ','?)? ']'
	| '{' (args | assocs ','?)? '}'
	| 'return' ('(' callArgs? ')')?
	| 'yield' ('(' callArgs? ')')?
	| 'defined?' '(' arg ')'
	| function
	| function '{' ('|' blockVar? '|')? compstmt '}'
	| 'if' expr then compstmt ('elsif' expr then compstmt)* ('else' compstmt)? 'end'
	| 'unless' expr then compstmt ('else' compstmt)? 'end'
	| 'while' expr keywordDo compstmt 'end'
	| 'until' expr keywordDo compstmt 'end'
	| 'case' compstmt 'when' whenArgs then compstmt (whenArgs then compstmt)* ('else' compstmt)? 'end'
 	| 'for' blockVar 'in' expr keywordDo compstmt 'end'
	| 'begin' compstmt ('rescue' args keywordDo compstmt)* ('else' compstmt)? ('ensure' compstmt)? 'end'
	| 'class' IDENTIFIER ('<' IDENTIFIER)? compstmt 'end'
	| 'module' IDENTIFIER compstmt 'end'
	| 'def' fname argDecl compstmt 'end'
	| 'def' singleton ('.' | '::') fname argDecl compstmt 'end'
	;

whenArgs:
	args (',' '*' arg)?
	| '*' arg
	;

then
	: t
	| 'then'
	| t 'then'
	;

keywordDo
	: t
	| 'do'
	| t 'do'
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
	| argList t
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
	: 'numeric'
	| symbol
	| string
	| string2
	| heredoc
	| regexp
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
	| '$' ANYCHAR
	| '$-' ANYCHAR
	;

string
	: '"' ANYCHAR* '"'
	| '’' ANYCHAR* '’'
	| '‘' ANYCHAR* '‘'
	;

string2
	: '%' ('Q'|'q'|'x')CHAR ANYCHAR* CHAR
	;

heredoc
	: '<<' (IDENTIFIER | string) ANYCHAR* IDENTIFIER
	;

regexp
	: '/' CHAR* '/' ('i'|'o'|'p')?
	| '%r' CHAR ANYCHAR* CHAR
	;

t
	: ';'
	| '\n'
	;

// LEXER

WS	: ' ' { _ttype = Token.SKIP; }
	;

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

CHAR
	: .+?
	;

ANYCHAR
	: .+?
	;
