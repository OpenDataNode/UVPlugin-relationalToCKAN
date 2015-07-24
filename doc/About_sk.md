### Popis

Tanhá tabuľky internej relačnej databázy na úložisko CKAN

### Konfiguračné parametre

| Meno | Popis |
|:----|:----|
|**Názov zdroja CKAN:** | Názov zdroja CKAN, ktorý á vyť vytvorený, má prednosť pred vstupom z e-distributionMetadata a ak nie je zadaný, použije sa virtuálna cesta alebo symbolické meno ako meno zdroja |
|**Prepísať existujúce zdroje**| Ak zdroj už existuje, bude prepísaný |

### Vstupy a výstupy ###

|Meno |Typ | Dátová hrana | Popis | Povinné |
|:--------|:------:|:------:|:-------------|:---------------------:|
|tablesInput       |i| RelationalDataUnit | Tabuľky, ktoré sa majú uložiť na úložisko CKAN |x|
|distributionInput |i| RDFDataUnit | Distribučné metadáta vytvorené z e-distributionMetadata ||