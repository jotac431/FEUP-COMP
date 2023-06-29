grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : [0] | [1-9][0-9]* ;
ID : [a-zA-Z_$][a-zA-Z_$0-9]* ;
WS : [ \t\n\r\f]+ -> skip ;
COMMENT : '/*' .*? '*/' -> skip ;
LINE_COMMENT : '//' ~[\r\n]* -> skip ;


program
    : (importDeclaration)* (classDeclaration | WS)* (statement | WS)* EOF #EntireProgram
    ;

importDeclaration
    : 'import' value+=ID ('.' value+=ID)* ';' #Import
    ;

classDeclaration
    : 'class' className=ID ( 'extends' superClass=ID )? '{' ( varDeclaration )* ( methodDeclaration )* '}' #Class
    ;

varDeclaration
    : type name=ID ';' #Variable
    ;


methodDeclaration
    //: ('public')? type methodName=ID '(' ( type ID ( ',' type ID )* )? ')' '{' ( varDeclaration)* ( statement )* 'return' expression ';' '}' #GeneralMethod
    : ('public')? type methodName=ID '(' (parameterList)? ')' '{' ( varDeclaration)* ( statement )* 'return' expression ';' '}' #GeneralMethod
    | ('public')? 'static' type methodName='main' '(' (parameterList)? ')' '{' ( varDeclaration)* ( statement )* '}' #MainMethod
    ;


parameterList
    : nameType+=type name+=ID ( ',' nameType+=type name+=ID )* #Parameter
    ;

type
    : type '['']'      # ArrayType
    | name = 'boolean'         # BooleanType
    | name = 'int'             # IntegerType
    | name = 'void'            # VoidType
    | name = ID        # IDStringType
    ;

statement
    : '{' ( statement )* '}' #BlockStatement
    | 'if' '(' expression ')' statement 'else' statement #IfElseStatement
    | 'if' '(' expression ')' statement #IfStatement
    | 'while' '(' expression ')' statement #WhileStatement
    | expression ';' #GeneralStatement
    | var=ID '=' expression ';' #Assignment
    | ID '[' expression ']' '=' expression ';' #ArrayDeclaration
    ;


expression
    : '!' expression                                      #UnaryOp
    | expression op=('<' | '>' | '<=' | '>=' | '==' | '!=') expression  #BinaryOpCompare
    | expression op='&&' expression                     #BinaryOpLogical
    | expression op='||' expression                     #BinaryOpLogical
    | expression op=( '*' | '/' ) expression              #BinaryOpArithmetic
    | expression op=( '+' | '-') expression               #BinaryOpArithmetic
    | expression '[' expression ']'                       #ArrayAccess
    | 'new' name='int' '[' expression ']' #NewArrayDeclaration
    | expression '.' 'length'                             #Length
    | expression '.' methodName=ID '(' (expression (',' expression)*)? ')' #MethodCall
    | 'new' name=ID '(' ')'                                    #GeneralDeclaration
    | '(' expression ')'                                  #Brackets
    | name=INTEGER                                       #Integer
    | name=('true' | 'false')                                  #Boolean
    | name=ID                                            #Identifier
    | name='this'                                              #This
    ;
