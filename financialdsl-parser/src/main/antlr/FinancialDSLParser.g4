parser grammar FinancialDSLParser;

options { tokenVocab=FinancialDSLLexer; }

financialDSLFile : declarations+=topLevelDeclaration
                 ;

topLevelDeclaration : companyTypeDeclaration #companyTypeDecl
                    | taxDeclaration #taxDecl
                    ;

companyTypeDeclaration : COMPANY TYPE name=ID
                       ;

taxDeclaration : TAX ON target=entity LBRACE stmts+=taxDeclarationStmt RBRACE
               ;

taxDeclarationStmt : (limit=limitDefinition)? field=ID COLON value=expression
                   ;

limitDefinition : LBRACE RBRACE
                ;

expression : ID #referenceExpr
           ;

entity : ID #companyTypeEntity
       | PERSON #personEntity
       ;