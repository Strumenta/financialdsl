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

entityDeclarationStmt : (name=ID|name=OWNERS|name=CITY) (IS type)?
                        ((EQUAL value=expression)|(IN_ARROW PARAMETER)|(IN_ARROW SUM))?
                        (OUT_ARROW CONTRIBUTES TO contributed=contributionTarget)?
                      ;

contributionTarget : fieldName=ID #sameEntityContributionTarget
                   | fieldName=ID OF entityName=ID #otherEntityContributionTarget
                   | fieldName=ID OF OWNERS #ownersContributionTarget
                   ;

companyTypeDeclaration : COMPANY TYPE name=ID LBRACE (stmts+=companyTypeDeclarationStmt)* RBRACE
                       ;

companyTypeDeclarationStmt : name=ID (IS type)? ((EQUAL value=expression)|(IN_ARROW PARAMETER)|(IN_ARROW SUM))?
                           ;

type : AMOUNT
     ;

taxDeclaration : TAX name=ID ON target=entityType LBRACE (stmts+=taxDeclarationStmt)* RBRACE
               ;

taxDeclarationStmt : name=ID EQUAL value=expression
                   ;

date : MONTH year=INTLIT #monthDate
     | year=INTLIT #yearDate
     ;

expression : left=expression PLUS right=expression #sumExpr
           | LSQUARE (entries+=shareEntry (COMMA entries+=shareEntry)*)? RSQUARE #sharesMapExpr
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
           | BRACKETS entries+=bracketEntry (COMMA entries+=bracketEntry)* #bracketsExpr
           ;

bracketEntry : LSQUARE range RSQUARE OUT_ARROW value=expression
             ;

range : TO value=expression
      | ABOVE
      ;

whenClause : WHEN condition=expression value=expression
           ;

valueInTime : timeClause expression
            ;

timeClause : TIMEOPEN (BEFORE|AFTER|SINCE) date RBRACE
           ;

shareEntry : owner=expression AT value=expression
         ;

entityType : name=ID #companyTypeEntity
           | PERSON #personEntity
           ;