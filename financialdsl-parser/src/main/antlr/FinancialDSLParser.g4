parser grammar FinancialDSLParser;

options { tokenVocab=FinancialDSLLexer; }

financialDSLFile : (declarations+=topLevelDeclaration)* EOF
                 ;

topLevelDeclaration : companyTypeDeclaration #companyTypeDecl
                    | taxDeclaration #taxDecl
                    | entityDeclaration #entityDecl
                    | countriesDeclaration #countriesDecl
                    | regionsDeclaration #regionsDecl
                    | citiesDeclaration #citiesDecl
                    ;

countriesDeclaration : COUNTRIES LBRACE (countries+=countryDeclaration)* RBRACE
                     ;

regionsDeclaration : REGIONS OF country=ID LBRACE (regions+=regionDeclaration)* RBRACE
                     ;

citiesDeclaration : CITIES OF region=ID LBRACE (cities+=cityDeclaration)* RBRACE
                     ;

countryDeclaration : name=ID (eu=EU)?
                   ;

regionDeclaration : name=ID
                   ;

cityDeclaration : name=ID
                ;

entityDeclaration : name=ID IS target=entityType LBRACE (stmts+=entityDeclarationStmt)* RBRACE
                  ;

entityDeclarationStmt : name=ID (IS type)?
                        ((EQUAL value=expression)|(IN_ARROW PARAMETER)|(IN_ARROW SUM))?
                        (OUT_ARROW CONTRIBUTES TO contributed=expression)?
                      ;

companyTypeDeclaration : COMPANY TYPE name=ID LBRACE (stmts+=companyTypeDeclarationStmt)* RBRACE
                       ;

companyTypeDeclarationStmt : name=ID (IS type)? ((EQUAL value=expression)|(IN_ARROW PARAMETER)|(IN_ARROW SUM))?
                           ;

type : AMOUNT
     ;

taxDeclaration : TAX name=ID ON target=entityType LBRACE (stmts+=taxDeclarationStmt)* RBRACE
               ;

taxDeclarationStmt : field=ID EQUAL value=expression
                   ;

date : MONTH year=INTLIT
     | year=INTLIT
     ;

expression : left=expression PLUS right=expression #sumExpr
           | LSQUARE (entries+=mapEntry (COMMA entries+=mapEntry)*)? RSQUARE #mapExpr
           | name=ID #referenceExpr
           | INTLIT #intLiteral
           | DECLIT #decimalLiteral
           | PERCLIT #percentageLiteral
           | expression PERIODICITY #periodicExpr
           | fieldName=ID OF expression #fieldAccessExpr
           | valueInTime+ #timeExpr
           | SHARE OF toShare=expression (FOR owner=expression)? #shareExpr
           | PERCLIT OF baseValue=expression #percentageExpr
           | clauses+=whenClause (COMMA clauses+=whenClause)* #whenExpr
           | left=expression EQUAL right=expression #equality
           ;

whenClause : WHEN condition=expression value=expression
           ;

valueInTime : timeClause expression
            ;

timeClause : TIMEOPEN (BEFORE|AFTER|SINCE) date RBRACE
           ;

mapEntry : expression AT expression
         ;

entityType : ID #companyTypeEntity
           | PERSON #personEntity
           ;