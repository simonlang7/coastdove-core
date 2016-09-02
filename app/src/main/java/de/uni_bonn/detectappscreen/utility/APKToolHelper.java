// Code adapted from apktool
// Original author: Ryszard Wiśniewski <brut.alll@gmail.com>

package de.uni_bonn.detectappscreen.utility;

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
import brut.androlib.res.decoder.ResFileDecoder;
import brut.androlib.res.decoder.XmlPullStreamDecoder;
import brut.androlib.res.util.ExtFile;
import brut.directory.Directory;
import brut.directory.DirectoryException;
import brut.directory.FileDirectory;
import brut.util.Duo;

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
