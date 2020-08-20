package jmvanel

import java.nio.file._
import org.gbif.dwc._
import org.gbif.dwc.record._
import org.gbif.dwc.terms._
import java.io._
import scala.collection.JavaConverters._

object Test1 extends App {
  val myArchiveFile = Paths.get(args(0)); // "myArchive.zip");
  val extractToFolder = Paths.get("/tmp/myarchive");
  val dwcArchive = DwcFiles.fromCompressed(myArchiveFile, extractToFolder);

  // Loop over core core records and display id, basis of record and scientific name
  for (rec <- dwcArchive.getCore().asScala) {
    println(String.format("%s: %s (%s)", rec.id(),
        rec.value(DwcTerm.basisOfRecord),
        rec.value(DwcTerm.scientificName)));
  }
}