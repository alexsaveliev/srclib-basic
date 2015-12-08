parser grammar RubyParser;

@header {
    package com.sourcegraph.toolchain.ruby.antlr4;
}

options { tokenVocab=RubyLexer; }

program
	: compstmt
	;

compstmt
	: (stmt | term)*
	;

stmt
	: call Do (Bar blockVar? Bar)? term? compstmt End
	| Undef fname
	| Alias fname fname
	| stmt If expr
	| stmt While expr
	| stmt Unless expr
	| stmt Until expr
	| BEGIN OpenCurlyBracket compstmt CloseCurlyBracket
	| END OpenCurlyBracket compstmt CloseCurlyBracket
	| lhs Eq command (Do (Bar blockVar? Bar)? compstmt End)?
	| expr
	;

expr
	: mlhs Eq mrhs
	| Return callArgs
	| Yield callArgs
	| expr And expr
	| expr Or expr
	| Not expr
	| command
	| Bang command
	| arg
	;

call
	: function
	| command
	;

command
	: operation callArgs
	| primary Dot operation callArgs
	| function DoubleColon operation callArgs
	| Super callArgs
	;

function
	: operation (OpenRoundBracket callArgs? CloseRoundBracket)?
	| function Dot operation (OpenRoundBracket callArgs? CloseRoundBracket)?
	| function DoubleColon operation (OpenRoundBracket callArgs? CloseRoundBracket)?
	| Super (OpenRoundBracket callArgs? CloseRoundBracket)?
	| primary
	;

arg
	: lhs Eq arg
	| lhs OpAssgn arg
	| arg DoubleDot arg
	| arg TripleDot arg
	| arg Plus arg
	| arg Minus arg
	| arg Star arg 
	| arg Divide arg
	| arg Percent arg
	| arg Pow arg
	| Plus arg
	| Minus arg
	| arg Bar arg
	| arg QuestionMark arg 
	| arg Ampersand arg
	| arg Spaceship arg
	| arg Greater arg
	| arg IsGreaterOrEqual arg
	| arg Smaller arg
	| arg IsSmallerOrEqual arg
	| arg IsEqual arg
	| arg IsIdentical arg
	| arg IsNotEq arg
	| arg EqQ arg
	| arg ExlQ arg
	| Bang arg 
	| QuestionMark arg
	| arg ShiftLeft arg
	| arg ShiftRight arg
	| arg BooleanAnd arg
	| arg BooleanOr arg
	| Defined arg
	| arg QuestionMark arg Colon arg
	| primaryOrFunction
	;

primary
	: OpenRoundBracket compstmt CloseRoundBracket
	| literal
	| variable
	| DoubleColon Identifier
	| primary OpenSquareBracket args? CloseSquareBracket
	| primary Dot primary OpenSquareBracket args? CloseSquareBracket
	| SquareBrackets
	| OpenSquareBracket (args Comma?)? CloseSquareBracket
	| OpenCurlyBracket (args | assocs Comma?)? CloseCurlyBracket
	| Return (OpenRoundBracket callArgs? CloseRoundBracket)?
	| Yield (OpenRoundBracket callArgs? CloseRoundBracket)?
	| Defined OpenRoundBracket arg CloseRoundBracket
	| If expr then compstmt (Elsif expr then compstmt)* (Else compstmt)? End
	| Unless expr then compstmt (Else compstmt)? End
	| While expr dostmt compstmt End
	| Until expr dostmt compstmt End
	| Case compstmt When whenArgs then compstmt (whenArgs then compstmt)* (Else compstmt)? End
 	| For blockVar In expr dostmt compstmt End
	| Begin compstmt (Rescue args dostmt compstmt)* (Else compstmt)? (Ensure compstmt)? End
	| Class Identifier (Smaller Identifier)? compstmt End
	| Module Identifier compstmt End
	| Def fname QuestionMark? argDecl compstmt End
	| Def singleton (Dot | DoubleColon) fname QuestionMark? argDecl compstmt End
	;

whenArgs:
	args (Comma Star arg)?
	| Star arg
	;

blockVar
	: lhs
	| mlhs
	;


mlhs
	: mlhsItem Comma (mlhsItem (Comma mlhsItem)*)? (Star lhs?)?
	| Star lhs
	;

mlhsItem
	: lhs
	| OpenRoundBracket mlhs CloseRoundBracket
	;

lhs
	: variable
	| primaryOrFunction OpenSquareBracket args? CloseSquareBracket
	| primaryOrFunction Dot Identifier
	;

mrhs
	: args (Comma Star arg)?
	| Star arg
	;

primaryOrFunction
		: primary
		| function
		;

callArgs
	: args
	| args (Comma assocs)? (Comma Star arg)? (Comma Ampersand arg)?
	| assocs (Comma Star arg)? (Comma Ampersand arg)?
	| Star arg (Comma Ampersand arg)?
	| Ampersand arg
	| command
	;

args
	: arg (Comma arg)*
	;

argDecl
	: OpenRoundBracket argList CloseRoundBracket
	| argList term
	;

argList
	: Identifier (Comma Identifier)* (Comma Star Identifier?)? ( Comma Ampersand Identifier)?
	| Star Identifier (Comma Ampersand Identifier)?
	| (Ampersand Identifier)?
	;

singleton
	: variable
	| OpenRoundBracket expr CloseRoundBracket
	;

assocs
	: assoc (Comma assoc )*
	;

assoc
	: arg DoubleArrow arg
	;

variable
	: varname
	| Nil
	| Self
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
	: Colon fname
	| Colon varname
	;

fname
	: Identifier
	| DoubleDot
	| Bar
	| QuestionMark
	| Ampersand
	| Spaceship
	| IsEqual
	| IsIdentical
	| EqQ
	| Greater
	| IsGreaterOrEqual
	| Smaller
	| IsSmallerOrEqual
	| Plus
	| Minus
	| Star
	| Divide
	| Percent
	| Pow
	| ShiftLeft
	| ShiftRight
	| QuestionMark
	| UnaryPlusMethod
	| UnaryMinusMethod
	| SquareBrackets
	| SquareBracketsAssgn
	;

operation
	: Identifier (Bang | QuestionMark)?
	;

varname
	: global
	| At Identifier
	| Identifier
	;

global
	: Dollar Identifier
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
// TODO	: '%' ('QBarqBarx')CHAR ANYCHAR* CHAR
// TODO	;

// TODO heredoc
// TODO	: '<<' (Identifier | string) ANYCHAR* Identifier
// TODO	;

// TODO regexp
// TODO	: '/' CHAR* '/' ('iBaroBarp')?
// TODO	| '%r' CHAR ANYCHAR* CHAR
// TODO	;

then
	: term
	| Then
	| term Then
	;

dostmt
	: term
	| Do
	| term Do
	;

term
	: SemiColon
	| Lf
	;
