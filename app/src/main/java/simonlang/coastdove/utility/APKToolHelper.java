/*  Coast Dove
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


// Code adapted from apktool, licensed under the Apache License 2.0
// Original author: Ryszard Wiśniewski <brut.alll@gmail.com>

package simonlang.coastdove.utility;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import brut.androlib.AndrolibException;
import brut.androlib.ApkOptions;
import brut.androlib.res.AndrolibResources;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResResource;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.decoder.AXmlResourceParser;
import brut.androlib.res.decoder.ResAttrDecoder;
import brut.androlib.res.decoder.XmlPullStreamDecoder;
import brut.androlib.res.util.ExtFile;
import brut.directory.DirectoryException;

/**
 * Some functions to help extract necessary data from APK files using apktool
 */
public class APKToolHelper {

    /** APK file to process */
    private ExtFile apk;

    /** Internal resources */
    private AndrolibResources androlibResources;
    /** Internal resource table */
    private ResTable resTable;
    /** Internal binary XML decoder */
    private XmlPullStreamDecoder xmlPullStreamDecoder;
    /** Internal decoder for resource attributes */
    private ResAttrDecoder attrDecoder;

    /** Current internal package */
    private ResPackage currentResPackage;
    /** Current internal resource */
    private ResResource currentResResource;

    /** Internal iterator for packages */
    private Iterator<ResPackage> resPackageIterator;
    /** Internal iterator for resources */
    private Iterator<ResResource> resResourceIterator;


    /** Creates an APKToolHelper for the given .apk file */
    public APKToolHelper(File apkFile) {
        this.apk = new ExtFile(apkFile);
        init();
    }

    /**
     * Returns an input stream for AndroidManifest.xml, as human-readable XML
     */
    public InputStream manifestInputStream() {
        return new ByteArrayInputStream(decodeManifestWithResources());
    }

    /**
     * Returns an input stream for the current resource, as human-readable XML
     */
    public InputStream currentResourceInputStream() {
        return new ByteArrayInputStream(decodeCurrentResource());
    }

    /**
     * Returns true if the currently selected resource contains the given sub-path
     */
    public boolean currentResourceHasSubPath(String subPath) {
        return currentResResource != null && currentResResource.toString().contains(subPath);
    }

    /**
     * Switches to the next resource that either contains the given sub-path, or to null
     */
    public void nextResourceWithSubPath(String subPath) {
        do {
            next();
        } while (currentResResource != null &&
                currentResResource.toString().contains(subPath));
    }

    /**
     * Decodes the current resource to human-readable XML
     * @return byte array of the decoded resource
     */
    private byte[] decodeCurrentResource() {
        byte[] result = null;

        try {
            InputStream in = apk.getDirectory().getFileInput(currentResResource.getFilePath());
            ByteArrayOutputStream out = new ByteArrayOutputStream(in.available());

            this.xmlPullStreamDecoder.decode(in, out);
            result = out.toByteArray();
        } catch (DirectoryException e) {
            Log.e("APKToolHelper", "Directory exception: " + e.toString());
        } catch (IOException e) {
            Log.e("APKToolHelper", "IO error: " + e.toString());
        } catch (AndrolibException e) {
            Log.e("APKToolHelper", "Error in Androlib: " + e.toString());
        }

        return result;
    }

    /**
     * Decodes AndroidManifest.xml
     * @return Contents of AndroidManifest.xml (non-binary) for further processing
     */
    private byte[] decodeManifestWithResources() {
        byte[] result = null;

        try {
            if (hasManifest()) {
                InputStream in = apk.getDirectory().getFileInput("AndroidManifest.xml");
                ByteArrayOutputStream out = new ByteArrayOutputStream(in.available());

                this.xmlPullStreamDecoder.decodeManifest(in, out);
                result = out.toByteArray();
            }
        } catch (AndrolibException e) {
            Log.e("APKToolHelper", "Error in Androlib: " + e.toString());
        } catch (DirectoryException e) {
            Log.e("APKToolHelper", "Directory exception: " + e.toString());
        } catch (IOException e) {
            Log.e("APKToolHelper", "IO error: " + e.toString());
        }

        return result;
    }

    /**
     * Returns true if the APK file contains AndroidManifest.xml
     */
    private boolean hasManifest() {
        try {
            return this.apk.getDirectory().containsFile("AndroidManifest.xml");
        } catch (DirectoryException e) {
            Log.e("APKToolHelper", "Error accessing APK file: " + e.getMessage());
            return false;
        }
    }

    private boolean hasResources() {
        try {
            return this.apk.getDirectory().containsFile("resources.arsc");
        } catch (DirectoryException e) {
            Log.e("APKToolHelper", "Error accessing APK file: " + e.getMessage());
            return false;
        }
    }

    private void init() {
        this.androlibResources = new AndrolibResources();
        androlibResources.apkOptions = new ApkOptions();

        AXmlResourceParser axmlParser = new AXmlResourceParser();
        axmlParser.setAttrDecoder(new ResAttrDecoder());
        this.xmlPullStreamDecoder = new XmlPullStreamDecoder(axmlParser, androlibResources.getResXmlSerializer());
        try {
            this.resTable = androlibResources.getResTable(apk, hasResources());
            this.attrDecoder = axmlParser.getAttrDecoder();
            // Initialize attribute decoder to first package
            this.resPackageIterator = resTable.listMainPackages().iterator();
            nextPackage();
            this.attrDecoder.setCurrentPackage(this.currentResPackage);


        } catch (AndrolibException e) {
            Log.e("APKToolHelper", "Error in Androlib: " + e.getMessage());
            throw new RuntimeException("Error in Androlib: " + e.getMessage());
        }
    }

    private void next() {
        if (resResourceIterator.hasNext())
            nextResource();
        else if (resPackageIterator.hasNext())
            nextPackage();
        else {
            currentResPackage = null;
            currentResResource = null;
        }
    }

    private void nextPackage() {
        this.currentResPackage = this.resPackageIterator.next();
        this.resResourceIterator = currentResPackage.listFiles().iterator();
        this.attrDecoder.setCurrentPackage(currentResPackage);
    }

    private void nextResource() {
        this.currentResResource = this.resResourceIterator.next();
    }
}
