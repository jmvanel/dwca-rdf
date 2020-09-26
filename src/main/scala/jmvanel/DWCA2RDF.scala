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

  def makeTaxonURI( reference: String, id: String) = reference2taxonTemplate(reference).replace("$1", id)
  def rowURI(implicit rec: Record) = {
    import rec._
    class2instancePrefix(rowType().qualifiedName()) + id
  }

  //// start of main() ////
  val myArchiveFile = Paths.get(args(0));
  val extractToFolder = Paths.get("/tmp/myarchive")
  val dwcArchive = DwcFiles.fromCompressed(myArchiveFile, extractToFolder)

  val graph = GraphFactory.createDefaultGraph()
  // Loop over core records and display id, basis of record and scientific name
  var i = 0
  for (rec <- dwcArchive.getCore().asScala) {
    if( i == 0 ) println( "# " + rec.terms().asScala.mkString(", ") )
    i = i+1
	  // printRecord(rec)
	  record2RDF(rec, graph)
  }
  addAgentTriples(graph)
  println(s"# graph size ${graph.size()}")
  println(s"# person Map size ${personMap.size}")
  RDFDataMgr.write(System.out, graph, Lang.NTRIPLES)
  //// end of main() ////
 
  def record2RDF(implicit rec: Record, graph: Graph): Graph = {
    import rec._
    implicit val rowURIrdf = NodeFactory.createURI(rowURI)
    addPropertyObject(
      a,
      NodeFactory.createURI(
        basisOfRecord2Class(
          value(DwcTerm.basisOfRecord))))
    addPropertyObject(
      a,
      NodeFactory.createURI(rowType().qualifiedName()))
    addPropertyStringObject(
      (dwciri + "toTaxon"),
      // TODO : the taxonID may be absent in given taxonomic registry
      NodeFactory.createURI(
        makeTaxonURI(value(DwcTerm.nameAccordingTo), value(DwcTerm.taxonID))))
    addPropertyStringObject(
      DwcTerm.decimalLongitude.qualifiedName,
      NodeFactory.createLiteral(
        value(DwcTerm.decimalLongitude)))
    addPropertyStringObject(
      DwcTerm.decimalLatitude.qualifiedName,
      NodeFactory.createLiteral(
        value(DwcTerm.decimalLatitude)))
    addPropertyStringObject(
      DwcTerm.scientificName.qualifiedName(),
      NodeFactory.createLiteral(
        value(DwcTerm.scientificName)))
    addPropertyObject(
      // TODO which RDF property here ? cf https://www.gbif.org/en/article/5i3CQEZ6DuWiycgMaaakCo/gbif-infrastructure-data-processing
      NodeFactory.createURI("urn:taxonKey"),
      NodeFactory.createURI(
          "https://api.gbif.org/v1/species/" +
          value(GbifTerm.taxonKey) ) )
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


//  lazy val personMap = scala.collection.mutable.Map[String, String]()
  lazy val personMap = scala.collection.mutable.Set[String]()
  /** process dwc property "recordedBy"
   *  TODO (how?) : recordedBy could a foaf:Organization ! . I'll suppose it's per dataset ... */
  def processRecordedBy(implicit rec: Record, graph: Graph, rowURIrdf: Node): Graph = {
    import rec._
    val personName = value(DwcTerm.recordedBy).replace(" (Non renseigné)", "")
    if (personName != null && personName != "") {
      val personURIrdf = makepersonURIrdf(personName)
      addPropertyStringObject(
        dwciri + "recordedBy", // identifiedBy",
        personURIrdf)
//      val alreadyAdded = personMap.put(personName, personURI).isDefined
      val alreadyAdded = personMap.contains(personName)
      personMap.add(personName)
      if( ! alreadyAdded ) {
        println(s"# Adding person '$personName' <$personURIrdf>")
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
    // maybe TODO speciesKey taxonKey county eventDate datasetID institutionCode
    )
  }
}
