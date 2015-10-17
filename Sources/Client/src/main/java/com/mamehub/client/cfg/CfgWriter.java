package com.mamehub.client.cfg;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CfgWriter {
  public CfgWriter(File file, final Map<String, String> inputs_readonly)
      throws IOException, ParserConfigurationException, SAXException,
      TransformerException {
    Map<String, String> inputs = new HashMap<String, String>(inputs_readonly);
    DocumentBuilderFactory docbf = DocumentBuilderFactory.newInstance();
    docbf.setNamespaceAware(true);
    DocumentBuilder docbuilder = docbf.newDocumentBuilder();
    Document document = docbuilder.parse(new FileInputStream(file), ".");

    NodeList portElements = document.getElementsByTagName("port");
    List<Node> nodesToRemove = new ArrayList<Node>();
    for (int a = 0; a < portElements.getLength(); a++) {
      Node portElement = portElements.item(a);
      String portType = portElement.getAttributes().getNamedItem("type")
          .getNodeValue();
      String newKeystroke = inputs.get(portType);
      if (newKeystroke != null) {
        getFirstChildOfType(portElement, "newseq").setTextContent(newKeystroke);
      } else {
        nodesToRemove.add(portElement);
      }
      inputs.remove(portType);
    }
    for (Node node : nodesToRemove) {
      node.getParentNode().removeChild(node);
    }
    for (Map.Entry<String, String> entry : inputs.entrySet()) {
      Node inputNode = getFirstChildOfType(
          getFirstChildOfType(document.getDocumentElement(), "system"), "input");
      Element portElement = document.createElement("port");
      portElement.setAttribute("type", entry.getKey());
      Element newseqElement = document.createElement("newseq");
      newseqElement.setAttribute("type", "standard");
      newseqElement.setTextContent(entry.getValue());
      portElement.appendChild(newseqElement);
      inputNode.appendChild(portElement);
    }

    // Use a Transformer for output
    TransformerFactory tFactory = TransformerFactory.newInstance();
    Transformer transformer = tFactory.newTransformer();

    DOMSource source = new DOMSource(document);
    StreamResult result = new StreamResult(file);
    transformer.transform(source, result);
  }

  private Node getFirstChildOfType(Node node, String string) {
    NodeList children = node.getChildNodes();
    for (int a = 0; a < children.getLength(); a++) {
      if (children.item(a).getNodeName().equals(string)) {
        return children.item(a);
      }
    }
    return null;
  }

  /**
   * @param args
   * @throws SAXException
   * @throws ParserConfigurationException
   * @throws IOException
   * @throws TransformerException
   */
  public static void main(String[] args) throws IOException,
      ParserConfigurationException, SAXException, TransformerException {
    Map<String, String> newInputs = new HashMap<String, String>();
    newInputs.put("P1_JOYSTICK_UP", "KEYCODE_I");
    newInputs.put("P1_JOYSTICK_RIGHT", "KEYCODE_L");
    CfgWriter writer = new CfgWriter(new File("../cfg/default.cfg"), newInputs);
  }

}
