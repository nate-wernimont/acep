package edu.iastate.cs.design.asymptotic.datastructures;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XMLParser {

	private String xmlfile;
	private Document dom;

	public XMLParser(String xmlfile) {
		this.xmlfile = xmlfile;
	}

	public Document parseXmlFile() {
		try {
			File fXmlFile = new File(xmlfile);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			return doc;
			// System.out.println(dom.getTextContent());
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (SAXException se) {
			se.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
	
	

}
