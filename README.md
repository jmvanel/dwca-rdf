## Convert a Darwin Core Archive into Darwin Core RDF
It's work in progress.

It leverages on existing gbif Java library
https://github.com/gbif/dwca-io
and Apache Jena.

I tried in on this Darwin Core Archive, that contains plants in my département:
https://www.gbif.org/dataset/85a97a5f-751a-49ca-ad7d-238a80c0d30c

TESTED in Scala REPL with
```
runMain jmvanel.DWCA2RDF /home/jmv/data/Biologie/GBIF.org/Flore_Ain_0039246-200613084148143.zip
```

See also the JSON-LD project for GBIF.org:
https://github.com/jmvanel/rdf-convert/tree/master/gbif.org

## Current result sample

```turtle
# graph size 34489
# person Map size 28
<https://api.gbif.org/v1/occurrence/2487147795> <http://rs.tdwg.org/dwc/iri/recordedBy> <https://api.gbif.org/v1/person/A_Cl__BOLOMIER_-> .
<https://api.gbif.org/v1/occurrence/2487147795> <urn:taxonKey> <https://api.gbif.org/v1/species/5366215> .
<https://api.gbif.org/v1/occurrence/2487147795> <http://rs.tdwg.org/dwc/terms/scientificName> "Potentilla crantzii (Crantz) Fritsch" .
<https://api.gbif.org/v1/occurrence/2487147795> <http://rs.tdwg.org/dwc/terms/decimalLatitude> "46.15952" .
<https://api.gbif.org/v1/occurrence/2487147795> <http://rs.tdwg.org/dwc/terms/decimalLongitude> "5.39777" .
<https://api.gbif.org/v1/occurrence/2487147795> <http://rs.tdwg.org/dwc/iri/toTaxon> <http://taxref.mnhn.fr/lod/taxon/139270/12.0> .
<https://api.gbif.org/v1/occurrence/2487147795> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://rs.tdwg.org/dwc/terms/Occurrence> .
```

NOTES
- I suppose that soon :) the GBIF API URL's will be dereferenceable RDF URI
- taxonKey is very importnt : it is the global GBIF ID for the taxon; a permanent dereferenceable URI has to be defined
- coordinates should be xsd:float's ; same predicates as geo:lat, geo:long
- the collectors do not AFAIK have a  GBIF ID
- given "basisOfRecord": "HUMAN_OBSERVATION", the class dwc:HumanObservation should also be assigned
- "modified" key should be used
- "identifier" key should be used; is there an API for this ?
- "eventID" key should be used; is there an API for this ?
- "nameAccordingTo": "TAXREF v12" was taken for granted; should be processed; case of other taxon registries to study

Here is the GBIF API result for this (observation) occurrence
```json
wget -O - https://api.gbif.org/v1/occurrence/2487147795 |jq .
{
  "key": 2487147795,
  "datasetKey": "85a97a5f-751a-49ca-ad7d-238a80c0d30c",
  "publishingOrgKey": "1928bdf0-f5d2-11dc-8c12-b8a03c50a862",
  "installationKey": "07ea29ef-e386-4278-ae0f-095778a1b061",
  "publishingCountry": "FR",
  "protocol": "DWC_ARCHIVE",
  "lastCrawled": "2020-06-03T01:11:30.173+0000",
  "lastParsed": "2020-06-08T16:39:04.529+0000",
  "crawlId": 1,
  "extensions": {},
  "basisOfRecord": "HUMAN_OBSERVATION",
  "taxonKey": 5366215,
  "kingdomKey": 6,
  "phylumKey": 7707728,
  "classKey": 220,
  "orderKey": 691,
  "familyKey": 5015,
  "genusKey": 8079058,
  "speciesKey": 8285546,
  "acceptedTaxonKey": 8370002,
  "scientificName": "Potentilla crantzii (Crantz) Fritsch",
  "acceptedScientificName": "Potentilla crantzii subsp. crantzii",
  "kingdom": "Plantae",
  "phylum": "Tracheophyta",
  "order": "Rosales",
  "family": "Rosaceae",
  "genus": "Potentilla",
  "species": "Potentilla crantzii",
  "genericName": "Potentilla",
  "specificEpithet": "crantzii",
  "taxonRank": "SPECIES",
  "taxonomicStatus": "SYNONYM",
  "decimalLongitude": 5.39777,
  "decimalLatitude": 46.15952,
  "coordinateUncertaintyInMeters": 5000,
  "issues": [
    "GEODETIC_DATUM_ASSUMED_WGS84",
    "RECORDED_DATE_INVALID"
  ],
  "modified": "2019-02-28T00:00:00.000+0000",
  "lastInterpreted": "2020-06-08T16:39:04.529+0000",
  "license": "http://creativecommons.org/licenses/by-nc/4.0/legalcode",
  "identifiers": [],
  "media": [],
  "facts": [],
  "relations": [],
  "geodeticDatum": "WGS84",
  "class": "Magnoliopsida",
  "countryCode": "FR",
  "recordedByIDs": [],
  "identifiedByIDs": [],
  "country": "France",
  "identifier": "82e3ca10-dcbf-24f6-e053-2614a8c008ee",
  "eventID": "82e3ca10-dcbf-24f6-e053-2614a8c008ee",
  "dataGeneralizations": "Géographie transmise soumise à floutage (grille avec mailles de 10x10km) pour le grand public » en conformité avec les règles de diffusion du SINP | Geographic information generalized during aggregation (grid with 10x10km cells) for the general public, according to SINP communication rules",
  "county": "01",
  "identificationVerificationStatus": "Control could not be conclusive due to insufficient knowledge",
  "gbifID": "2487147795",
  "occurrenceID": "82e3ca10-dcbf-24f6-e053-2614a8c008ee",
  "taxonID": "139270",
  "occurrenceStatus": "Présent",
  "recordedBy": "A.Cl. BOLOMIER - (Non renseigné)",
  "locationRemarks": "Data isn’t the original geo referenced one, but attached to the nearest 10x10 km grid cell",
  "institutionCode": "Non renseigné",
  "originalNameUsage": "Potentilla verna",
  "datasetID": "4A9DDA1F-B72E-3E13-E053-2614A8C02B7C",
  "nameAccordingTo": "TAXREF v12",
  "identifiedBy": "Non renseigné (Non renseigné)"
}
```

*Links*
- https://dwc.tdwg.org/rdf/
- http://baskauf.blogspot.com/2019/
