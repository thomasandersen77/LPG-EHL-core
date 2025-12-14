VERSION 5.00
Begin {C0E45035-5775-11D0-B388-00A0C9055D8E} lpgnorge 
   ClientHeight    =   9990
   ClientLeft      =   0
   ClientTop       =   0
   ClientWidth     =   13560
   _ExtentX        =   23918
   _ExtentY        =   17621
   FolderFlags     =   5
   TypeLibGuid     =   "{5916406A-2577-4E79-A3B4-B6D7BFF82A44}"
   TypeInfoGuid    =   "{CA520700-81EE-492F-AA85-56D6489F71F4}"
   TypeInfoCookie  =   0
   Version         =   4
   NumConnections  =   2
   BeginProperty Connection1 
      ConnectionName  =   "betterm"
      ConnDispId      =   1001
      SourceOfData    =   3
      ConnectionSource=   $"lpgnorge.dsx":0000
      Expanded        =   -1  'True
      IsSQL           =   -1  'True
      QuoteChar       =   34
      SeparatorChar   =   46
   EndProperty
   BeginProperty Connection2 
      ConnectionName  =   "butikkdata"
      ConnDispId      =   1015
      SourceOfData    =   3
      ConnectionSource=   "Provider=SQLOLEDB.1;Password=lpg01;Persist Security Info=True;User ID=sa;Initial Catalog=uni_1;Data Source=LPGROMERIKE_POS\UNI"
      Expanded        =   -1  'True
      IsSQL           =   -1  'True
      QuoteChar       =   34
      SeparatorChar   =   46
   EndProperty
   NumRecordsets   =   25
   BeginProperty Recordset1 
      CommandName     =   "stasjonskreditt"
      CommDispId      =   1002
      RsDispId        =   1064
      CommandText     =   "dbo.Stasjonskreditt_Tankinger"
      ActiveConnectionName=   "betterm"
      CommandType     =   2
      dbObjectType    =   1
      Locktype        =   3
      IsRSReturning   =   -1  'True
      NumFields       =   13
      BeginProperty Field1 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   20
         Name            =   "Tankid"
         Caption         =   "Tankid"
      EndProperty
      BeginProperty Field2 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   20
         Name            =   "kundeid"
         Caption         =   "kundeid"
      EndProperty
      BeginProperty Field3 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   20
         Name            =   "Unikundeid"
         Caption         =   "Unikundeid"
      EndProperty
      BeginProperty Field4 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   20
         Name            =   "Unikontaktid"
         Caption         =   "Unikontaktid"
      EndProperty
      BeginProperty Field5 
         Precision       =   23
         Size            =   16
         Scale           =   3
         Type            =   135
         Name            =   "Datostart"
         Caption         =   "Datostart"
      EndProperty
      BeginProperty Field6 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "Liter"
         Caption         =   "Liter"
      EndProperty
      BeginProperty Field7 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "Pris"
         Caption         =   "Pris"
      EndProperty
      BeginProperty Field8 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "sum"
         Caption         =   "sum"
      EndProperty
      BeginProperty Field9 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "rabatt"
         Caption         =   "rabatt"
      EndProperty
      BeginProperty Field10 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Status"
         Caption         =   "Status"
      EndProperty
      BeginProperty Field11 
         Precision       =   23
         Size            =   16
         Scale           =   3
         Type            =   135
         Name            =   "Datostopp"
         Caption         =   "Datostopp"
      EndProperty
      BeginProperty Field12 
         Precision       =   0
         Size            =   2
         Scale           =   0
         Type            =   11
         Name            =   "Transferred"
         Caption         =   "Transferred"
      EndProperty
      BeginProperty Field13 
         Precision       =   23
         Size            =   16
         Scale           =   3
         Type            =   135
         Name            =   "Transferdato"
         Caption         =   "Transferdato"
      EndProperty
      NumGroups       =   0
      ParamCount      =   0
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset2 
      CommandName     =   "ordre"
      CommDispId      =   1016
      RsDispId        =   1028
      CommandText     =   "SELECT * FROM ordre WHERE ordrenr = (SELECT (MAX(ordrenr)) FROM ordre)"
      ActiveConnectionName=   "butikkdata"
      CommandType     =   1
      Locktype        =   3
      IsRSReturning   =   -1  'True
      NumFields       =   75
      BeginProperty Field1 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Ordrenr"
         Caption         =   "Ordrenr"
      EndProperty
      BeginProperty Field2 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Kundenr"
         Caption         =   "Kundenr"
      EndProperty
      BeginProperty Field3 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "NameID"
         Caption         =   "NameID"
      EndProperty
      BeginProperty Field4 
         Precision       =   0
         Size            =   60
         Scale           =   0
         Type            =   200
         Name            =   "Kundenavn"
         Caption         =   "Kundenavn"
      EndProperty
      BeginProperty Field5 
         Precision       =   0
         Size            =   60
         Scale           =   0
         Type            =   200
         Name            =   "Adresse"
         Caption         =   "Adresse"
      EndProperty
      BeginProperty Field6 
         Precision       =   0
         Size            =   60
         Scale           =   0
         Type            =   200
         Name            =   "Adresse2"
         Caption         =   "Adresse2"
      EndProperty
      BeginProperty Field7 
         Precision       =   0
         Size            =   10
         Scale           =   0
         Type            =   200
         Name            =   "Postnr"
         Caption         =   "Postnr"
      EndProperty
      BeginProperty Field8 
         Precision       =   0
         Size            =   60
         Scale           =   0
         Type            =   200
         Name            =   "Poststed"
         Caption         =   "Poststed"
      EndProperty
      BeginProperty Field9 
         Precision       =   0
         Size            =   10
         Scale           =   0
         Type            =   200
         Name            =   "Landkode"
         Caption         =   "Landkode"
      EndProperty
      BeginProperty Field10 
         Precision       =   0
         Size            =   30
         Scale           =   0
         Type            =   200
         Name            =   "Land"
         Caption         =   "Land"
      EndProperty
      BeginProperty Field11 
         Precision       =   16
         Size            =   16
         Scale           =   0
         Type            =   135
         Name            =   "Ordredato"
         Caption         =   "Ordredato"
      EndProperty
      BeginProperty Field12 
         Precision       =   16
         Size            =   16
         Scale           =   0
         Type            =   135
         Name            =   "Leveringsdato"
         Caption         =   "Leveringsdato"
      EndProperty
      BeginProperty Field13 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Avgfritt_grunnlag"
         Caption         =   "Avgfritt_grunnlag"
      EndProperty
      BeginProperty Field14 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Behandlingsregel"
         Caption         =   "Behandlingsregel"
      EndProperty
      BeginProperty Field15 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Bet_maate"
         Caption         =   "Bet_maate"
      EndProperty
      BeginProperty Field16 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Dekningsbidrag"
         Caption         =   "Dekningsbidrag"
      EndProperty
      BeginProperty Field17 
         Precision       =   0
         Size            =   20
         Scale           =   0
         Type            =   200
         Name            =   "Deres_Ref"
         Caption         =   "Deres_Ref"
      EndProperty
      BeginProperty Field18 
         Precision       =   0
         Size            =   20
         Scale           =   0
         Type            =   200
         Name            =   "Vaar_Ref"
         Caption         =   "Vaar_Ref"
      EndProperty
      BeginProperty Field19 
         Precision       =   0
         Size            =   2147483647
         Scale           =   0
         Type            =   201
         Name            =   "Fritekst"
         Caption         =   "Fritekst"
      EndProperty
      BeginProperty Field20 
         Precision       =   0
         Size            =   100
         Scale           =   0
         Type            =   200
         Name            =   "Kommentarer"
         Caption         =   "Kommentarer"
      EndProperty
      BeginProperty Field21 
         Precision       =   0
         Size            =   2147483647
         Scale           =   0
         Type            =   201
         Name            =   "Levering"
         Caption         =   "Levering"
      EndProperty
      BeginProperty Field22 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Moms"
         Caption         =   "Moms"
      EndProperty
      BeginProperty Field23 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Momsprosent"
         Caption         =   "Momsprosent"
      EndProperty
      BeginProperty Field24 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Mvagrunnlag"
         Caption         =   "Mvagrunnlag"
      EndProperty
      BeginProperty Field25 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Nettosum"
         Caption         =   "Nettosum"
      EndProperty
      BeginProperty Field26 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Rabatt"
         Caption         =   "Rabatt"
      EndProperty
      BeginProperty Field27 
         Precision       =   16
         Size            =   16
         Scale           =   0
         Type            =   135
         Name            =   "Registreringsdato"
         Caption         =   "Registreringsdato"
      EndProperty
      BeginProperty Field28 
         Precision       =   0
         Size            =   60
         Scale           =   0
         Type            =   200
         Name            =   "Rekvisisjon"
         Caption         =   "Rekvisisjon"
      EndProperty
      BeginProperty Field29 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Selgernr1"
         Caption         =   "Selgernr1"
      EndProperty
      BeginProperty Field30 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Selgernr2"
         Caption         =   "Selgernr2"
      EndProperty
      BeginProperty Field31 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Spraak"
         Caption         =   "Spraak"
      EndProperty
      BeginProperty Field32 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Totalsum"
         Caption         =   "Totalsum"
      EndProperty
      BeginProperty Field33 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "Transportmaate"
         Caption         =   "Transportmaate"
      EndProperty
      BeginProperty Field34 
         Precision       =   0
         Size            =   40
         Scale           =   0
         Type            =   200
         Name            =   "Transport"
         Caption         =   "Transport"
      EndProperty
      BeginProperty Field35 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Transportoer"
         Caption         =   "Transportoer"
      EndProperty
      BeginProperty Field36 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Status"
         Caption         =   "Status"
      EndProperty
      BeginProperty Field37 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Type"
         Caption         =   "Type"
      EndProperty
      BeginProperty Field38 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Valuta"
         Caption         =   "Valuta"
      EndProperty
      BeginProperty Field39 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim1"
         Caption         =   "dim1"
      EndProperty
      BeginProperty Field40 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim2"
         Caption         =   "dim2"
      EndProperty
      BeginProperty Field41 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim3"
         Caption         =   "dim3"
      EndProperty
      BeginProperty Field42 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim4"
         Caption         =   "dim4"
      EndProperty
      BeginProperty Field43 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim5"
         Caption         =   "dim5"
      EndProperty
      BeginProperty Field44 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim6"
         Caption         =   "dim6"
      EndProperty
      BeginProperty Field45 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim7"
         Caption         =   "dim7"
      EndProperty
      BeginProperty Field46 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim8"
         Caption         =   "dim8"
      EndProperty
      BeginProperty Field47 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim9"
         Caption         =   "dim9"
      EndProperty
      BeginProperty Field48 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim10"
         Caption         =   "dim10"
      EndProperty
      BeginProperty Field49 
         Precision       =   0
         Size            =   10
         Scale           =   0
         Type            =   200
         Name            =   "ValutaKode"
         Caption         =   "ValutaKode"
      EndProperty
      BeginProperty Field50 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "ValutaSum"
         Caption         =   "ValutaSum"
      EndProperty
      BeginProperty Field51 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "ValutaForced"
         Caption         =   "ValutaForced"
      EndProperty
      BeginProperty Field52 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "GrlHoy"
         Caption         =   "GrlHoy"
      EndProperty
      BeginProperty Field53 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "GrlLav"
         Caption         =   "GrlLav"
      EndProperty
      BeginProperty Field54 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "MomsHoy"
         Caption         =   "MomsHoy"
      EndProperty
      BeginProperty Field55 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "MomsLav"
         Caption         =   "MomsLav"
      EndProperty
      BeginProperty Field56 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "PrintStatus"
         Caption         =   "PrintStatus"
      EndProperty
      BeginProperty Field57 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "ExportHandTerm"
         Caption         =   "ExportHandTerm"
      EndProperty
      BeginProperty Field58 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Assigned1"
         Caption         =   "Assigned1"
      EndProperty
      BeginProperty Field59 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Assigned2"
         Caption         =   "Assigned2"
      EndProperty
      BeginProperty Field60 
         Precision       =   0
         Size            =   2147483647
         Scale           =   0
         Type            =   201
         Name            =   "Internal_comment"
         Caption         =   "Internal_comment"
      EndProperty
      BeginProperty Field61 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "BetBetingelse"
         Caption         =   "BetBetingelse"
      EndProperty
      BeginProperty Field62 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "LevBetingelse"
         Caption         =   "LevBetingelse"
      EndProperty
      BeginProperty Field63 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "Signatur"
         Caption         =   "Signatur"
      EndProperty
      BeginProperty Field64 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "ValgtLagerID"
         Caption         =   "ValgtLagerID"
      EndProperty
      BeginProperty Field65 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "ms_ordretype"
         Caption         =   "ms_ordretype"
      EndProperty
      BeginProperty Field66 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "ms_fastpris"
         Caption         =   "ms_fastpris"
      EndProperty
      BeginProperty Field67 
         Precision       =   5
         Size            =   2
         Scale           =   0
         Type            =   2
         Name            =   "ms_avsluttet"
         Caption         =   "ms_avsluttet"
      EndProperty
      BeginProperty Field68 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "MS_PriceCollectionRef"
         Caption         =   "MS_PriceCollectionRef"
      EndProperty
      BeginProperty Field69 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Selgernr3"
         Caption         =   "Selgernr3"
      EndProperty
      BeginProperty Field70 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Selgernr4"
         Caption         =   "Selgernr4"
      EndProperty
      BeginProperty Field71 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Selgernr5"
         Caption         =   "Selgernr5"
      EndProperty
      BeginProperty Field72 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Rabattavtalenr"
         Caption         =   "Rabattavtalenr"
      EndProperty
      BeginProperty Field73 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "fraktid"
         Caption         =   "fraktid"
      EndProperty
      BeginProperty Field74 
         Precision       =   16
         Size            =   16
         Scale           =   0
         Type            =   135
         Name            =   "finishedDate"
         Caption         =   "finishedDate"
      EndProperty
      BeginProperty Field75 
         Precision       =   16
         Size            =   16
         Scale           =   0
         Type            =   135
         Name            =   "LastEdit"
         Caption         =   "LastEdit"
      EndProperty
      NumGroups       =   0
      ParamCount      =   0
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset3 
      CommandName     =   "Navnaddress"
      CommDispId      =   1019
      RsDispId        =   1021
      CommandText     =   $"lpgnorge.dsx":0088
      ActiveConnectionName=   "butikkdata"
      CommandType     =   1
      IsRSReturning   =   -1  'True
      NumFields       =   6
      BeginProperty Field1 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "kontonr"
         Caption         =   "kontonr"
      EndProperty
      BeginProperty Field2 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "name"
         Caption         =   "name"
      EndProperty
      BeginProperty Field3 
         Precision       =   0
         Size            =   60
         Scale           =   0
         Type            =   200
         Name            =   "address"
         Caption         =   "address"
      EndProperty
      BeginProperty Field4 
         Precision       =   0
         Size            =   60
         Scale           =   0
         Type            =   200
         Name            =   "address2"
         Caption         =   "address2"
      EndProperty
      BeginProperty Field5 
         Precision       =   0
         Size            =   10
         Scale           =   0
         Type            =   200
         Name            =   "postal_code"
         Caption         =   "postal_code"
      EndProperty
      BeginProperty Field6 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "city"
         Caption         =   "city"
      EndProperty
      NumGroups       =   0
      ParamCount      =   0
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset4 
      CommandName     =   "ordrelinje"
      CommDispId      =   1029
      RsDispId        =   1035
      CommandText     =   "select * from varelnordre where id=(select max(id) from varelnordre)"
      ActiveConnectionName=   "butikkdata"
      CommandType     =   1
      Locktype        =   3
      IsRSReturning   =   -1  'True
      NumFields       =   72
      BeginProperty Field1 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "ID"
         Caption         =   "ID"
      EndProperty
      BeginProperty Field2 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Ordrenr"
         Caption         =   "Ordrenr"
      EndProperty
      BeginProperty Field3 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Kundenr"
         Caption         =   "Kundenr"
      EndProperty
      BeginProperty Field4 
         Precision       =   0
         Size            =   20
         Scale           =   0
         Type            =   200
         Name            =   "Varenr"
         Caption         =   "Varenr"
      EndProperty
      BeginProperty Field5 
         Precision       =   0
         Size            =   150
         Scale           =   0
         Type            =   200
         Name            =   "Varetekst"
         Caption         =   "Varetekst"
      EndProperty
      BeginProperty Field6 
         Precision       =   0
         Size            =   20
         Scale           =   0
         Type            =   200
         Name            =   "ErstatnVareid"
         Caption         =   "ErstatnVareid"
      EndProperty
      BeginProperty Field7 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Antall"
         Caption         =   "Antall"
      EndProperty
      BeginProperty Field8 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Antall_lev"
         Caption         =   "Antall_lev"
      EndProperty
      BeginProperty Field9 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Antall_fakt"
         Caption         =   "Antall_fakt"
      EndProperty
      BeginProperty Field10 
         Precision       =   0
         Size            =   10
         Scale           =   0
         Type            =   200
         Name            =   "Enhet"
         Caption         =   "Enhet"
      EndProperty
      BeginProperty Field11 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "InnPris"
         Caption         =   "InnPris"
      EndProperty
      BeginProperty Field12 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Kostnad"
         Caption         =   "Kostnad"
      EndProperty
      BeginProperty Field13 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Kontonr"
         Caption         =   "Kontonr"
      EndProperty
      BeginProperty Field14 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Momskode"
         Caption         =   "Momskode"
      EndProperty
      BeginProperty Field15 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Pris"
         Caption         =   "Pris"
      EndProperty
      BeginProperty Field16 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Rabatt"
         Caption         =   "Rabatt"
      EndProperty
      BeginProperty Field17 
         Precision       =   0
         Size            =   20
         Scale           =   0
         Type            =   200
         Name            =   "Serienr"
         Caption         =   "Serienr"
      EndProperty
      BeginProperty Field18 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "LSum"
         Caption         =   "LSum"
      EndProperty
      BeginProperty Field19 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Type"
         Caption         =   "Type"
      EndProperty
      BeginProperty Field20 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Status"
         Caption         =   "Status"
      EndProperty
      BeginProperty Field21 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Db"
         Caption         =   "Db"
      EndProperty
      BeginProperty Field22 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Netto"
         Caption         =   "Netto"
      EndProperty
      BeginProperty Field23 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Mva"
         Caption         =   "Mva"
      EndProperty
      BeginProperty Field24 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "AB_ID"
         Caption         =   "AB_ID"
      EndProperty
      BeginProperty Field25 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "AB_AVREGNID"
         Caption         =   "AB_AVREGNID"
      EndProperty
      BeginProperty Field26 
         Precision       =   0
         Size            =   20
         Scale           =   0
         Type            =   200
         Name            =   "GrpKode"
         Caption         =   "GrpKode"
      EndProperty
      BeginProperty Field27 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "kun_til_samlefaktura"
         Caption         =   "kun_til_samlefaktura"
      EndProperty
      BeginProperty Field28 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim1"
         Caption         =   "dim1"
      EndProperty
      BeginProperty Field29 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim2"
         Caption         =   "dim2"
      EndProperty
      BeginProperty Field30 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim3"
         Caption         =   "dim3"
      EndProperty
      BeginProperty Field31 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim4"
         Caption         =   "dim4"
      EndProperty
      BeginProperty Field32 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim5"
         Caption         =   "dim5"
      EndProperty
      BeginProperty Field33 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim6"
         Caption         =   "dim6"
      EndProperty
      BeginProperty Field34 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim7"
         Caption         =   "dim7"
      EndProperty
      BeginProperty Field35 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim8"
         Caption         =   "dim8"
      EndProperty
      BeginProperty Field36 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim9"
         Caption         =   "dim9"
      EndProperty
      BeginProperty Field37 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim10"
         Caption         =   "dim10"
      EndProperty
      BeginProperty Field38 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Komp_lagertype"
         Caption         =   "Komp_lagertype"
      EndProperty
      BeginProperty Field39 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Komp_visningstype"
         Caption         =   "Komp_visningstype"
      EndProperty
      BeginProperty Field40 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Komp_pristype"
         Caption         =   "Komp_pristype"
      EndProperty
      BeginProperty Field41 
         Precision       =   0
         Size            =   10
         Scale           =   0
         Type            =   200
         Name            =   "ValutaKode"
         Caption         =   "ValutaKode"
      EndProperty
      BeginProperty Field42 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "ValutaLSum"
         Caption         =   "ValutaLSum"
      EndProperty
      BeginProperty Field43 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "ValutaPrice"
         Caption         =   "ValutaPrice"
      EndProperty
      BeginProperty Field44 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Komp_level"
         Caption         =   "Komp_level"
      EndProperty
      BeginProperty Field45 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Momsprosent"
         Caption         =   "Momsprosent"
      EndProperty
      BeginProperty Field46 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "PlukkID"
         Caption         =   "PlukkID"
      EndProperty
      BeginProperty Field47 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "RenteJobbID"
         Caption         =   "RenteJobbID"
      EndProperty
      BeginProperty Field48 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "DeliveryLink"
         Caption         =   "DeliveryLink"
      EndProperty
      BeginProperty Field49 
         Precision       =   0
         Size            =   20
         Scale           =   0
         Type            =   200
         Name            =   "itemnr"
         Caption         =   "itemnr"
      EndProperty
      BeginProperty Field50 
         Precision       =   16
         Size            =   16
         Scale           =   0
         Type            =   135
         Name            =   "leveringsdato"
         Caption         =   "leveringsdato"
      EndProperty
      BeginProperty Field51 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "TrackingID"
         Caption         =   "TrackingID"
      EndProperty
      BeginProperty Field52 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "div1"
         Caption         =   "div1"
      EndProperty
      BeginProperty Field53 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "weight_per_unit"
         Caption         =   "weight_per_unit"
      EndProperty
      BeginProperty Field54 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "weight_total"
         Caption         =   "weight_total"
      EndProperty
      BeginProperty Field55 
         Precision       =   15
         Size            =   8
         Scale           =   0
         Type            =   5
         Name            =   "dimension_x"
         Caption         =   "dimension_x"
      EndProperty
      BeginProperty Field56 
         Precision       =   15
         Size            =   8
         Scale           =   0
         Type            =   5
         Name            =   "dimension_y"
         Caption         =   "dimension_y"
      EndProperty
      BeginProperty Field57 
         Precision       =   15
         Size            =   8
         Scale           =   0
         Type            =   5
         Name            =   "dimension_z"
         Caption         =   "dimension_z"
      EndProperty
      BeginProperty Field58 
         Precision       =   15
         Size            =   8
         Scale           =   0
         Type            =   5
         Name            =   "dimension_p"
         Caption         =   "dimension_p"
      EndProperty
      BeginProperty Field59 
         Precision       =   15
         Size            =   8
         Scale           =   0
         Type            =   5
         Name            =   "dimension_n"
         Caption         =   "dimension_n"
      EndProperty
      BeginProperty Field60 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "MS_GroupInvoiceRef"
         Caption         =   "MS_GroupInvoiceRef"
      EndProperty
      BeginProperty Field61 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "anbudtype"
         Caption         =   "anbudtype"
      EndProperty
      BeginProperty Field62 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "ExternalInvoiceRowID"
         Caption         =   "ExternalInvoiceRowID"
      EndProperty
      BeginProperty Field63 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Snittpris"
         Caption         =   "Snittpris"
      EndProperty
      BeginProperty Field64 
         Precision       =   16
         Size            =   16
         Scale           =   0
         Type            =   135
         Name            =   "Registreringsdato"
         Caption         =   "Registreringsdato"
      EndProperty
      BeginProperty Field65 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "AvgMalId"
         Caption         =   "AvgMalId"
      EndProperty
      BeginProperty Field66 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "AvgMalType"
         Caption         =   "AvgMalType"
      EndProperty
      BeginProperty Field67 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "MiljoavgForID"
         Caption         =   "MiljoavgForID"
      EndProperty
      BeginProperty Field68 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "fraktLinjeID"
         Caption         =   "fraktLinjeID"
      EndProperty
      BeginProperty Field69 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "levOrg_ant"
         Caption         =   "levOrg_ant"
      EndProperty
      BeginProperty Field70 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "levTracknr"
         Caption         =   "levTracknr"
      EndProperty
      BeginProperty Field71 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "SC_JobNrRef"
         Caption         =   "SC_JobNrRef"
      EndProperty
      BeginProperty Field72 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Selgerid"
         Caption         =   "Selgerid"
      EndProperty
      NumGroups       =   0
      ParamCount      =   0
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset5 
      CommandName     =   "cashback"
      CommDispId      =   1038
      RsDispId        =   1047
      CommandText     =   "Select * from cashback where reported=0"
      ActiveConnectionName=   "betterm"
      CommandType     =   1
      Locktype        =   3
      IsRSReturning   =   -1  'True
      NumFields       =   5
      BeginProperty Field1 
         Precision       =   18
         Size            =   19
         Scale           =   0
         Type            =   131
         Name            =   "id"
         Caption         =   "id"
      EndProperty
      BeginProperty Field2 
         Precision       =   16
         Size            =   16
         Scale           =   0
         Type            =   135
         Name            =   "dato"
         Caption         =   "dato"
      EndProperty
      BeginProperty Field3 
         Precision       =   0
         Size            =   20
         Scale           =   0
         Type            =   200
         Name            =   "Carddata"
         Caption         =   "Carddata"
      EndProperty
      BeginProperty Field4 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "belop"
         Caption         =   "belop"
      EndProperty
      BeginProperty Field5 
         Precision       =   0
         Size            =   2
         Scale           =   0
         Type            =   11
         Name            =   "Reported"
         Caption         =   "Reported"
      EndProperty
      NumGroups       =   0
      ParamCount      =   0
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset6 
      CommandName     =   "firmainfo"
      CommDispId      =   1048
      RsDispId        =   1050
      CommandText     =   "dbo.Firmainfo"
      ActiveConnectionName=   "betterm"
      CommandType     =   2
      dbObjectType    =   1
      Locktype        =   3
      IsRSReturning   =   -1  'True
      NumFields       =   9
      BeginProperty Field1 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "Firmanavn"
         Caption         =   "Firmanavn"
      EndProperty
      BeginProperty Field2 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "Firmaadresse"
         Caption         =   "Firmaadresse"
      EndProperty
      BeginProperty Field3 
         Precision       =   0
         Size            =   10
         Scale           =   0
         Type            =   200
         Name            =   "Firmapostnr"
         Caption         =   "Firmapostnr"
      EndProperty
      BeginProperty Field4 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "Firmapoststed"
         Caption         =   "Firmapoststed"
      EndProperty
      BeginProperty Field5 
         Precision       =   0
         Size            =   10
         Scale           =   0
         Type            =   200
         Name            =   "Telefonnummer"
         Caption         =   "Telefonnummer"
      EndProperty
      BeginProperty Field6 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "Epost"
         Caption         =   "Epost"
      EndProperty
      BeginProperty Field7 
         Precision       =   0
         Size            =   20
         Scale           =   0
         Type            =   130
         Name            =   "Orgnr"
         Caption         =   "Orgnr"
      EndProperty
      BeginProperty Field8 
         Precision       =   0
         Size            =   255
         Scale           =   0
         Type            =   200
         Name            =   "aapningstider"
         Caption         =   "aapningstider"
      EndProperty
      BeginProperty Field9 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "technical_email"
         Caption         =   "technical_email"
      EndProperty
      NumGroups       =   0
      ParamCount      =   0
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset7 
      CommandName     =   "kvittering_pr_dag"
      CommDispId      =   1051
      RsDispId        =   1053
      CommandText     =   "dbo.kvittering_pr_dag"
      ActiveConnectionName=   "betterm"
      CallSyntax      =   "{? = CALL dbo.kvittering_pr_dag( ?, ?, ?) }"
      IsRSReturning   =   -1  'True
      NumFields       =   4
      BeginProperty Field1 
         Precision       =   18
         Size            =   19
         Scale           =   0
         Type            =   131
         Name            =   "reportid"
         Caption         =   "reportid"
      EndProperty
      BeginProperty Field2 
         Precision       =   0
         Size            =   2147483647
         Scale           =   0
         Type            =   201
         Name            =   "reporttext"
         Caption         =   "reporttext"
      EndProperty
      BeginProperty Field3 
         Precision       =   0
         Size            =   30
         Scale           =   0
         Type            =   200
         Name            =   "type"
         Caption         =   "type"
      EndProperty
      BeginProperty Field4 
         Precision       =   16
         Size            =   16
         Scale           =   0
         Type            =   135
         Name            =   "dato"
         Caption         =   "dato"
      EndProperty
      NumGroups       =   0
      ParamCount      =   4
      BeginProperty P1 
         RealName        =   "@RETURN_VALUE"
         UserName        =   "RETURN_VALUE"
         Direction       =   4
         Precision       =   10
         Scale           =   0
         Size            =   0
         DataType        =   3
         HostType        =   3
         Required        =   0   'False
      EndProperty
      BeginProperty P2 
         RealName        =   "@dag"
         Direction       =   1
         Precision       =   10
         Scale           =   0
         Size            =   0
         DataType        =   3
         HostType        =   3
         Required        =   -1  'True
      EndProperty
      BeginProperty P3 
         RealName        =   "@mnd"
         Direction       =   1
         Precision       =   10
         Scale           =   0
         Size            =   0
         DataType        =   3
         HostType        =   3
         Required        =   -1  'True
      EndProperty
      BeginProperty P4 
         RealName        =   "@aar"
         Direction       =   1
         Precision       =   10
         Scale           =   0
         Size            =   0
         DataType        =   3
         HostType        =   3
         Required        =   -1  'True
      EndProperty
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset8 
      CommandName     =   "Settings"
      CommDispId      =   1055
      RsDispId        =   1059
      CommandText     =   "dbo.settings"
      ActiveConnectionName=   "betterm"
      CommandType     =   2
      dbObjectType    =   1
      Locktype        =   3
      IsRSReturning   =   -1  'True
      NumFields       =   21
      BeginProperty Field1 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "termsqlserver"
         Caption         =   "termsqlserver"
      EndProperty
      BeginProperty Field2 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "termdb"
         Caption         =   "termdb"
      EndProperty
      BeginProperty Field3 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "termuid"
         Caption         =   "termuid"
      EndProperty
      BeginProperty Field4 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "termpw"
         Caption         =   "termpw"
      EndProperty
      BeginProperty Field5 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "possqlserver"
         Caption         =   "possqlserver"
      EndProperty
      BeginProperty Field6 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "posdb"
         Caption         =   "posdb"
      EndProperty
      BeginProperty Field7 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "posui"
         Caption         =   "posui"
      EndProperty
      BeginProperty Field8 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "pospw"
         Caption         =   "pospw"
      EndProperty
      BeginProperty Field9 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "Pathexportautogas"
         Caption         =   "Pathexportautogas"
      EndProperty
      BeginProperty Field10 
         Precision       =   5
         Size            =   2
         Scale           =   0
         Type            =   2
         Name            =   "Disp_comport"
         Caption         =   "Disp_comport"
      EndProperty
      BeginProperty Field11 
         Precision       =   5
         Size            =   2
         Scale           =   0
         Type            =   2
         Name            =   "RS485autogas"
         Caption         =   "RS485autogas"
      EndProperty
      BeginProperty Field12 
         Precision       =   5
         Size            =   2
         Scale           =   0
         Type            =   2
         Name            =   "RS485container"
         Caption         =   "RS485container"
      EndProperty
      BeginProperty Field13 
         Precision       =   5
         Size            =   2
         Scale           =   0
         Type            =   2
         Name            =   "Paymentpinpad_comport"
         Caption         =   "Paymentpinpad_comport"
      EndProperty
      BeginProperty Field14 
         Precision       =   5
         Size            =   2
         Scale           =   0
         Type            =   2
         Name            =   "Reciptprinter_comport"
         Caption         =   "Reciptprinter_comport"
      EndProperty
      BeginProperty Field15 
         Precision       =   5
         Size            =   2
         Scale           =   0
         Type            =   2
         Name            =   "Reciptprinter_printerfeed"
         Caption         =   "Reciptprinter_printerfeed"
      EndProperty
      BeginProperty Field16 
         Precision       =   5
         Size            =   2
         Scale           =   0
         Type            =   2
         Name            =   "Pinpad_comport"
         Caption         =   "Pinpad_comport"
      EndProperty
      BeginProperty Field17 
         Precision       =   0
         Size            =   7
         Scale           =   0
         Type            =   200
         Name            =   "F1_value"
         Caption         =   "F1_value"
      EndProperty
      BeginProperty Field18 
         Precision       =   0
         Size            =   7
         Scale           =   0
         Type            =   200
         Name            =   "F2_value"
         Caption         =   "F2_value"
      EndProperty
      BeginProperty Field19 
         Precision       =   0
         Size            =   7
         Scale           =   0
         Type            =   200
         Name            =   "F3_value"
         Caption         =   "F3_value"
      EndProperty
      BeginProperty Field20 
         Precision       =   0
         Size            =   7
         Scale           =   0
         Type            =   200
         Name            =   "F4_value"
         Caption         =   "F4_value"
      EndProperty
      BeginProperty Field21 
         Precision       =   5
         Size            =   2
         Scale           =   0
         Type            =   2
         Name            =   "RFID_comport"
         Caption         =   "RFID_comport"
      EndProperty
      NumGroups       =   0
      ParamCount      =   0
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset9 
      CommandName     =   "stasjonskred"
      CommDispId      =   1060
      RsDispId        =   1062
      CommandText     =   $"lpgnorge.dsx":0153
      ActiveConnectionName=   "butikkdata"
      CommandType     =   1
      Expanded        =   -1  'True
      IsRSReturning   =   -1  'True
      NumFields       =   3
      BeginProperty Field1 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "kontonr"
         Caption         =   "kontonr"
      EndProperty
      BeginProperty Field2 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "id"
         Caption         =   "id"
      EndProperty
      BeginProperty Field3 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "kortnummer"
         Caption         =   "kortnummer"
      EndProperty
      NumGroups       =   0
      ParamCount      =   0
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset10 
      CommandName     =   "kunder"
      CommDispId      =   1065
      RsDispId        =   1070
      CommandText     =   "dbo.Kunder"
      ActiveConnectionName=   "betterm"
      CommandType     =   2
      dbObjectType    =   1
      Locktype        =   3
      IsRSReturning   =   -1  'True
      NumFields       =   7
      BeginProperty Field1 
         Precision       =   18
         Size            =   19
         Scale           =   0
         Type            =   131
         Name            =   "Kundeid"
         Caption         =   "Kundeid"
      EndProperty
      BeginProperty Field2 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "Kundenavn"
         Caption         =   "Kundenavn"
      EndProperty
      BeginProperty Field3 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "Korteier"
         Caption         =   "Korteier"
      EndProperty
      BeginProperty Field4 
         Precision       =   0
         Size            =   14
         Scale           =   0
         Type            =   200
         Name            =   "Kortnummer"
         Caption         =   "Kortnummer"
      EndProperty
      BeginProperty Field5 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "Periodekreditt"
         Caption         =   "Periodekreditt"
      EndProperty
      BeginProperty Field6 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "Rabatt"
         Caption         =   "Rabatt"
      EndProperty
      BeginProperty Field7 
         Precision       =   0
         Size            =   2
         Scale           =   0
         Type            =   11
         Name            =   "Aktiv"
         Caption         =   "Aktiv"
      EndProperty
      NumGroups       =   0
      ParamCount      =   0
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset11 
      CommandName     =   "varer"
      CommDispId      =   1071
      RsDispId        =   1073
      CommandText     =   "dbo.VARER"
      ActiveConnectionName=   "butikkdata"
      CommandType     =   2
      dbObjectType    =   1
      Locktype        =   3
      IsRSReturning   =   -1  'True
      NumFields       =   94
      BeginProperty Field1 
         Precision       =   0
         Size            =   20
         Scale           =   0
         Type            =   200
         Name            =   "Varenr"
         Caption         =   "Varenr"
      EndProperty
      BeginProperty Field2 
         Precision       =   0
         Size            =   60
         Scale           =   0
         Type            =   200
         Name            =   "Varenavn1"
         Caption         =   "Varenavn1"
      EndProperty
      BeginProperty Field3 
         Precision       =   0
         Size            =   60
         Scale           =   0
         Type            =   200
         Name            =   "Varenavn2"
         Caption         =   "Varenavn2"
      EndProperty
      BeginProperty Field4 
         Precision       =   0
         Size            =   60
         Scale           =   0
         Type            =   200
         Name            =   "Varenavn3"
         Caption         =   "Varenavn3"
      EndProperty
      BeginProperty Field5 
         Precision       =   0
         Size            =   6
         Scale           =   0
         Type            =   200
         Name            =   "Enhet"
         Caption         =   "Enhet"
      EndProperty
      BeginProperty Field6 
         Precision       =   0
         Size            =   20
         Scale           =   0
         Type            =   200
         Name            =   "ErstatnVareid"
         Caption         =   "ErstatnVareid"
      EndProperty
      BeginProperty Field7 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Status"
         Caption         =   "Status"
      EndProperty
      BeginProperty Field8 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "KomponentID"
         Caption         =   "KomponentID"
      EndProperty
      BeginProperty Field9 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Kontonr_avgpliktig"
         Caption         =   "Kontonr_avgpliktig"
      EndProperty
      BeginProperty Field10 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Kontonr_avgpliktig2"
         Caption         =   "Kontonr_avgpliktig2"
      EndProperty
      BeginProperty Field11 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Kontonr_avgfritt"
         Caption         =   "Kontonr_avgfritt"
      EndProperty
      BeginProperty Field12 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Kontonr_utenfor_avgift"
         Caption         =   "Kontonr_utenfor_avgift"
      EndProperty
      BeginProperty Field13 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "KontonrInkj"
         Caption         =   "KontonrInkj"
      EndProperty
      BeginProperty Field14 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Kontonr_lager"
         Caption         =   "Kontonr_lager"
      EndProperty
      BeginProperty Field15 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Momsind"
         Caption         =   "Momsind"
      EndProperty
      BeginProperty Field16 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Innpris_selvkost"
         Caption         =   "Innpris_selvkost"
      EndProperty
      BeginProperty Field17 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "kostnader"
         Caption         =   "kostnader"
      EndProperty
      BeginProperty Field18 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "kostnader_psnt"
         Caption         =   "kostnader_psnt"
      EndProperty
      BeginProperty Field19 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Avanse"
         Caption         =   "Avanse"
      EndProperty
      BeginProperty Field20 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Avanse_psnt"
         Caption         =   "Avanse_psnt"
      EndProperty
      BeginProperty Field21 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Paslag"
         Caption         =   "Paslag"
      EndProperty
      BeginProperty Field22 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Paslag_psnt"
         Caption         =   "Paslag_psnt"
      EndProperty
      BeginProperty Field23 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Pris1"
         Caption         =   "Pris1"
      EndProperty
      BeginProperty Field24 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Pris2"
         Caption         =   "Pris2"
      EndProperty
      BeginProperty Field25 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Pris3"
         Caption         =   "Pris3"
      EndProperty
      BeginProperty Field26 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "keep_paslag_nelfo"
         Caption         =   "keep_paslag_nelfo"
      EndProperty
      BeginProperty Field27 
         Precision       =   0
         Size            =   2
         Scale           =   0
         Type            =   11
         Name            =   "Serienr"
         Caption         =   "Serienr"
      EndProperty
      BeginProperty Field28 
         Precision       =   0
         Size            =   20
         Scale           =   0
         Type            =   200
         Name            =   "Varegruppe"
         Caption         =   "Varegruppe"
      EndProperty
      BeginProperty Field29 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Vekt"
         Caption         =   "Vekt"
      EndProperty
      BeginProperty Field30 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Volum"
         Caption         =   "Volum"
      EndProperty
      BeginProperty Field31 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Antall_pr_Kolli"
         Caption         =   "Antall_pr_Kolli"
      EndProperty
      BeginProperty Field32 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim1"
         Caption         =   "dim1"
      EndProperty
      BeginProperty Field33 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim2"
         Caption         =   "dim2"
      EndProperty
      BeginProperty Field34 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim3"
         Caption         =   "dim3"
      EndProperty
      BeginProperty Field35 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim4"
         Caption         =   "dim4"
      EndProperty
      BeginProperty Field36 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim5"
         Caption         =   "dim5"
      EndProperty
      BeginProperty Field37 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim6"
         Caption         =   "dim6"
      EndProperty
      BeginProperty Field38 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim7"
         Caption         =   "dim7"
      EndProperty
      BeginProperty Field39 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim8"
         Caption         =   "dim8"
      EndProperty
      BeginProperty Field40 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim9"
         Caption         =   "dim9"
      EndProperty
      BeginProperty Field41 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "dim10"
         Caption         =   "dim10"
      EndProperty
      BeginProperty Field42 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Komp_lagertype"
         Caption         =   "Komp_lagertype"
      EndProperty
      BeginProperty Field43 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Komp_visningstype"
         Caption         =   "Komp_visningstype"
      EndProperty
      BeginProperty Field44 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Komp_pristype"
         Caption         =   "Komp_pristype"
      EndProperty
      BeginProperty Field45 
         Precision       =   0
         Size            =   2147483647
         Scale           =   0
         Type            =   201
         Name            =   "Produktbeskrivelse"
         Caption         =   "Produktbeskrivelse"
      EndProperty
      BeginProperty Field46 
         Precision       =   0
         Size            =   255
         Scale           =   0
         Type            =   200
         Name            =   "PicturePath"
         Caption         =   "PicturePath"
      EndProperty
      BeginProperty Field47 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "PictureID"
         Caption         =   "PictureID"
      EndProperty
      BeginProperty Field48 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "DocumentID"
         Caption         =   "DocumentID"
      EndProperty
      BeginProperty Field49 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "UserDocID"
         Caption         =   "UserDocID"
      EndProperty
      BeginProperty Field50 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Webshop"
         Caption         =   "Webshop"
      EndProperty
      BeginProperty Field51 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "LagerType"
         Caption         =   "LagerType"
      EndProperty
      BeginProperty Field52 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "KoeType"
         Caption         =   "KoeType"
      EndProperty
      BeginProperty Field53 
         Precision       =   15
         Size            =   8
         Scale           =   0
         Type            =   5
         Name            =   "MinBeholdning"
         Caption         =   "MinBeholdning"
      EndProperty
      BeginProperty Field54 
         Precision       =   15
         Size            =   8
         Scale           =   0
         Type            =   5
         Name            =   "Bestillingskvantum"
         Caption         =   "Bestillingskvantum"
      EndProperty
      BeginProperty Field55 
         Precision       =   15
         Size            =   8
         Scale           =   0
         Type            =   5
         Name            =   "MaxLager"
         Caption         =   "MaxLager"
      EndProperty
      BeginProperty Field56 
         Precision       =   16
         Size            =   16
         Scale           =   0
         Type            =   135
         Name            =   "LastEdit"
         Caption         =   "LastEdit"
      EndProperty
      BeginProperty Field57 
         Precision       =   0
         Size            =   20
         Scale           =   0
         Type            =   200
         Name            =   "VariansParent"
         Caption         =   "VariansParent"
      EndProperty
      BeginProperty Field58 
         Precision       =   0
         Size            =   40
         Scale           =   0
         Type            =   200
         Name            =   "vdim1"
         Caption         =   "vdim1"
      EndProperty
      BeginProperty Field59 
         Precision       =   0
         Size            =   40
         Scale           =   0
         Type            =   200
         Name            =   "vdim2"
         Caption         =   "vdim2"
      EndProperty
      BeginProperty Field60 
         Precision       =   0
         Size            =   40
         Scale           =   0
         Type            =   200
         Name            =   "vdim3"
         Caption         =   "vdim3"
      EndProperty
      BeginProperty Field61 
         Precision       =   0
         Size            =   40
         Scale           =   0
         Type            =   200
         Name            =   "vdim4"
         Caption         =   "vdim4"
      EndProperty
      BeginProperty Field62 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Valutainnpris"
         Caption         =   "Valutainnpris"
      EndProperty
      BeginProperty Field63 
         Precision       =   0
         Size            =   10
         Scale           =   0
         Type            =   200
         Name            =   "Valutakode"
         Caption         =   "Valutakode"
      EndProperty
      BeginProperty Field64 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Valutakurs"
         Caption         =   "Valutakurs"
      EndProperty
      BeginProperty Field65 
         Precision       =   0
         Size            =   2
         Scale           =   0
         Type            =   11
         Name            =   "NotPurchase"
         Caption         =   "NotPurchase"
      EndProperty
      BeginProperty Field66 
         Precision       =   0
         Size            =   2147483647
         Scale           =   0
         Type            =   201
         Name            =   "Kommentar"
         Caption         =   "Kommentar"
      EndProperty
      BeginProperty Field67 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Snittpris"
         Caption         =   "Snittpris"
      EndProperty
      BeginProperty Field68 
         Precision       =   15
         Size            =   8
         Scale           =   0
         Type            =   5
         Name            =   "dimension_x"
         Caption         =   "dimension_x"
      EndProperty
      BeginProperty Field69 
         Precision       =   15
         Size            =   8
         Scale           =   0
         Type            =   5
         Name            =   "dimension_y"
         Caption         =   "dimension_y"
      EndProperty
      BeginProperty Field70 
         Precision       =   15
         Size            =   8
         Scale           =   0
         Type            =   5
         Name            =   "dimension_z"
         Caption         =   "dimension_z"
      EndProperty
      BeginProperty Field71 
         Precision       =   15
         Size            =   8
         Scale           =   0
         Type            =   5
         Name            =   "dimension_p"
         Caption         =   "dimension_p"
      EndProperty
      BeginProperty Field72 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "IsFarligGods"
         Caption         =   "IsFarligGods"
      EndProperty
      BeginProperty Field73 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "ArticleType"
         Caption         =   "ArticleType"
      EndProperty
      BeginProperty Field74 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "VariousFlag"
         Caption         =   "VariousFlag"
      EndProperty
      BeginProperty Field75 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "MiljoavgiftType"
         Caption         =   "MiljoavgiftType"
      EndProperty
      BeginProperty Field76 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "MiljoAvgVarenr"
         Caption         =   "MiljoAvgVarenr"
      EndProperty
      BeginProperty Field77 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "MiljoAvgSats"
         Caption         =   "MiljoAvgSats"
      EndProperty
      BeginProperty Field78 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "PrisInclMiljo"
         Caption         =   "PrisInclMiljo"
      EndProperty
      BeginProperty Field79 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "MiljoAvgGruppe"
         Caption         =   "MiljoAvgGruppe"
      EndProperty
      BeginProperty Field80 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "isBruttoNotAvanse"
         Caption         =   "isBruttoNotAvanse"
      EndProperty
      BeginProperty Field81 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "pakrevdefelterid"
         Caption         =   "pakrevdefelterid"
      EndProperty
      BeginProperty Field82 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "kontonr_person"
         Caption         =   "kontonr_person"
      EndProperty
      BeginProperty Field83 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "kontonr_no_mva"
         Caption         =   "kontonr_no_mva"
      EndProperty
      BeginProperty Field84 
         Precision       =   0
         Size            =   20
         Scale           =   0
         Type            =   200
         Name            =   "varegruppe2"
         Caption         =   "varegruppe2"
      EndProperty
      BeginProperty Field85 
         Precision       =   0
         Size            =   20
         Scale           =   0
         Type            =   200
         Name            =   "ssbvaregruppe"
         Caption         =   "ssbvaregruppe"
      EndProperty
      BeginProperty Field86 
         Precision       =   0
         Size            =   2147483647
         Scale           =   0
         Type            =   201
         Name            =   "kortbeskrivelse"
         Caption         =   "kortbeskrivelse"
      EndProperty
      BeginProperty Field87 
         Precision       =   5
         Size            =   2
         Scale           =   0
         Type            =   2
         Name            =   "allowcomments"
         Caption         =   "allowcomments"
      EndProperty
      BeginProperty Field88 
         Precision       =   0
         Size            =   3
         Scale           =   0
         Type            =   200
         Name            =   "landkode"
         Caption         =   "landkode"
      EndProperty
      BeginProperty Field89 
         Precision       =   5
         Size            =   2
         Scale           =   0
         Type            =   2
         Name            =   "individual_varians_price"
         Caption         =   "individual_varians_price"
      EndProperty
      BeginProperty Field90 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "NOBB_modulnr"
         Caption         =   "NOBB_modulnr"
      EndProperty
      BeginProperty Field91 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "NELFO_blokknr"
         Caption         =   "NELFO_blokknr"
      EndProperty
      BeginProperty Field92 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "Fabrikat"
         Caption         =   "Fabrikat"
      EndProperty
      BeginProperty Field93 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "Type"
         Caption         =   "Type"
      EndProperty
      BeginProperty Field94 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Nobb_artikkelnr"
         Caption         =   "Nobb_artikkelnr"
      EndProperty
      NumGroups       =   0
      ParamCount      =   0
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset12 
      CommandName     =   "fyllemaskin"
      CommDispId      =   1074
      RsDispId        =   1075
      CommandText     =   "dbo.fyllemaskin"
      ActiveConnectionName=   "betterm"
      CommandType     =   2
      dbObjectType    =   1
      Locktype        =   3
      IsRSReturning   =   -1  'True
      NumFields       =   5
      BeginProperty Field1 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   20
         Name            =   "Tankid"
         Caption         =   "Tankid"
      EndProperty
      BeginProperty Field2 
         Precision       =   23
         Size            =   16
         Scale           =   3
         Type            =   135
         Name            =   "Datostart"
         Caption         =   "Datostart"
      EndProperty
      BeginProperty Field3 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "Liter"
         Caption         =   "Liter"
      EndProperty
      BeginProperty Field4 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Status"
         Caption         =   "Status"
      EndProperty
      BeginProperty Field5 
         Precision       =   23
         Size            =   16
         Scale           =   3
         Type            =   135
         Name            =   "Datostopp"
         Caption         =   "Datostopp"
      EndProperty
      NumGroups       =   0
      ParamCount      =   0
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset13 
      CommandName     =   "dispensere"
      CommDispId      =   1076
      RsDispId        =   1078
      CommandText     =   "dbo.Dispensere"
      ActiveConnectionName=   "betterm"
      CommandType     =   2
      dbObjectType    =   1
      Locktype        =   3
      IsRSReturning   =   -1  'True
      NumFields       =   9
      BeginProperty Field1 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   20
         Name            =   "DispID"
         Caption         =   "DispID"
      EndProperty
      BeginProperty Field2 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Dispensernr"
         Caption         =   "Dispensernr"
      EndProperty
      BeginProperty Field3 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "Produkt"
         Caption         =   "Produkt"
      EndProperty
      BeginProperty Field4 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Pris"
         Caption         =   "Pris"
      EndProperty
      BeginProperty Field5 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Type"
         Caption         =   "Type"
      EndProperty
      BeginProperty Field6 
         Precision       =   0
         Size            =   2
         Scale           =   0
         Type            =   11
         Name            =   "Aktiv"
         Caption         =   "Aktiv"
      EndProperty
      BeginProperty Field7 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Rs485adrcontainer"
         Caption         =   "Rs485adrcontainer"
      EndProperty
      BeginProperty Field8 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Stationcredit"
         Caption         =   "Stationcredit"
      EndProperty
      BeginProperty Field9 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Pos"
         Caption         =   "Pos"
      EndProperty
      NumGroups       =   0
      ParamCount      =   0
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset14 
      CommandName     =   "logs"
      CommDispId      =   1079
      RsDispId        =   1108
      CommandText     =   "SELECT * FROM logs ORDER BY dato DESC"
      ActiveConnectionName=   "betterm"
      CommandType     =   1
      Locktype        =   3
      IsRSReturning   =   -1  'True
      NumFields       =   7
      BeginProperty Field1 
         Precision       =   18
         Size            =   19
         Scale           =   0
         Type            =   131
         Name            =   "id"
         Caption         =   "id"
      EndProperty
      BeginProperty Field2 
         Precision       =   23
         Size            =   16
         Scale           =   3
         Type            =   135
         Name            =   "dato"
         Caption         =   "dato"
      EndProperty
      BeginProperty Field3 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "event"
         Caption         =   "event"
      EndProperty
      BeginProperty Field4 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   20
         Name            =   "ref_tableid"
         Caption         =   "ref_tableid"
      EndProperty
      BeginProperty Field5 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "ref_tablename"
         Caption         =   "ref_tablename"
      EndProperty
      BeginProperty Field6 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "rawdata"
         Caption         =   "rawdata"
      EndProperty
      BeginProperty Field7 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "Type"
         Caption         =   "Type"
      EndProperty
      NumGroups       =   0
      ParamCount      =   0
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset15 
      CommandName     =   "kvitteringer"
      CommDispId      =   1085
      RsDispId        =   1087
      CommandText     =   "dbo.rapporter_bankterminal"
      ActiveConnectionName=   "betterm"
      CommandType     =   2
      dbObjectType    =   1
      Locktype        =   3
      IsRSReturning   =   -1  'True
      NumFields       =   7
      BeginProperty Field1 
         Precision       =   18
         Size            =   19
         Scale           =   0
         Type            =   131
         Name            =   "reportid"
         Caption         =   "reportid"
      EndProperty
      BeginProperty Field2 
         Precision       =   0
         Size            =   2147483647
         Scale           =   0
         Type            =   201
         Name            =   "reporttext"
         Caption         =   "reporttext"
      EndProperty
      BeginProperty Field3 
         Precision       =   0
         Size            =   30
         Scale           =   0
         Type            =   200
         Name            =   "type"
         Caption         =   "type"
      EndProperty
      BeginProperty Field4 
         Precision       =   23
         Size            =   16
         Scale           =   3
         Type            =   135
         Name            =   "dato"
         Caption         =   "dato"
      EndProperty
      BeginProperty Field5 
         Precision       =   0
         Size            =   255
         Scale           =   0
         Type            =   200
         Name            =   "Comment"
         Caption         =   "Comment"
      EndProperty
      BeginProperty Field6 
         Precision       =   23
         Size            =   16
         Scale           =   3
         Type            =   135
         Name            =   "Commentdate"
         Caption         =   "Commentdate"
      EndProperty
      BeginProperty Field7 
         Precision       =   23
         Size            =   16
         Scale           =   3
         Type            =   135
         Name            =   "CommentLastedit"
         Caption         =   "CommentLastedit"
      EndProperty
      NumGroups       =   0
      ParamCount      =   0
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset16 
      CommandName     =   "tankinger"
      CommDispId      =   1088
      RsDispId        =   1090
      CommandText     =   "dbo.Tankinger"
      ActiveConnectionName=   "betterm"
      CommandType     =   2
      dbObjectType    =   1
      Locktype        =   3
      IsRSReturning   =   -1  'True
      NumFields       =   10
      BeginProperty Field1 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   20
         Name            =   "Tankid"
         Caption         =   "Tankid"
      EndProperty
      BeginProperty Field2 
         Precision       =   23
         Size            =   16
         Scale           =   3
         Type            =   135
         Name            =   "Datostart"
         Caption         =   "Datostart"
      EndProperty
      BeginProperty Field3 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "Liter"
         Caption         =   "Liter"
      EndProperty
      BeginProperty Field4 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "Pris"
         Caption         =   "Pris"
      EndProperty
      BeginProperty Field5 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "Presalg"
         Caption         =   "Presalg"
      EndProperty
      BeginProperty Field6 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "sum"
         Caption         =   "sum"
      EndProperty
      BeginProperty Field7 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "tilbakesum"
         Caption         =   "tilbakesum"
      EndProperty
      BeginProperty Field8 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "betalingstype"
         Caption         =   "betalingstype"
      EndProperty
      BeginProperty Field9 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Status"
         Caption         =   "Status"
      EndProperty
      BeginProperty Field10 
         Precision       =   23
         Size            =   16
         Scale           =   3
         Type            =   135
         Name            =   "Datostopp"
         Caption         =   "Datostopp"
      EndProperty
      NumGroups       =   0
      ParamCount      =   0
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset17 
      CommandName     =   "tasks"
      CommDispId      =   1091
      RsDispId        =   1093
      CommandText     =   "dbo.Oppgaver"
      ActiveConnectionName=   "betterm"
      CommandType     =   2
      dbObjectType    =   1
      Locktype        =   3
      IsRSReturning   =   -1  'True
      NumFields       =   5
      BeginProperty Field1 
         Precision       =   0
         Size            =   2
         Scale           =   0
         Type            =   11
         Name            =   "zrapport"
         Caption         =   "zrapport"
      EndProperty
      BeginProperty Field2 
         Precision       =   0
         Size            =   2
         Scale           =   0
         Type            =   11
         Name            =   "xrapport"
         Caption         =   "xrapport"
      EndProperty
      BeginProperty Field3 
         Precision       =   0
         Size            =   2
         Scale           =   0
         Type            =   11
         Name            =   "avstemming"
         Caption         =   "avstemming"
      EndProperty
      BeginProperty Field4 
         Precision       =   0
         Size            =   2
         Scale           =   0
         Type            =   11
         Name            =   "lowpaper"
         Caption         =   "lowpaper"
      EndProperty
      BeginProperty Field5 
         Precision       =   0
         Size            =   2
         Scale           =   0
         Type            =   11
         Name            =   "Unilink"
         Caption         =   "Unilink"
      EndProperty
      NumGroups       =   0
      ParamCount      =   0
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset18 
      CommandName     =   "kortholder"
      CommDispId      =   1094
      RsDispId        =   1096
      CommandText     =   "dbo.kortholder"
      ActiveConnectionName=   "betterm"
      CommandType     =   2
      dbObjectType    =   1
      Locktype        =   3
      IsRSReturning   =   -1  'True
      NumFields       =   7
      BeginProperty Field1 
         Precision       =   18
         Size            =   19
         Scale           =   0
         Type            =   131
         Name            =   "kortholderid"
         Caption         =   "kortholderid"
      EndProperty
      BeginProperty Field2 
         Precision       =   18
         Size            =   19
         Scale           =   0
         Type            =   131
         Name            =   "kundeid"
         Caption         =   "kundeid"
      EndProperty
      BeginProperty Field3 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "Navn"
         Caption         =   "Navn"
      EndProperty
      BeginProperty Field4 
         Precision       =   0
         Size            =   20
         Scale           =   0
         Type            =   200
         Name            =   "Telefon"
         Caption         =   "Telefon"
      EndProperty
      BeginProperty Field5 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "Merknad"
         Caption         =   "Merknad"
      EndProperty
      BeginProperty Field6 
         Precision       =   0
         Size            =   14
         Scale           =   0
         Type            =   200
         Name            =   "kortnummer"
         Caption         =   "kortnummer"
      EndProperty
      BeginProperty Field7 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Aktiv"
         Caption         =   "Aktiv"
      EndProperty
      NumGroups       =   0
      ParamCount      =   0
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset19 
      CommandName     =   "levering"
      CommDispId      =   1097
      RsDispId        =   1103
      CommandText     =   "SELECT * FROM levering WHERE id= (SELECT (MAX(id)) FROM levering)"
      ActiveConnectionName=   "butikkdata"
      CommandType     =   1
      Locktype        =   3
      IsRSReturning   =   -1  'True
      NumFields       =   6
      BeginProperty Field1 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "ID"
         Caption         =   "ID"
      EndProperty
      BeginProperty Field2 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "OrderNo"
         Caption         =   "OrderNo"
      EndProperty
      BeginProperty Field3 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "InvoiceNo"
         Caption         =   "InvoiceNo"
      EndProperty
      BeginProperty Field4 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "ExtOrderNo"
         Caption         =   "ExtOrderNo"
      EndProperty
      BeginProperty Field5 
         Precision       =   16
         Size            =   16
         Scale           =   0
         Type            =   135
         Name            =   "DTime"
         Caption         =   "DTime"
      EndProperty
      BeginProperty Field6 
         Precision       =   0
         Size            =   30
         Scale           =   0
         Type            =   200
         Name            =   "UserName"
         Caption         =   "UserName"
      EndProperty
      NumGroups       =   0
      ParamCount      =   0
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset20 
      CommandName     =   "Command1"
      CommDispId      =   1104
      RsDispId        =   1107
      CommandText     =   "SELECT TOP 100 * FROM leveringslinje "
      ActiveConnectionName=   "butikkdata"
      CommandType     =   1
      IsRSReturning   =   -1  'True
      NumFields       =   13
      BeginProperty Field1 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "ID"
         Caption         =   "ID"
      EndProperty
      BeginProperty Field2 
         Precision       =   16
         Size            =   16
         Scale           =   0
         Type            =   135
         Name            =   "Dato"
         Caption         =   "Dato"
      EndProperty
      BeginProperty Field3 
         Precision       =   0
         Size            =   6
         Scale           =   0
         Type            =   200
         Name            =   "Sign"
         Caption         =   "Sign"
      EndProperty
      BeginProperty Field4 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "Antall"
         Caption         =   "Antall"
      EndProperty
      BeginProperty Field5 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "PickId"
         Caption         =   "PickId"
      EndProperty
      BeginProperty Field6 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Leveringsid"
         Caption         =   "Leveringsid"
      EndProperty
      BeginProperty Field7 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "VarelagerId"
         Caption         =   "VarelagerId"
      EndProperty
      BeginProperty Field8 
         Precision       =   0
         Size            =   20
         Scale           =   0
         Type            =   200
         Name            =   "varenr"
         Caption         =   "varenr"
      EndProperty
      BeginProperty Field9 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Total"
         Caption         =   "Total"
      EndProperty
      BeginProperty Field10 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Mottaksid"
         Caption         =   "Mottaksid"
      EndProperty
      BeginProperty Field11 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "KommentarID"
         Caption         =   "KommentarID"
      EndProperty
      BeginProperty Field12 
         Precision       =   16
         Size            =   16
         Scale           =   0
         Type            =   135
         Name            =   "transdato"
         Caption         =   "transdato"
      EndProperty
      BeginProperty Field13 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   6
         Name            =   "innpris"
         Caption         =   "innpris"
      EndProperty
      NumGroups       =   0
      ParamCount      =   0
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset21 
      CommandName     =   "tankinger_pr_dag"
      CommDispId      =   1109
      RsDispId        =   1113
      CommandText     =   "dbo.tankinger_pr_dag"
      ActiveConnectionName=   "betterm"
      CallSyntax      =   "{? = CALL dbo.tankinger_pr_dag( ?, ?, ?) }"
      IsRSReturning   =   -1  'True
      NumFields       =   10
      BeginProperty Field1 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   20
         Name            =   "Tankid"
         Caption         =   "Tankid"
      EndProperty
      BeginProperty Field2 
         Precision       =   23
         Size            =   16
         Scale           =   3
         Type            =   135
         Name            =   "Datostart"
         Caption         =   "Datostart"
      EndProperty
      BeginProperty Field3 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "Liter"
         Caption         =   "Liter"
      EndProperty
      BeginProperty Field4 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "Pris"
         Caption         =   "Pris"
      EndProperty
      BeginProperty Field5 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "Presalg"
         Caption         =   "Presalg"
      EndProperty
      BeginProperty Field6 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "sum"
         Caption         =   "sum"
      EndProperty
      BeginProperty Field7 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "tilbakesum"
         Caption         =   "tilbakesum"
      EndProperty
      BeginProperty Field8 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "betalingstype"
         Caption         =   "betalingstype"
      EndProperty
      BeginProperty Field9 
         Precision       =   10
         Size            =   4
         Scale           =   0
         Type            =   3
         Name            =   "Status"
         Caption         =   "Status"
      EndProperty
      BeginProperty Field10 
         Precision       =   23
         Size            =   16
         Scale           =   3
         Type            =   135
         Name            =   "Datostopp"
         Caption         =   "Datostopp"
      EndProperty
      NumGroups       =   0
      ParamCount      =   4
      BeginProperty P1 
         RealName        =   "@RETURN_VALUE"
         Direction       =   4
         Precision       =   10
         Scale           =   0
         Size            =   0
         DataType        =   3
         HostType        =   3
         Required        =   0   'False
      EndProperty
      BeginProperty P2 
         RealName        =   "@dag"
         Direction       =   1
         Precision       =   10
         Scale           =   0
         Size            =   0
         DataType        =   3
         HostType        =   3
         Required        =   -1  'True
      EndProperty
      BeginProperty P3 
         RealName        =   "@mnd"
         Direction       =   1
         Precision       =   10
         Scale           =   0
         Size            =   0
         DataType        =   3
         HostType        =   3
         Required        =   -1  'True
      EndProperty
      BeginProperty P4 
         RealName        =   "@aar"
         Direction       =   1
         Precision       =   10
         Scale           =   0
         Size            =   0
         DataType        =   3
         HostType        =   3
         Required        =   -1  'True
      EndProperty
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset22 
      CommandName     =   "salgstall"
      CommDispId      =   1114
      RsDispId        =   1116
      CommandText     =   "dbo.Salgstall"
      ActiveConnectionName=   "betterm"
      CommandType     =   2
      dbObjectType    =   1
      Locktype        =   3
      IsRSReturning   =   -1  'True
      NumFields       =   10
      BeginProperty Field1 
         Precision       =   18
         Size            =   19
         Scale           =   0
         Type            =   131
         Name            =   "SalesID"
         Caption         =   "SalesID"
      EndProperty
      BeginProperty Field2 
         Precision       =   16
         Size            =   16
         Scale           =   0
         Type            =   135
         Name            =   "Dato"
         Caption         =   "Dato"
      EndProperty
      BeginProperty Field3 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "Zrapportsum"
         Caption         =   "Zrapportsum"
      EndProperty
      BeginProperty Field4 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "Avstemmingsum"
         Caption         =   "Avstemmingsum"
      EndProperty
      BeginProperty Field5 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "Tekniskretursum"
         Caption         =   "Tekniskretursum"
      EndProperty
      BeginProperty Field6 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "Totalsumkort"
         Caption         =   "Totalsumkort"
      EndProperty
      BeginProperty Field7 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "Stasjonskredittsum"
         Caption         =   "Stasjonskredittsum"
      EndProperty
      BeginProperty Field8 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "Manuellsum"
         Caption         =   "Manuellsum"
      EndProperty
      BeginProperty Field9 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "MVA"
         Caption         =   "MVA"
      EndProperty
      BeginProperty Field10 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "Totalsum"
         Caption         =   "Totalsum"
      EndProperty
      NumGroups       =   0
      ParamCount      =   0
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset23 
      CommandName     =   "stdagensomsetning"
      CommDispId      =   1117
      RsDispId        =   1119
      CommandText     =   "dbo.oms_stasjonskreditt"
      ActiveConnectionName=   "betterm"
      CallSyntax      =   "{? = CALL dbo.oms_stasjonskreditt }"
      IsRSReturning   =   -1  'True
      NumFields       =   1
      BeginProperty Field1 
         Precision       =   38
         Size            =   19
         Scale           =   2
         Type            =   131
         Name            =   "dagsomsetnimg"
         Caption         =   "dagsomsetnimg"
      EndProperty
      NumGroups       =   0
      ParamCount      =   1
      BeginProperty P1 
         RealName        =   "@RETURN_VALUE"
         UserName        =   "RETURN_VALUE"
         Direction       =   4
         Precision       =   10
         Scale           =   0
         Size            =   0
         DataType        =   3
         HostType        =   3
         Required        =   0   'False
      EndProperty
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset24 
      CommandName     =   "dbo_oms_manuell"
      CommDispId      =   1120
      RsDispId        =   1123
      CommandText     =   "dbo.oms_manuell"
      ActiveConnectionName=   "betterm"
      CallSyntax      =   "{? = CALL dbo.oms_manuell }"
      Expanded        =   -1  'True
      IsRSReturning   =   -1  'True
      NumFields       =   1
      BeginProperty Field1 
         Precision       =   15
         Size            =   8
         Scale           =   0
         Type            =   5
         Name            =   "dagsomsetning"
         Caption         =   "dagsomsetning"
      EndProperty
      NumGroups       =   0
      ParamCount      =   1
      BeginProperty P1 
         RealName        =   "@RETURN_VALUE"
         Direction       =   4
         Precision       =   10
         Scale           =   0
         Size            =   0
         DataType        =   3
         HostType        =   3
         Required        =   0   'False
      EndProperty
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
   BeginProperty Recordset25 
      CommandName     =   "stasjonskort"
      CommDispId      =   1124
      RsDispId        =   1126
      CommandText     =   "dbo.stasjonskort"
      ActiveConnectionName=   "betterm"
      CommandType     =   2
      dbObjectType    =   1
      Locktype        =   3
      IsRSReturning   =   -1  'True
      NumFields       =   9
      BeginProperty Field1 
         Precision       =   18
         Size            =   19
         Scale           =   0
         Type            =   131
         Name            =   "Stasjonskortid"
         Caption         =   "Stasjonskortid"
      EndProperty
      BeginProperty Field2 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   20
         Name            =   "Kundeid"
         Caption         =   "Kundeid"
      EndProperty
      BeginProperty Field3 
         Precision       =   19
         Size            =   8
         Scale           =   0
         Type            =   20
         Name            =   "kortholderid"
         Caption         =   "kortholderid"
      EndProperty
      BeginProperty Field4 
         Precision       =   0
         Size            =   17
         Scale           =   0
         Type            =   200
         Name            =   "kortnummer"
         Caption         =   "kortnummer"
      EndProperty
      BeginProperty Field5 
         Precision       =   0
         Size            =   50
         Scale           =   0
         Type            =   200
         Name            =   "kortholdernavn"
         Caption         =   "kortholdernavn"
      EndProperty
      BeginProperty Field6 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "kredittlimit"
         Caption         =   "kredittlimit"
      EndProperty
      BeginProperty Field7 
         Precision       =   7
         Size            =   4
         Scale           =   0
         Type            =   4
         Name            =   "Rabatt"
         Caption         =   "Rabatt"
      EndProperty
      BeginProperty Field8 
         Precision       =   0
         Size            =   2
         Scale           =   0
         Type            =   11
         Name            =   "Aktiv"
         Caption         =   "Aktiv"
      EndProperty
      BeginProperty Field9 
         Precision       =   23
         Size            =   16
         Scale           =   3
         Type            =   135
         Name            =   "Lastupdated"
         Caption         =   "Lastupdated"
      EndProperty
      NumGroups       =   0
      ParamCount      =   0
      RelationCount   =   0
      AggregateCount  =   0
   EndProperty
End
Attribute VB_Name = "lpgnorge"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = True
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False
