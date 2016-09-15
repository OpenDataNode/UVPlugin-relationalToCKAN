### Popis

Nahrá tabuľky internej relačnej databázy na úložisko CKAN

### Konfiguračné parametre

| Meno | Popis |
|:----|:----|
|**Názov zdroja CKAN** | Názov zdroja CKAN, ktorý má byť vytvorený, má prednosť pred vstupom z e-distributionMetadata a ak nie je zadaný, použije sa virtuálna cesta alebo symbolické meno ako meno zdroja |
|**Prepísať existujúce zdroje**| Ak zdroj už existuje, bude prepísaný |

### Vstupy a výstupy ###

|Meno |Typ | Dátová hrana | Popis | Povinné |
|:--------|:------:|:------:|:-------------|:---------------------:|
|tablesInput       |vstup| RelationalDataUnit | Tabuľky, ktoré sa majú uložiť na úložisko CKAN |áno|
|distributionInput |vstup| RDFDataUnit | Distribučné metadáta vytvorené z e-distributionMetadata | |
