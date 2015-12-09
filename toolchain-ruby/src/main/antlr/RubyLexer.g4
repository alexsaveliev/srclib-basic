lexer grammar RubyLexer;

@header {
    package com.sourcegraph.toolchain.ruby.antlr4;

    import java.util.Map;
    import java.util.HashMap;
    import java.util.Queue;
    import java.util.LinkedList;
}

@lexer::members {

    private static final Map<Character, Character> BRACKET_PAIRS = new HashMap<>();

    static {
        BRACKET_PAIRS.put('<', '>');
        BRACKET_PAIRS.put('{', '}');
        BRACKET_PAIRS.put('(', ')');
        BRACKET_PAIRS.put('[', ']');
    }

    // A flag indicating if the lexer encountered __END__
    private boolean end;

    // Saved identifier used in the last heredoc
    private String heredocIdent;

    private Token lastToken;

    private char quotedStringDelimiter;

    private boolean quotedStringState;

    private boolean regexpMode;

    private StringBuilder quotedStringBuffer = new StringBuilder();

    private Queue<Token> queue = new LinkedList<>();

    @Override
    public Token nextToken() {

        if (!queue.isEmpty()) {
            return queue.remove();
        }

        if (end) {
            return emitEOF();
        }

        Token token = next();

        if (_mode == ModeHereDoc) {
            switch (token.getType())
            {
                case StartHereDoc1:
                case StartHereDoc2:
                case StartHereDoc3:
                case StartHereDoc4:
                    heredocIdent = extractHeredocIdent(token.getText());
                    while (isCrLf()) {
                        _input.consume();
                    }
                    break;
                case HereDocText:
                    if (checkHeredocEnd(token.getText())) {
                        popMode();
                        return nextToken();
                    }
                    break;
            }
        } else if (_mode == ModeQuotedString) {
            if (!quotedStringState) {
				if ("%r".equals(lastToken.getText())) {
					regexpMode = true;
				}
                quotedStringState = true;
            } else {
                token = lastToken = extractQuotedStringTokens();
                popMode();
                quotedStringState = false;
                regexpMode = false;
            }
        }
        return token;
    }

    private void setEnd() {
        this.end = true;
    }

    private String extractHeredocIdent(String tokenText) {
        // remove leading <<
        tokenText = tokenText.substring(2).trim();
        char c = tokenText.charAt(0);
        if (c == '-') {
            // remove optional leading -
            tokenText = tokenText.substring(1);
            c = tokenText.charAt(0);
        }
        if (c == '\'' || c == '"' || c == '`') {
            // remove leading and trailing quote
            tokenText = tokenText.substring(1, tokenText.length() - 1);
        }
        return tokenText;
    }

    private boolean checkHeredocEnd(String text)
    {
        return text.trim().equals(heredocIdent);
    }

    private boolean isCrLf() {
        return _input.LA(1) == '\r' || _input.LA(1) == '\n';
    }

    private boolean isRegexPossible() {

        if (this.lastToken == null) {
            // No token has been produced yet: at the start of the input,
            // no division is possible, so a regex literal _is_ possible.
            return true;
        }

        switch (this.lastToken.getType()) {
            case Identifier:
            case Nil:
            case Self:
            case CloseSquareBracket:
            case CloseRoundBracket:
            case OctalIntegerLiteral:
            case DecimalIntegerLiteral:
            case HexIntegerLiteral:
            case StringLiteral:
                // After any of the tokens above, no regex literal can follow.
                return false;
            default:
                // In all other cases, a regex literal _is_ possible.
                return true;
        }
    }

    private boolean isPercentStringPossible() {
        int c = _input.LA(2);
        if (c == '=') {
            return false;
        }
        if ("qQriIwWxs".indexOf(c) >= 0) {
            return true;
        }
        if (c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
            return false;
        }
        return true;
    }

    private void startRegexp() {
        regexpMode = true;
        pushMode(ModeQuotedString);
        quotedStringDelimiter = '/';
        quotedStringBuffer.setLength(0);
        quotedStringBuffer.append((char) _input.LA(1));
    }

    private void guessQuotedString() {
        char c = (char) _input.LA(1);
        quotedStringBuffer.setLength(0);
        quotedStringBuffer.append(c);
        Character delim = BRACKET_PAIRS.get(c);
        if (delim == null) {
            delim = c;
        }
        this.quotedStringDelimiter = delim;
        //_input.consume();
        pushMode(ModeQuotedString);
    }

    private Token extractQuotedStringTokens() {
        Token t = next();
        boolean escaped = false;
        while (t.getType() != EOF) {
            char c = t.getText().charAt(0);
            if (c == '\\') {
                escaped = !escaped;
            } else if (c == quotedStringDelimiter) {
                if (!escaped) {
                    quotedStringBuffer.append(t.getText());
                    if (regexpMode) {
                        extractRegExpModifiers();
                    }
                    return new CommonToken(QuotedString, quotedStringBuffer.toString());
                }
                escaped = false;
            } else {
                escaped = false;
            }
            quotedStringBuffer.append(t.getText());
            t = next();
        }
        return emitEOF();
    }

    private void extractRegExpModifiers() {
        StringBuilder buf = new StringBuilder();
        int c = _input.LA(1);
        while (c != EOF) {
            if ("ioxmuesn".indexOf(c) >= 0) {
                buf.append((char) c);
                _input.consume();
            } else {
                if (buf.length() > 0) {
                    queue.add(new CommonToken(RegularExpressionModifiers, buf.toString()));
                }
                break;
            }
			c = _input.LA(1);
        }
    }

    private Token next() {
        Token token = super.nextToken();
        if (token.getChannel() == Token.DEFAULT_CHANNEL) {
            // Keep track of the last token on the default channel.
            this.lastToken = token;
        }
        return token;
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
Tilde:              '~';
Divide:             {!isRegexPossible()}? '/';
Percent:            {!isPercentStringPossible()}? '%';
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

StartHereDoc1
    : '<<' '-'?  Identifier  { isCrLf() }? -> pushMode(ModeHereDoc)
    ;

StartHereDoc2
    : '<<' '-'?  '"' ~["] '"' { isCrLf() }? -> pushMode(ModeHereDoc)
    ;

StartHereDoc3
	: '<<' '-'?  '\'' ~['] '\'' { isCrLf() }? -> pushMode(ModeHereDoc)
    ;

StartHereDoc4
	: '<<' '-'?  '`' ~[`] '`' { isCrLf() }? -> pushMode(ModeHereDoc)
    ;

RegularExpressionLiteral
    : {isRegexPossible()}? '/' { startRegexp(); }
    ;

GeneralDelimitedRegularExpressionLiteral
    : '%r' { guessQuotedString(); }
    ;

PercentString
    : {isPercentStringPossible()}? '%' [qQiIwWxs]? { guessQuotedString(); }
    ;

MultiLineComment
    :   '=begin' .*? '=end' -> skip
    ;

mode ModeHereDoc;

HereDocText
	: ~[\r\n]*? '\r'? '\n'
	;

mode ModeQuotedString;

QuotedString
	:  .;

RegularExpressionModifiers
    : [ioxmuesn]+
    ;