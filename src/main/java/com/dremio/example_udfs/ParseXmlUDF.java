/*
 * Copyright (C) 2017-2021 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Usage:  Call this UDF from a Dremio VDS like this:
 *
 *
SELECT parse_xml_udf(
'<?xml version="1.0" encoding="UTF-8"?>
<epl>
  <region id="eastern">
    <player id="1">
      <name>Harry Kane</name>
      <position>First Base</position>
    </player>
  </region>
  <region id="western">
    <player id="2">
      <name>Bruno Fernandes</name>
      <position>Third Base</position>
    </player>
    <player id="3">
      <name>Sam Grady</name>
      <position>Picher</position>
    </player>
  </region>
</epl>',
  '(//epl/region[@id="western"]/player/name/text())') AS player_names 
FROM (VALUES(1));

 */

package com.dremio.example_udfs;

import java.util.*;
import javax.inject.Inject;
import org.apache.arrow.memory.ArrowBuf;
import org.apache.arrow.vector.holders.VarCharHolder;
import com.dremio.exec.expr.SimpleFunction;
import com.dremio.exec.expr.annotations.FunctionTemplate;
import com.dremio.exec.expr.annotations.FunctionTemplate.NullHandling;
import com.dremio.exec.expr.annotations.Output;
import com.dremio.exec.expr.annotations.Param;
import com.dremio.exec.expr.fn.impl.StringFunctionHelpers.*;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.StringReader;
import java.io.IOException;

public class ParseXmlUDF {

    private static boolean isAsciiPrintable(String str) {
      if (str == null) {
          return false;
      }
      int sz = str.length();
      for (int i = 0; i < sz; i++) {
          if (isAsciiPrintable(str.charAt(i)) == false) {
              return false;
          }
      }
      return true;
    }
    private static boolean isAsciiPrintable(char ch) {
      return ch >= 32 && ch < 127;
    }

    static String parseXPath(String xml_content, String xpath_expression){

        Document document = convertStringToXMLDocument(xml_content);

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();
	StringBuilder result = new StringBuilder();

        try {
            XPathExpression xPathExpression = xPath.compile(xpath_expression);
            NodeList nodeList = (NodeList) xPathExpression.evaluate(document, XPathConstants.NODESET);
	    String nodeName = "";

	    // Create a JSON like output, with embedded XML if returned.
	    result.append("{\"results\": [");
            for(int i=0; i<nodeList.getLength(); i++){
	        if (nodeList.item(i).getNodeValue() != null 
		    && isAsciiPrintable(nodeList.item(i).getNodeValue()))
		{
		  //System.out.println("parse_xml_udf() parseXPath() Node Value: [" + nodeList.item(i).getNodeValue() + "]");
		  if (i>0)
		    result.append(",");
		  result.append("\"");
		  result.append(nodeList.item(i).getNodeValue());
		  result.append("\"");
		}
            }
	    result.append("]}");

        } catch (XPathExpressionException e) {
            e.printStackTrace();
            return null;
        }

        return result.toString();
    }

    private static Document convertStringToXMLDocument(String xml_content){
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = null;
        Document doc = null;

        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            doc = documentBuilder.parse(new InputSource(new StringReader(xml_content)));

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        } catch (SAXException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return doc;
    }

  @FunctionTemplate(
    name = "parse_xml_udf",
    scope = FunctionTemplate.FunctionScope.SIMPLE,
    nulls = NullHandling.NULL_IF_NULL)

  public static class ExampleParseXmlUdf implements SimpleFunction {

    @Param private VarCharHolder xmlContent;

    @Param private VarCharHolder xpath;

    @Output private VarCharHolder out;

    @Inject private ArrowBuf buffer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {

      final int bytesXmlContentArg = xmlContent.end - xmlContent.start;
      final int bytesXpathArg = xpath.end - xpath.start;

      try {
        String xmlContentStr = com.dremio.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8( 
			         xmlContent.start, xmlContent.end, xmlContent.buffer);
        String xpathStr      = com.dremio.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8( 
			         xpath.start, xpath.end, xpath.buffer);
	String resultStr     = null;

	resultStr =  com.dremio.example_udfs.ParseXmlUDF.parseXPath(xmlContentStr, xpathStr);

	if (resultStr != null) {
          int finalLength = resultStr.getBytes().length;
          buffer = out.buffer = buffer.reallocIfNeeded(finalLength);
          out.start = 0;
          out.end = finalLength;
	  buffer.setBytes(0, resultStr.getBytes());
        } else {
          // Do nothing if no XML was returned
	  String tmpStr =  new String("");
          int finalLength = tmpStr.getBytes().length;
          buffer = out.buffer = buffer.reallocIfNeeded(finalLength);
          out.start = 0;
          out.end = finalLength;
	  buffer.setBytes(0, tmpStr.getBytes());
	}
      
      } catch (RuntimeException ex) {
            System.err.println(" Exception in Dremio UDF: parse_xml_udf():" + ex);
      }
    }



  }
}
