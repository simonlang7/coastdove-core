/*  DetectAppScreen
    Copyright (C) 2016  Simon Lang
    Contact: simon.lang7 at gmail dot com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package de.uni_bonn.detectappscreen.utility;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * A collection of XML-related functions
 */
public class XMLHelper {

    /**
     * Parses an XML file
     * @param filename    Full path (including directories) to the XML file
     * @return The XML as a Document
     */
    public static Document parseXMLFile(String filename) {
        File file = new File(filename);
        return parseXMLFile(file);
    }

    /**
     * Parses an XML file
     * @param basePath    Path to the directory that contains the XML file
     * @param filename    Filename (without directory) of the XML file
     * @return The XML as a Document
     */
    public static Document parseXMLFile(String basePath, String filename) {
        File file = new File(basePath, filename);
        return parseXMLFile(file);
    }

    /**
     * Parses an XML file
     * @param file    File to parse from
     * @return The XML as a Document
     */
    public static Document parseXMLFile(File file) {
        Document result = null;

        if (!file.exists())
            return null;

        // I just want to parse a file...
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            result = db.parse(file);
        } catch (Exception e) {
            System.err.println("Error parsing file (" + file.getAbsolutePath() + "): " + e.getMessage());
        }

        return result;
    }

    /**
     * Determines whether the data from the given InputStream is a binary XML
     * Code taken from apk2xml by Ivo Hradek, licensed under the Apache License 2.0
     * @param stream    Stream to read from
     * @return True if the stream contains a binary XML, false if not
     * @throws IOException If the stream cannot be read
     */
    public static boolean isBinaryXML(InputStream stream) throws IOException {
        byte[] expect = new byte[]{0x03, 0x00, 0x08, 0x00};
        byte[] magic = new byte[4];
        stream.read(magic);
        return Arrays.equals(magic, expect);
    }
}
