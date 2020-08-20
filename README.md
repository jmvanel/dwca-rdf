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

*Links*
- https://dwc.tdwg.org/rdf/
- http://baskauf.blogspot.com/2019/
