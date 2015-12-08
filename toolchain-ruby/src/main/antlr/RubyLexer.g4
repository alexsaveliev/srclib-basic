lexer grammar RubyLexer;

@header {
    package com.sourcegraph.toolchain.ruby.antlr4;
}

@lexer::members {

    // A flag indicating if the lexer encountered __END__
    private boolean end;

    @Override
    public Token nextToken() {

		if (end) {
			return emitEOF();
		}
        return super.nextToken();
    }

    private void setEnd() {
    	this.end = true;
    }

}

WS
	:  [ \r\t\u000C] -> skip;

Identifier
	: '__END__' { setEnd(); }
	| [a-zA-Z_][a-zA-Z_0-9]*
	;

OpAssgn
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
	:	'\\' .
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

End
	: 'end'
	;

Do
	: 'do'
	;

Undef
	: 'undef'
	;

Alias
	: 'alias'
	;

If
	: 'if'
	;

Elsif
	: 'elsif'
	;

Else
	: 'else'
	;

While
	: 'while'
	;

Unless
	: 'unless'
	;

Until
	: 'until'
	;

Nil
	: 'nil'
	;

Self
	: 'self'
	;

BEGIN
	: 'BEGIN'
	;

END
	: 'END'
	;

Return
	: 'return'
	;

Yield
	: 'yield'
	;

And
	: 'and'
	;

Or
	: 'or'
	;

Not
	: 'not'
	;

Super
	: 'super'
	;

Defined
	: 'defined?'
	;

Case
	: 'case'
	;

When
	: 'when'
	;

Then
	: 'then'
	;

Rescue
	: 'rescue'
	;

Ensure
	: 'ensure'
	;

For
	: 'for'
	;

In
	: 'in'
	;

Begin
	: 'begin'
	;

Class
	: 'class'
	;

Module
	: 'module'
	;

Def
	: 'def'
	;

SquareBrackets: 	'[]';
SquareBracketsAssgn:'[]=';
OpenRoundBracket:   '(';
CloseRoundBracket:  ')';
OpenSquareBracket:  '[';
CloseSquareBracket: ']';
OpenCurlyBracket:   '{';
CloseCurlyBracket:  '}';
Dot:                '.';
Colon:        		':';
DoubleColon:        '::';
Comma:              ',';
Star:               '*';
Ampersand:          '&';
Bar: 				'|';
Eq:                 '=';
Bang:               '!';
Greater:            '>';
Smaller:            '<';
Plus:               '+';
Minus:              '-';
Asterisk:           '*';
Percent:            '%';
Divide:             '/';
QuestionMark:       '?';
IsSmallerOrEqual:   '<=';
IsGreaterOrEqual:   '>=';
Pow:                '**';
Spaceship:          '<==>';
IsIdentical:        '===';
IsEqual:            '==';
IsNotEq:            '!=';
BooleanOr:          '||';
BooleanAnd:         '&&';
ShiftLeft:          '<<';
ShiftRight:         '>>';
DoubleArrow:        '=>';
At:   				'@';
Dollar:             '$';
DoubleDot:          '..';
TripleDot:          '...';
SemiColon:          ';';
UnaryPlusMethod:    '+@';
UnaryMinusMethod:   '-@';
EqQ:   				'=?';
ExlQ:   			'!?';
Lf:          		'\n';

