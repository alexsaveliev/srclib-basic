// Converted to ANTLR 4 by Terence Parr; added @property and a few others.
// Seems to handle stuff like this except for blocks:
// https://google-api-objectivec-client.googlecode.com/svn/trunk/Examples/ShoppingSample/ShoppingSampleWindowController.m

/**
* ObjectiveC version 2
* based on an LL ansic grammars and
* and ObjectiveC grammar found in Learning Object C
*
* It's a Work in progress, most of the .h file can be parsed
* June 2008 Cedric Cuche
* Updated June 2014, Carlos Mejia.  Fix try-catch, add support for @( @{ @[ and blocks
**/

/**
 * Based on https://github.com/antlr/grammars-v4/blob/0c862ab4feb1adb9b02334ebbc42a42b0c854967/objc/ObjC.g4
**/
grammar ObjC;

@header {
    package com.sourcegraph.toolchain.objc.antlr4;
}

// alexsaveliev: changed external_declaration+ to external_declaration* - it's ok to have empty file
translation_unit: external_declaration* EOF;

external_declaration:
COMMENT | LINE_COMMENT | preprocessor_declaration
| function_definition
| declaration 
| class_interface
| class_implementation
| category_interface
| category_implementation
| protocol_declaration
| protocol_declaration_list
| class_declaration_list;

preprocessor_declaration:
IMPORT
| INCLUDE;


// alexsaveliev: added attribute_specifier_list?
class_interface:
    attribute_specifier_list?
	'@interface'
	attribute_specifier_list?
	class_name (':' superclass_name)?
	attribute_specifier_list?
	protocol_reference_list?
	instance_variables?
	interface_declaration_list?
	'@end';

// alexsaveliev: added attribute_specifier_list?
category_interface:
    attribute_specifier_list?
	'@interface'
	attribute_specifier_list?
	class_name '(' category_name? ')'
	attribute_specifier_list?
	protocol_reference_list?
	attribute_specifier_list?
	instance_variables?
	interface_declaration_list?
	'@end';

class_implementation:
	'@implementation'
	(
	class_name ( ':' superclass_name )?
	instance_variables?
	( implementation_definition_list )?
	)
	'@end';

category_implementation:
	'@implementation'(
	class_name '(' category_name ')'
	( implementation_definition_list )?
	)'@end';

// alexsaveliev: added attribute_specifier_list?
protocol_declaration:
    attribute_specifier_list?
	'@protocol'(
	protocol_name ( protocol_reference_list )?
	attribute_specifier_list?
	interface_declaration_list? '@optional'? interface_declaration_list?
	)'@end';

protocol_declaration_list:
	('@protocol' protocol_list';')
	;

class_declaration_list:
	('@class' class_list';')
	;

class_list:
	class_name (',' class_name)*;

protocol_reference_list:
	('<' protocol_list '>');

protocol_list:
	protocol_name (',' protocol_name)*;

property_declaration
    : '@property' property_attributes_declaration? struct_declaration
    ;

property_attributes_declaration
    : '(' property_attributes_list ')'
    ;

property_attributes_list
    : property_attribute (',' property_attribute)*
    ;

property_attribute
    : 'nonatomic' | 'assign' | 'weak' | 'strong' | 'retain' | 'readonly' | 'readwrite' |
    | 'getter' '=' IDENTIFIER //  getter 
    | 'setter' '=' IDENTIFIER ':' // setter
    | IDENTIFIER
    ;

class_name:
	IDENTIFIER;

superclass_name:
	IDENTIFIER;

category_name:
	IDENTIFIER;

protocol_name:
	IDENTIFIER;

instance_variables
    :   '{' struct_declaration* '}'
// alexsaveliev: replaced
// '{' visibility_specification struct_declaration+ '}' with
// with
// '{' (visibility_specification struct_declaration+)* '}'
// to add support of
// @protected
//   A
// @private
//   B
    |   '{' (visibility_specification? struct_declaration+)* '}'
    |   '{' struct_declaration+ instance_variables '}'
    |   '{' visibility_specification struct_declaration+ instance_variables '}'
    ;

visibility_specification
	: '@private'
	| '@protected'
	| '@package' 
	| '@public';

interface_declaration_list:
	(declaration | class_method_declaration | instance_method_declaration | property_declaration)+
	;

class_method_declaration:
	('+' method_declaration)
	;

instance_method_declaration:
	('-' method_declaration)
	;

// alexsaveliev: added attribute_specifier_list?
method_declaration:
	attribute_specifier_list? ( method_type )? attribute_specifier_list? method_selector attribute_specifier_list? ';';

implementation_definition_list
    : ( function_definition
      | declaration 
      | class_method_definition 
      | instance_method_definition
      | property_implementation
      )+
    ;

class_method_definition:
	('+' method_definition)
	;

instance_method_definition:
	('-' method_definition) 
	;
	
method_definition:
	(method_type)? method_selector (init_declarator_list)? ';'? compound_statement;

method_selector
	: selector
	| 'retain'
	| (keyword_declarator+ (parameter_list)? )
	| (keyword_declarator+ (',' '...')? )
	;

keyword_declarator:
	selector? ':' method_type* IDENTIFIER;

selector:
IDENTIFIER;

method_type: '(' type_name ')';

property_implementation
    : '@synthesize' property_synthesize_list ';'
    | '@dynamic' property_synthesize_list ';'
    ;

property_synthesize_list
    : property_synthesize_item (',' property_synthesize_item)*
    ;

property_synthesize_item
    : IDENTIFIER | IDENTIFIER '=' IDENTIFIER
    ;

block_type:type_specifier '(''^' type_specifier? ')' block_parameters? ;

// alexsaveliev: replaced IDENTIFIER with identifier
type_specifier:
'void' | 'char' | 'short' | 'int' | 'long' | 'float' | 'double' | 'signed' | 'unsigned' 
	|	('id' ( protocol_reference_list )? '*'? )
	|	(class_name ( protocol_reference_list )? '*'? )
	|	struct_or_union_specifier '*'?
	|	enum_specifier '*'?
    |   identifier '*'?;

type_qualifier:
	'const' | 'volatile' | protocol_qualifier;

protocol_qualifier:
	'in' | 'out' | 'inout' | 'bycopy' | 'byref' | 'oneway';

// alexsaveliev: added string_literal
// alexsaveliev: added '@' identifier to support "return [foo bar:@baz]"
primary_expression:
	identifier
	| constant
	| string_literal+
	| ('(' expression ')')
	| 'self'
    | 'super'
	| message_expression
	| selector_expression
	| protocol_expression
	| encode_expression
    | dictionary_expression
    | array_expression
    | box_expression
    | block_expression
    | '@' identifier;

dictionary_pair:
         postfix_expression':'postfix_expression;

dictionary_expression:
        '@''{' dictionary_pair? (',' dictionary_pair)* ','? '}';

// alexsaveliev: allowing cast expression insteaf of postfix expression
// to support the following constructs
 // "v = @[(id)[UIColor whiteColor].CGColor, (id)[CGTATestAppColorList tealColor].CGColor];"
array_expression:
        '@''[' cast_expression? (',' cast_expression)* ','? ']';

box_expression:
        '@''('conditional_expression')' |
        '@'constant;
block_parameters: '(' (type_variable_declarator | 'void')? (',' type_variable_declarator)* ')';

block_expression:'^' type_specifier? block_parameters? compound_statement;

message_expression:
	'[' receiver message_selector ']'
	;

receiver:
	expression
	| class_name 
	| 'super';

message_selector:
	selector
	| 'retain'
	| keyword_argument+;

keyword_argument:
	selector? ':' expression;

selector_expression:
	'@selector' '(' selector_name ')';

selector_name:
	selector
	| (selector? ':')+;

protocol_expression:
	'@protocol' '(' protocol_name ')';

encode_expression:
	'@encode' '(' type_name ')';

type_variable_declarator:
	declaration_specifiers declarator;

try_statement:
	'@try' compound_statement;

catch_statement:
	'@catch' '('type_variable_declarator')' compound_statement;

finally_statement:
	'@finally' compound_statement;

throw_statement:
	'@throw' '('IDENTIFIER')';

try_block: 
        try_statement
        ( catch_statement)*
        ( finally_statement )?;

synchronized_statement:
	'@synchronized' '(' primary_expression ')' compound_statement;

autorelease_statement:
	'@autoreleasepool'  compound_statement;

// alexsaveliev: added attribute_specifier_list?
function_definition : attribute_specifier_list? declaration_specifiers? attribute_specifier_list? identifier ( '(' parameter_list? ')' )? attribute_specifier_list? compound_statement ;

// alexsaveliev: added attribute_specifier_list?
declaration : attribute_specifier_list? declaration_specifiers attribute_specifier_list? init_declarator_list? ';';

declaration_specifiers 
  : (arc_behaviour_specifier | storage_class_specifier | type_specifier | type_qualifier)+ ;

// alexsaveliev: added __strong
arc_behaviour_specifier: '__unsafe_unretained' | '__weak' | '__strong';
storage_class_specifier: 'auto' | 'register' | 'static' | 'extern' | 'typedef';

init_declarator_list :	init_declarator (',' init_declarator)* ;
init_declarator : declarator ('=' initializer)? ;

struct_or_union_specifier: ('struct' | 'union')
  ( IDENTIFIER | IDENTIFIER? '{' struct_declaration+ '}') ;

struct_declaration : specifier_qualifier_list struct_declarator_list ';' ;

specifier_qualifier_list : (arc_behaviour_specifier | type_specifier | type_qualifier)+ ;

struct_declarator_list : struct_declarator (',' struct_declarator)* ;

struct_declarator : declarator | declarator? ':' constant;

enum_specifier : 'enum' (':' type_name)? 
  ( identifier ('{' enumerator_list '}')? 
  | '{' enumerator_list '}') 
  | 'NS_OPTIONS' '(' type_name ',' identifier ')' '{' enumerator_list '}'
  | 'NS_ENUM' '(' type_name ',' identifier ')' '{' enumerator_list '}' ;


enumerator_list : enumerator (',' enumerator)* ','? ;
enumerator : identifier ('=' constant_expression)?;

pointer
    :   '*' declaration_specifiers?
    |   '*' declaration_specifiers? pointer;
    


declarator : pointer ? direct_declarator ;

direct_declarator : identifier declarator_suffix*
                  | '(' declarator ')' declarator_suffix* 
                  | '(''^' identifier? ')' block_parameters;


declarator_suffix : '[' constant_expression? ']'
		  | '(' parameter_list? ')';

parameter_list : parameter_declaration_list ( ',' '...' )? ;

parameter_declaration 
  : declaration_specifiers (declarator? | abstract_declarator) ;

initializer : assignment_expression
	    | '{' initializer? (',' initializer)* ','? '}' ;

type_name : specifier_qualifier_list abstract_declarator 
          | block_type;

abstract_declarator : pointer abstract_declarator 
  | '(' abstract_declarator ')' abstract_declarator_suffix+
  | ('[' constant_expression? ']')+
  | ;

abstract_declarator_suffix
  : '[' constant_expression? ']'
  | '('  parameter_declaration_list? ')' ;

parameter_declaration_list
  : parameter_declaration ( ',' parameter_declaration )* ;

statement_list : statement+ ;

statement 
  : labeled_statement
  | expression ';'
  | compound_statement
  | selection_statement
  | iteration_statement
  | jump_statement
  | synchronized_statement
  | autorelease_statement
  | try_block
  | ';' ;

labeled_statement
  : identifier ':' statement
  | 'case' constant_expression ':' statement
  | 'default' ':' statement ;

compound_statement : '{' (declaration|statement_list)* '}' ;

selection_statement
  : 'if' '(' expression ')' statement ('else' statement)?
  | 'switch' '(' expression ')' statement ;

for_in_statement : 'for' '(' for_in_type_variable_declarator 'in' expression? ')' statement;
for_statement: 'for' '(' ((declaration_specifiers init_declarator_list) | expression)? ';' expression? ';' expression? ')' statement;
while_statement: 'while' '(' expression ')' statement;
do_statement: 'do' statement 'while' '(' expression ')' ';';

for_in_type_variable_declarator:
	declaration_specifiers? declarator;


iteration_statement
  : while_statement
  | do_statement
  | for_statement
  | for_in_statement ;

jump_statement
  : 'goto' identifier ';'
  | 'continue' ';'
  | 'break' ';'
  | 'return' expression? ';' 
  ;

expression : assignment_expression (',' assignment_expression)* ;

assignment_expression
    :   conditional_expression
    |   unary_expression assignment_operator assignment_expression
    ;

assignment_operator: 
  '=' | '*=' | '/=' | '%=' | '+=' | '-=' | '<<=' | '>>=' | '&=' | '^=' | '|=';

// alexsaveliev: made first conditional_expression optional to add support of "a ?: b;"
conditional_expression : logical_or_expression 
  ('?' conditional_expression? ':' conditional_expression)? ;

constant_expression : conditional_expression ;

logical_or_expression :	logical_and_expression 
  ('||' logical_and_expression)* ;

logical_and_expression : inclusive_or_expression 
  ('&&' inclusive_or_expression)* ;

inclusive_or_expression : exclusive_or_expression 
  ('|' exclusive_or_expression)* ;

exclusive_or_expression : and_expression ('^' and_expression)* ;

and_expression : equality_expression ('&' equality_expression)* ;

equality_expression : relational_expression 
  (('!=' | '==') relational_expression)* ;

relational_expression : shift_expression
 (('<' | '>' | '<=' | '>=') shift_expression)* ;

shift_expression : additive_expression (('<<' | '>>') additive_expression)* ;

additive_expression : multiplicative_expression
  (('+' | '-') multiplicative_expression)* ;

multiplicative_expression : cast_expression 
  (('*' | '/' | '%') cast_expression)* ;

cast_expression : '(' type_name ')' cast_expression | unary_expression ;

unary_expression 
  : postfix_expression
  | '++' unary_expression
  | '--' unary_expression
  | unary_operator cast_expression
  | 'sizeof' ('(' type_name ')' | unary_expression) ;

unary_operator : '&' | '*' | '-' | '~' | '!' ;

postfix_expression : primary_expression
  ('[' expression ']' 
  | '(' argument_expression_list? ')'
  | '.' identifier
  | '->' identifier
  | '++'
  | '--'
  )* ;

// alexsaveliev: replaced assignment_expression with new rule argument_expression
argument_expression_list
  : argument_expression (',' argument_expression )* ;

// alexsaveliev: identifier '*' supposed to support constructs such as "va_arg(args, char *)";
argument_expression
  : assignment_expression
  | identifier '*' ;

// alexsaveliev: basic attributes support
attribute_specifier_list
  :	attribute_specifier ;

attribute_specifier
  : '__attribute__' attribute_list? ;

attribute_list
  : '(' '(' attribute (',' attribute)* ')' ')';

attribute
  : attribute_name attribute_parameters? ;

attribute_name
  : 'const'
  | IDENTIFIER ;

attribute_parameters
  : '(' attribute_parameter_list? ')' ;

attribute_parameter_list
  : attribute_parameter (',' attribute_parameter)* ;

attribute_parameter
  : attribute
  | constant
  | string_literal
  | attribute_parameter_assignment;

attribute_parameter_assignment
  : attribute_name '=' constant
  | attribute_name '=' attribute_name
  | attribute_name '=' string_literal;

string_literal : STRING_LITERAL ;
// alexsaveliev: end of basic attributes support

// alexsaveliev: self is allowed identifier name
// alexsaveliev: predefined types are also allowed in some cases (va_arg(args, int))
identifier
  : IDENTIFIER
  | 'self'
  | 'int'
  | 'void'
  | 'char'
  | 'short'
  | 'long'
  | 'float'
  | 'double'
  | 'signed'
  | 'unsigned';

constant : DECIMAL_LITERAL | HEX_LITERAL | OCTAL_LITERAL | CHARACTER_LITERAL | FLOATING_POINT_LITERAL;

// LEXER

// §3.9 Keywords


AUTORELEASEPOOL : '@autoreleasepool';
CATCH           : '@catch';
CLASS           : '@class';
DYNAMIC         : '@dynamic';
ENCODE          : '@encode';
END             : '@end';
FINALLY         : '@finally';
IMPLEMENTATION  : '@implementation';
INTERFACE       : '@interface';
PACKAGE         : '@package';
PROTOCOL        : '@protocol';
OPTIONAL        : '@optional';
PRIVATE         : '@private';
PROPERTY        : '@property';
PROTECTED       : '@protected';
PUBLIC          : '@public';
SELECTOR        : '@selector';
SYNCHRONIZED    : '@synchronized';
SYNTHESIZE      : '@synthesize';
THROW           : '@throw';
TRY             : '@try';

SUPER           : 'super';
SELF            : 'self';


ABSTRACT      : 'abstract';
AUTO          : 'auto';
BOOLEAN       : 'boolean';
BREAK         : 'break';
BYCOPY        : 'bycopy';
BYREF         : 'byref';
CASE          : 'case';
CHAR          : 'char';
CONST         : 'const';
CONTINUE      : 'continue';
DEFAULT       : 'default';
DO            : 'do';
DOUBLE        : 'double';
ELSE          : 'else';
ENUM          : 'enum';
EXTERN        : 'extern';
FLOAT         : 'float';
FOR           : 'for';
ID            : 'id';
IF            : 'if';
IN            : 'in';
INOUT         : 'inout';
GOTO          : 'goto';
INT           : 'int';
LONG          : 'long';
ONEWAY        : 'oneway';
OUT           : 'out';
REGISTER      : 'register';
RETURN        : 'return';
SHORT         : 'short';
SIGNED        : 'signed';
SIZEOF        : 'sizeof';
STATIC        : 'static';
STRUCT        : 'struct';
SWITCH        : 'switch';
TYPEDEF       : 'typedef';
UNION         : 'union';
UNSIGNED      : 'unsigned';
VOID          : 'void';
VOLATILE      : 'volatile';
WHILE         : 'while';

NS_OPTIONS          : 'NS_OPTIONS';
NS_ENUM             : 'NS_ENUM';
WWEAK               : '__weak';
WUNSAFE_UNRETAINED  : '__unsafe_unretained';

// §3.11 Separators

LPAREN          : '(';
RPAREN          : ')';
LBRACE          : '{';
RBRACE          : '}';
LBRACK          : '[';
RBRACK          : ']';
SEMI            : ';';
COMMA           : ',';
DOT             : '.';
STRUCTACCESS    : '->';
AT              : '@';

// Operators

ASSIGN          : '=';
GT              : '>';
LT              : '<';
BANG            : '!';
TILDE           : '~';
QUESTION        : '?';
COLON           : ':';
EQUAL           : '==';
LE              : '<=';
GE              : '>=';
NOTEQUAL        : '!=';
AND             : '&&';
OR              : '||';
INC             : '++';
DEC             : '--';
ADD             : '+';
SUB             : '-';
MUL             : '*';
DIV             : '/';
BITAND          : '&';
BITOR           : '|';
CARET           : '^';
MOD             : '%';
SHIFT_R         : '>>';
SHIFT_L         : '<<';

// Assignment

ADD_ASSIGN      : '+=';
SUB_ASSIGN      : '-=';
MUL_ASSIGN      : '*=';
DIV_ASSIGN      : '/=';
AND_ASSIGN      : '&=';
OR_ASSIGN       : '|=';
XOR_ASSIGN      : '^=';
MOD_ASSIGN      : '%=';
LSHIFT_ASSIGN   : '<<=';
RSHIFT_ASSIGN   : '>>=';
ELIPSIS         : '...';

// Property attributes
ASSIGNPA        : 'assign';
GETTER          : 'getter';
NONATOMIC       : 'nonatomic';
SETTER          : 'setter';
STRONG          : 'strong';
RETAIN          : 'retain';
READONLY        : 'readonly';
READWRITE       : 'readwrite';
WEAK            : 'weak';

IDENTIFIER
	:	LETTER (LETTER|'0'..'9')*
	;
	
fragment
LETTER
	:	'$'
	|	'A'..'Z'
	|	'a'..'z'
	|	'_'
	;

CHARACTER_LITERAL
    :   '\'' ( EscapeSequence | ~('\''|'\\') ) '\''
    ;

/*
s_char = [[[any_char - '"'] - '\'] - nl] | escape_sequence;
string_literal = ('L' | '@') '"' s_char* '"';
*/

STRING_LITERAL
    :  [L@]? STRING
    ;

// alexsaveliev: added UnicodeEscape
fragment
STRING : '"' ( EscapeSequence | UnicodeEscape | ~('\\'|'"') )* '"' ;

HEX_LITERAL : '0' ('x'|'X') HexDigit+ IntegerTypeSuffix? ;

DECIMAL_LITERAL : ('0' | '1'..'9' '0'..'9'*) IntegerTypeSuffix? ;

OCTAL_LITERAL : '0' ('0'..'7')+ IntegerTypeSuffix? ;

fragment
HexDigit : ('0'..'9'|'a'..'f'|'A'..'F') ;

fragment
IntegerTypeSuffix
	:	('u'|'U'|'l'|'L')
	;

FLOATING_POINT_LITERAL
    :   ('0'..'9')+ ('.' ('0'..'9')*)? Exponent? FloatTypeSuffix?
    |   '.' ('0'..'9')+ Exponent? FloatTypeSuffix?
	;

fragment
Exponent : ('e'|'E') ('+'|'-')? ('0'..'9')+ ;

fragment
FloatTypeSuffix : ('f'|'F'|'d'|'D') ;

fragment
EscapeSequence
    :   '\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\')
    |   OctalEscape
    ;

fragment
OctalEscape
    :   '\\' ('0'..'3') ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7')
    ;

fragment
UnicodeEscape
    :   '\\' 'u' HexDigit HexDigit HexDigit HexDigit
    ;

// alexsaveliev: removed -> channel(HIDDEN)
IMPORT : '#import' [ \t]* (STRING|ANGLE_STRING) WS ;
// alexsaveliev: removed -> channel(HIDDEN)
INCLUDE: '#include'[ \t]* (STRING|ANGLE_STRING) WS ;
// alexsaveliev: added WS*
PRAGMA : '#' WS* 'pragma' ~[\r\n]* -> channel(HIDDEN) ;

fragment
ANGLE_STRING
    :   '<' .*? '>'
    ;

WS  :  [ \r\n\t\u000C] -> channel(HIDDEN) ;

COMMENT
    :   '/*' .*? '*/'  -> channel(HIDDEN)
    ;

LINE_COMMENT
    : '//' ~[\r\n]*  -> channel(HIDDEN)
    ;

NEW_LINE
    : ('\r'? '\n') -> channel(HIDDEN)
    ;

LINE_CONTINUE
    : '\\' [ \t\u000C]* NEW_LINE -> channel(HIDDEN)
    ;

// ignore preprocessor defines for now

HDEFINE : '#define' ~[\r\n]* -> channel(HIDDEN);
// alexsaveliev: multiline define
HMULTILINEDEFINE : '#define' (~[\r\n] | LINE_CONTINUE)* -> channel(HIDDEN);
HIF : '#if' ~[\r\n]* -> channel(HIDDEN);
// alexsaveliev: added #elif
HELIF : '#elif' ~[\r\n]* -> channel(HIDDEN);
HELSE : '#else' ~[\r\n]* -> channel(HIDDEN);
HUNDEF : '#undef' ~[\r\n]* -> channel(HIDDEN);
HIFNDEF : '#ifndef' ~[\r\n]* -> channel(HIDDEN);
HENDIF : '#endif' ~[\r\n]* -> channel(HIDDEN);


