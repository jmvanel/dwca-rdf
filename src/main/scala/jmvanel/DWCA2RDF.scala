package jmvanel

import java.nio.file._
import org.gbif.dwc._
import org.gbif.dwc.record._
import org.gbif.dwc.terms._
import java.io._
import scala.collection.JavaConverters._
import org.apache.jena.graph.NodeFactory
import org.apache.jena.query.TxnType
import org.apache.jena.graph.Node
import org.apache.jena.graph.Graph
import org.apache.jena.graph.Triple
import org.apache.jena.vocabulary.RDF
import org.apache.jena.sparql.graph.GraphFactory
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.Lang

object DWCA2RDF extends App {
  val dsw = "http://purl.org/dsw/"
  val dwc = "http://rs.tdwg.org/dwc/terms/"
  val foaf = "http://xmlns.com/foaf/0.1/"
  val foafPerson = foaf + "Person"
  val a = RDF.`type`.asNode()
  val foafPersonRDF = NodeFactory.createURI( foafPerson )
  val foafNameRDF = NodeFactory.createURI(foaf + "name")

  val class2instancePrefix = Map[String, String](
    // this gbif API is not currently RDF, but it could be made later with JSON-LD :)
    "http://rs.tdwg.org/dwc/terms/Occurrence" -> "https://api.gbif.org/v1/occurrence/",
    foafPerson -> "https://api.gbif.org/v1/person/" // ???
  )
  val reference2taxonTemplate = Map[String, String](
    "TAXREF v12" -> "http://taxref.mnhn.fr/lod/taxon/$1/12.0"
  )
  def makeTaxonURI( reference: String, id: String) = reference2taxonTemplate(reference).replace("$1", id)
  def rowURI(implicit rec: Record) = {
    import rec._
    class2instancePrefix(rowType().qualifiedName()) + id
  }

  //// start of main() ////
  val myArchiveFile = Paths.get(args(0)); // "myArchive.zip");
  val extractToFolder = Paths.get("/tmp/myarchive")
  val dwcArchive = DwcFiles.fromCompressed(myArchiveFile, extractToFolder)

  val graph = GraphFactory.createDefaultGraph()
  // Loop over core records and display id, basis of record and scientific name
  for (rec <- dwcArchive.getCore().asScala) {
	  // printRecord(rec)
	  record2RDF(rec, graph)
  }
  println(s"# graph size ${graph.size()}")
  println(s"# person Map size ${personMap.size}")
  RDFDataMgr.write(System.out, graph, Lang.NTRIPLES)
  //// end of main() ////
 
  def record2RDF(implicit rec: Record, graph: Graph): Graph = {
    import rec._
    implicit val rowURIrdf = NodeFactory.createURI(rowURI)
   addPropertyObject(
      a,
      NodeFactory.createURI(rowType().qualifiedName()))
    addPropertyStringObject(
      (dsw + "toTaxon"),
      NodeFactory.createURI(
        makeTaxonURI(value(DwcTerm.nameAccordingTo), value(DwcTerm.taxonID))))
    addPropertyStringObject(
      dwc + DwcTerm.decimalLongitude.qualifiedName,
      NodeFactory.createLiteral(
        value(DwcTerm.decimalLongitude)))
    addPropertyStringObject(
      dwc + DwcTerm.decimalLatitude.qualifiedName,
      NodeFactory.createLiteral(
        value(DwcTerm.decimalLatitude)))
    processRecordedBy
  }
  def addTriple(subject: Node, property: Node, objet: Node)(implicit graph: Graph) =  graph.add(
      Triple.create(subject, property, objet))
  def addPropertyObject(property: Node, objet: Node)(implicit rowURIrdf: Node, graph: Graph): Unit =
    addTriple(
        rowURIrdf,
        property, objet)  
  def addPropertyStringObject(property: String, objet: Node)(implicit rowURIrdf: Node, graph: Graph): Unit =
      addPropertyObject(
        NodeFactory.createURI(property), objet)

  lazy val personMap = scala.collection.mutable.Map[String, String]()
  /** process dwc property "recordedBy"
   *  TODO : it could a foaf:Organization ! . I'll suppose it's per dataset ... */
  def processRecordedBy(implicit rec: Record, graph: Graph, rowURIrdf: Node): Graph = {
    import rec._
    val personName = value(DwcTerm.recordedBy).replace(" (Non renseigné)", "")
    if (personName != null && personName != "") {
      val personURI = personName.replace(" ", "_").replace(".", "_")
      val personURIrdf = NodeFactory.createURI(
          class2instancePrefix(foafPerson) + personURI)
      addPropertyStringObject(
        dwc + "identifiedBy",
        personURIrdf)
        // println(s"# personMap $personMap put(personName=$personName, personURI=$personURI ; graph.size() ${graph.size()}")
      val alreadyAdded = personMap.put(personName, personURI).isDefined
      if( ! alreadyAdded ) {
        addTriple( personURIrdf, a, foafPersonRDF )
        addTriple( personURIrdf, foafNameRDF, NodeFactory.createLiteral(personName) )
      }
    }
    graph
  }

  def printRecord(implicit rec: Record) = {
    import rec._
    val personName = value(DwcTerm.recordedBy).replace(" - (Non renseigné)", "")
    println(
      s"""
        <${rowURI}>,
        taxonID ${value(DwcTerm.taxonID)},
        <${makeTaxonURI(value(DwcTerm.nameAccordingTo), value(DwcTerm.taxonID))}>
        occurrenceID ${value(DwcTerm.occurrenceID)},
        ${value(DwcTerm.scientificName)},
        ${DwcTerm.decimalLongitude.qualifiedName}> ${value(DwcTerm.decimalLongitude)},
        ${DwcTerm.decimalLatitude.qualifiedName}> ${value(DwcTerm.decimalLatitude)},
        personName "$personName"
        """
    // maybe TODO speciesKey taxonKey county eventDate
    )
  }
}