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

trait DWCA2RDFconstants {
  val dsw = "http://purl.org/dsw/"
  val dwc = "http://rs.tdwg.org/dwc/terms/"
  val dwciri = "http://rs.tdwg.org/dwc/iri/"

  val foaf = "http://xmlns.com/foaf/0.1/"
  val foafPerson = foaf + "Person"
  val a = RDF.`type`.asNode()
  val foafPersonRDF = NodeFactory.createURI(foafPerson)
  val foafNameRDF = NodeFactory.createURI(foaf + "name")

  val class2instancePrefix = Map[String, String](
    // this gbif API is not currently RDF, but it could be made later with JSON-LD :)
    dwc + "Occurrence" -> "https://api.gbif.org/v1/occurrence/",
    foafPerson -> "https://api.gbif.org/v1/person/" // ???
  )

  val reference2taxonTemplate = Map[String, String](
    "TAXREF v12" -> "http://taxref.mnhn.fr/lod/taxon/$1/12.0")
  def reference2taxon(reference: String) = reference2taxonTemplate.getOrElse(reference, "")

  val dwcClasses = Seq(
    "PreservedSpecimen",
    "FossilSpecimen",
    "LivingSpecimen",
    "MaterialSample",
    "Event",
    "HumanObservation",
    "MachineObservation",
    "Taxon",
    "Occurrence")
  val basisOfRecord2Class0: Map[String, String] =
    (dwcClasses.map { c => (c, dwc + c) }).toMap
  val basisOfRecord2Class = basisOfRecord2Class0 ++ Map(
    "PRESERVED_SPECIMEN" -> (dwc + "PreservedSpecimen"),
    "FOSSIL_SPECIMEN" -> (dwc + "FossilSpecimen"),
    "LIVING_SPECIMEN" -> (dwc + "LivingSpecimen"),
    "MATERIAL_SAMPLE" -> (dwc + "MaterialSample"),
    "EVENT" -> (dwc + "Event"),
    "HUMAN_OBSERVATION" -> (dwc + "HumanObservation"),
    "MACHINE_OBSERVATION" -> (dwc + ""),
    "TAXON" -> (dwc + "Taxon"),
    "OCCURRENCE" -> (dwc + "Occurrence"))
}


object DWCA2RDF extends App with DWCA2RDFconstants {

  def makeTaxonURI( reference: String, id: String) = {
    // println( s"reference: $reference, id $id")
    reference2taxon(reference).replace("$1", id)
    }
  def rowURI(implicit rec: Record) = {
    import rec._
    class2instancePrefix(rowType().qualifiedName()) + id
  }

  //// start of main() ////
  val myArchiveFile = Paths.get(args(0));
  val extractToFolder = Paths.get("/tmp/myarchive")
  val dwcArchive = DwcFiles.fromCompressed(myArchiveFile, extractToFolder)

  val graph = GraphFactory.createDefaultGraph()
  val ntFile = args(0) + ".nt"
  val fileOutputStream = new FileOutputStream(ntFile)
  val ntPrinter = new PrintWriter(fileOutputStream)

  // Loop over core records and display id, basis of record and scientific name
  var i = 0
  for (rec <- dwcArchive.getCore().asScala) {
    if( i == 0 ) printToFile( "# " + rec.terms().asScala.mkString(", ") )
    i = i+1
	  // printRecord(rec)
	  record2RDF(rec, graph)
  }
  addAgentTriples(graph)
  printToFile(s"# graph size ${graph.size()}")
  printToFile(s"# person Map size ${personMap.size}")
  ntPrinter.flush()
  RDFDataMgr.write(fileOutputStream, graph, Lang.NTRIPLES)
  println( s"N-TRIPLES file written: '$ntFile'" )
  ntPrinter.close()
  //// end of main() ////

 
  /** DWCA record to RDF
   *  TODO speciesKey county eventDate datasetID institutionCode, datasetKey, institutionCode, license
   * les noms géographiques, et la relation avec le dataset , institutionID	, collectionID
   */
  def record2RDF(implicit rec: Record, graph: Graph): Graph = {
    import rec._
    implicit val rowURIrdf = NodeFactory.createURI(rowURI)

    // rdf:type's
    addPropertyObject(
      a,
      NodeFactory.createURI(
        basisOfRecord2Class(
          valueNotNull(DwcTerm.basisOfRecord))))
    addPropertyObject(
      a,
      NodeFactory.createURI(rowType().qualifiedName()))

    // the taxonID may be absent alltogether, or absent in given taxonomic registry
    val taxonID = valueNotNull(DwcTerm.taxonID)
    if (taxonID != "") {
      addPropertyStringObject(
        (dwciri + "toTaxon"),
        NodeFactory.createURI(
          makeTaxonURI(valueNotNull(DwcTerm.nameAccordingTo), taxonID)))
    }
    import DwcTerm._
    addLiteralTriple(decimalLongitude)
    addLiteralTriple(decimalLatitude)
    addLiteralTriple(scientificName)

    // taxonKey => <https://api.gbif.org/v1/species/$taxonKey>
    addPropertyObject(
      // RDF property here: also dwci:toTaxon; cf https://www.gbif.org/en/article/5i3CQEZ6DuWiycgMaaakCo/gbif-infrastructure-data-processing
      NodeFactory.createURI(  dwciri + "toTaxon" ),
      NodeFactory.createURI(
          "https://api.gbif.org/v1/species/" +
          valueNotNull(GbifTerm.taxonKey) ) )
    processRecordedBy
  }

  /** add Literal Triple, directly from TDWG vocab' */
  def addLiteralTriple(t: Term)(implicit rowURIrdf: Node, rec: Record, graph: Graph) =
    addPropertyStringObject(
      t . qualifiedName,
      NodeFactory.createLiteral( valueNotNull(t)))

  def addTriple(subject: Node, property: Node, objet: Node)(implicit graph: Graph) =  graph.add(
      Triple.create(subject, property, objet))

  /** add Property & Object to implicit subject */
  def addPropertyObject(property: Node, objet: Node)(implicit rowURIrdf: Node, graph: Graph): Unit =
    addTriple(
        rowURIrdf,
        property, objet)

  /** add Property (from String) & Object to implicit subject */
  def addPropertyStringObject(property: String, objet: Node)(implicit rowURIrdf: Node, graph: Graph): Unit =
      addPropertyObject(
        NodeFactory.createURI(property), objet)


//  lazy val personMap = scala.collection.mutable.Map[String, String]()
  lazy val personMap = scala.collection.mutable.Set[String]()
  /** process dwc property "recordedBy"
   *  TODO (how?) : recordedBy could a foaf:Organization ! . I'll suppose it's per dataset ... */

  def processRecordedBy(implicit rec: Record, graph: Graph, rowURIrdf: Node): Graph = {
    import rec._
    val personName = valueNotNull(DwcTerm.recordedBy).replace(" (Non renseigné)", "")
    if (personName != "") {
      val personURIrdf = makepersonURIrdf(personName)
      addPropertyStringObject(
        dwciri + "recordedBy", // identifiedBy",
        personURIrdf)
      val alreadyAdded = personMap.contains(personName)
      personMap.add(personName)
      if( ! alreadyAdded ) {
        printToFile(s"# Adding person '$personName' <$personURIrdf>")
//        addTriple( personURIrdf, a, foafPersonRDF )
//        addTriple( personURIrdf, foafNameRDF, NodeFactory.createLiteral(personName) )
      }
    }
    graph
  }

  /** make person URI as RDF node */
  def makepersonURIrdf(personName: String): Node = {
    val personURI = personName.replace(" ", "_").replace(".", "_")
    NodeFactory.createURI(
      class2instancePrefix(foafPerson) + personURI)
  }

  def addAgentTriples(implicit graph: Graph) = {
//    for ((person, personURI) <- personMap) {
    for (person <- personMap) {
      val personURInode = makepersonURIrdf(person)
      addTriple(
        personURInode,
        a,
        NodeFactory.createURI(foafPerson))
      addTriple(
        personURInode,
        foafNameRDF,
        NodeFactory.createLiteral(person))
    }
  }

  def printToFile(s: String) = ntPrinter println(s)

  def printRecord(implicit rec: Record) = {
    import rec._
    val personName = valueNotNull(DwcTerm.recordedBy).replace(" - (Non renseigné)", "")
    printToFile(
      s"""
        <${rowURI}>,
        taxonID ${valueNotNull(DwcTerm.taxonID)},
        <${makeTaxonURI(valueNotNull(DwcTerm.nameAccordingTo), valueNotNull(DwcTerm.taxonID))}>
        occurrenceID ${valueNotNull(DwcTerm.occurrenceID)},
        ${valueNotNull(DwcTerm.scientificName)},
        ${DwcTerm.decimalLongitude.qualifiedName}> ${valueNotNull(DwcTerm.decimalLongitude)},
        ${DwcTerm.decimalLatitude.qualifiedName}> ${valueNotNull(DwcTerm.decimalLatitude)},
        personName "$personName"
        """
    )
  }

  def valueNotNull(t : Term)(implicit rec: Record) = {
    import rec._
    val rawVal = value(t)
    if(rawVal == null) ""
    else rawVal
  }
}
