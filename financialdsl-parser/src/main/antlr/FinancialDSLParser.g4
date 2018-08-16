parser grammar FinancialDSLParser;

options { tokenVocab=FinancialDSLLexer; }

financialDSLFile : declarations+=topLevelDeclaration
                 ;

topLevelDeclaration : companyTypeDeclaration #companyTypeDecl
                    | taxDeclaration #taxDecl
                    ;

companyTypeDeclaration : COMPANY TYPE name=ID LBRACE (stmts+=companyTypeDeclarationStmt)* RBRACE
                       ;

companyTypeDeclarationStmt : name=ID IS type (EQUAL value=expression)?
                           ;

type : AMOUNT
     ;

taxDeclaration : TAX ON target=entity LBRACE (stmts+=taxDeclarationStmt)* RBRACE
               ;

taxDeclarationStmt : (limit=limitDefinition)? field=ID COLON value=expression
                   ;

limitDefinition : LBRACE RBRACE
                ;

expression : expression PLUS expression #sumExpr
           | ID #referenceExpr
           ;

entity : ID #companyTypeEntity
       | PERSON #personEntity
       ;