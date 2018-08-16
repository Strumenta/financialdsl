parser grammar FinancialDSLParser;

options { tokenVocab=FinancialDSLLexer; }

financialDSLFile : declarations+=topLevelDeclaration EOF
                 ;

topLevelDeclaration : companyTypeDeclaration #companyTypeDecl
                    | taxDeclaration #taxDecl
                    | entityDeclaration #entityDecl
                    ;

entityDeclaration : name=ID IS target=entityType LBRACE (stmts+=entityDeclarationStmt)* RBRACE
                  ;

entityDeclarationStmt : name=ID IS type (EQUAL value=expression)?
                      ;

companyTypeDeclaration : COMPANY TYPE name=ID LBRACE (stmts+=companyTypeDeclarationStmt)* RBRACE
                       ;

companyTypeDeclarationStmt : name=ID (IS type)? ((EQUAL value=expression)|(IN_ARROW PARAMETER)|(IN_ARROW SUM))?
                           ;

type : AMOUNT
     ;

taxDeclaration : TAX ON target=entityType LBRACE (stmts+=taxDeclarationStmt)* RBRACE
               ;

taxDeclarationStmt : (limit=limitDefinition)? field=ID COLON value=expression
                   ;

limitDefinition : LBRACE RBRACE
                ;

expression : expression PLUS expression #sumExpr
           | LSQUARE (entries+=mapEntry (COMMA entries+=mapEntry)*)? RSQUARE #mapExpr
           | ID #referenceExpr
           | INTLIT #intLiteral
           | DECLIT #decimalLiteral
           | PERCLIT #percentageLiteral
           ;

mapEntry : expression AT expression
         ;

entityType : ID #companyTypeEntity
           | PERSON #personEntity
           ;