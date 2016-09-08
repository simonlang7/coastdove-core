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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import brut.androlib.AndrolibException;
import brut.androlib.ApkOptions;
import brut.androlib.res.AndrolibResources;
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
    /**
     * Decodes AndroidManifest.xml from an APK file using apktool
     * @param apkFile    APK file from which to parse AndroidManifest.xml
     * @return Contents of AndroidManifest.xml (non-binary) for further processing
     */
    public static byte[] decodeManifestWithResources(Context context, File apkFile) {
        AndrolibResources androlibResources = new AndrolibResources();
        androlibResources.apkOptions = new ApkOptions();
        ExtFile apkExtFile = new ExtFile(apkFile);
        byte[] result = null;

        try {
            boolean hasManifest = apkExtFile.getDirectory().containsFile("AndroidManifest.xml");
            boolean hasResources = apkExtFile.getDirectory().containsFile("resources.arsc");
            if (hasManifest) {
                InputStream in = apkExtFile.getDirectory().getFileInput("AndroidManifest.xml");
                ByteArrayOutputStream out = new ByteArrayOutputStream(in.available());

                AXmlResourceParser axmlParser = new AXmlResourceParser();
                axmlParser.setAttrDecoder(new ResAttrDecoder());
                XmlPullStreamDecoder xmlPullStreamDecoder = new XmlPullStreamDecoder(axmlParser, androlibResources.getResXmlSerializer());
                ResTable resTable = androlibResources.getResTable(apkExtFile, hasResources);
                ResAttrDecoder attrDecoder = axmlParser.getAttrDecoder();
                attrDecoder.setCurrentPackage(resTable.listMainPackages().iterator().next());

                xmlPullStreamDecoder.decodeManifest(in, out);
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
    
}
