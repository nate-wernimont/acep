package edu.iastate.cs.design.asymptotic.tests.benchmarks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.iastate.cs.design.asymptotic.datastructures.CallGraphDFS;
import edu.iastate.cs.design.asymptotic.datastructures.XMLParser;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootResolver;
import soot.options.Options;

public class Benchmark {

	private String JRE = "";
	protected String _class;
	private String _method;
	private SootMethod _sootMethod;
	private List<String> _processdir = new ArrayList<String>();
	private List<String> _includes = new ArrayList<String>();
	private String _classpath;
	protected CallGraphDFS _cg;
	private final String FS = File.separator;

	// Libraries to use
	private final String[] _jre_jars = { "rt.jar", "jce.jar" };

	protected List<String> _lib_jars = new ArrayList<String>();
	protected String _LIB;

	protected String _config;

	public Benchmark(String config) {
		_config = config;
		// Parse config file
		XMLParser parser = new XMLParser(_config);
		Document document = parser.parseXmlFile();
		config(document);
	}

	/**
	 * Reads all of the config documents properties
	 * @param xml A xml document
	 */
	void config(Document xml) {
		// get the root element
		Element benchmark = xml.getDocumentElement();
		// Read the main class
		_class = getTagValue("class", benchmark);
		Node method = benchmark.getElementsByTagName("method").item(0);
		_method = getTagValue("method", benchmark);
		Node nNode = benchmark.getElementsByTagName("processdir").item(0);
		if (nNode.getNodeType() == Node.ELEMENT_NODE) {
//			Element eElement = (Element) nNode;
//			NodeList dirs = benchmark.getElementsByTagName("dir");
//			for (int i = 0; i < dirs.getLength(); i++) {
//				String child = dirs.item(i).getChildNodes().item(0)
//						.getNodeValue();
//				_processdir.add(child);
//			}
			_processdir.addAll(getAllTagValues("dir", benchmark));
		}

		nNode = benchmark.getElementsByTagName("includes").item(0);
		if (nNode.getNodeType() == Node.ELEMENT_NODE) {
//			Element eElement = (Element) nNode;
//			NodeList incs = benchmark.getElementsByTagName("inc");
//			for (int i = 0; i < incs.getLength(); i++) {
//				String child = incs.item(i).getChildNodes().item(0)
//						.getNodeValue();
//				_includes.add(child);
//			}
			_includes.addAll(getAllTagValues("inc", benchmark));
		}
		
		_LIB = getTagValue("lib", benchmark);
		JRE = getTagValue("jre", benchmark);
		
		nNode = benchmark.getElementsByTagName("jars").item(0);
		if (nNode != null) {
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
//				Element eElement = (Element) nNode;
//				NodeList jars = benchmark.getElementsByTagName("jar");
//				for (int i = 0; i < jars.getLength(); i++) {
//					String child = jars.item(i).getChildNodes().item(0)
//							.getNodeValue();
//					_lib_jars.add(child);
//				}
				_lib_jars.addAll(getAllTagValues("jar", benchmark));
			}
		}
	}
	
	/**
	 * @author nwernimont
	 * @param eElement The document being searched
	 * @param sTag The tag to search for
	 * @returns a list of the the values of the given tags
	 *///There can be more than one value for that tag
	private List<String> getAllTagValues(String sTag, Element eElement){
		NodeList nList = eElement.getElementsByTagName(sTag);
		List<String> result = new ArrayList<String>();
		for(int i = 0; i < nList.getLength(); i++){
			String child = nList.item(i).getChildNodes().item(0).getNodeValue();
			result.add(child);
		}
		return result;
	}

	//This is if there is expected to be only one value for that tag
	private static String getTagValue(String sTag, Element eElement) {
		if (eElement.getElementsByTagName(sTag).item(0) == null)
			return "";
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0)
				.getChildNodes();
		Node nValue = nlList.item(0);
		return nValue.getNodeValue();
	}
	
	public static boolean isWindows() {

		String os = System.getProperty("os.name").toLowerCase();
		// windows
		return (os.indexOf("win") >= 0);

	}

	/**
	 * 
	 * @return
	 */
	void construct_class_path() {
		String cp = "";
		String pathSeparator = "";
		
		if (isWindows())	pathSeparator = ";";
		else	pathSeparator = ":";
		
		for (String jar : _jre_jars) {
			cp = cp + JRE + FS + jar + pathSeparator;
		}
		for (String jar : _lib_jars) {
			cp = cp + _LIB + FS + jar + pathSeparator;
		}
		for (String processdir : _processdir) {
			cp = cp + processdir + pathSeparator;
		}
		cp = cp + _LIB;
		cp += ":bin";//MINE TODO:
		_classpath = cp;
	}

	/**
	 * Prepare soot for spark points to analysis
	 */
	protected void prepareSoot() {
		Options.v().set_keep_line_number(true);
		Options.v().set_whole_program(true);
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_allow_phantom_refs(true);
		// Processdir should specify the class file locations which
		// have to be considered for the analysis
		Options.v().set_process_dir(_processdir);
		// Soot classpath should have path to JRE (rt.jar, jce.jar),
		// specific benchmark related jar file locations, and
		// processdir paths
		construct_class_path();
		System.out.println(_classpath);
		Options.v().set_soot_classpath(_classpath);
		// Setting the phase options accelerates the initial cg construction
		Options.v().setPhaseOption("cg", "verbose:true");
		Options.v().setPhaseOption("cg", "safe-newinstance");
		Options.v().setPhaseOption("cg", "safe-forname");
		// Only the classes which are part of includes are analyzed
		Options.v().set_include(_includes);
		// Add excludes,
		List<String> _excludes = new ArrayList<String>();
		_excludes.add("java");//TODO: excludes
		Options.v().set_exclude(_excludes);
		// Mention the starting point and the main method
		SootClass c = Scene.v().loadClassAndSupport(_class);
		_sootMethod = c.getMethodByName(_method);
		
		// TODO: temporary fix for ppgp
		SootResolver.v().resolveClass("javax.print.attribute.standard.MediaPrintableArea", 
				SootClass.SIGNATURES);
		
		c.setApplicationClass();
		Scene.v().setMainClass(c);
		// Important step, without which you will not be able to run spark
		// analysis
		//Scene.v().addBasicClass("java.io.ObjectStreamClass$MemberSignature", SootClass.HIERARCHY);
		//Scene.v().addBasicClass("java.util.concurrent.ConcurrentHashMap$Segment", SootClass.HIERARCHY);
		Scene.v().loadNecessaryClasses();
		// Spark analysis, true for brief analysis and false for full
		//CallGraphBuilder.setSparkPointsToAnalysis(true); //   <--- UNCOMMENT
		// Create callgraph
		//_cg = new CallGraphDFS(_sootMethod);//   <--- UNCOMMENT
	}

	public CallGraphDFS getCallGraph() {
		return _cg;
	}

	public SootMethod main() {
		return _sootMethod;
	}

}
