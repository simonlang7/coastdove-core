// Code adapted from apktool
// Original author: Ryszard Wiśniewski <brut.alll@gmail.com>

package de.uni_bonn.detectappscreen.utility;

import android.util.Log;

import java.io.File;

import brut.androlib.AndrolibException;
import brut.androlib.ApkOptions;
import brut.androlib.res.AndrolibResources;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.decoder.AXmlResourceParser;
import brut.androlib.res.decoder.ResAttrDecoder;
import brut.androlib.res.decoder.ResFileDecoder;
import brut.androlib.res.util.ExtFile;
import brut.directory.DirectoryException;
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
    public static byte[] decodeManifestWithResources(File apkFile) {
        AndrolibResources androlibResources = new AndrolibResources();
        androlibResources.apkOptions = new ApkOptions();
        ExtFile apkExtFile = new ExtFile(apkFile);
        FakeDirectory fakeDirectory = new FakeDirectory();

        try {
            boolean hasManifest = apkExtFile.getDirectory().containsFile("AndroidManifest.xml");
            boolean hasResources = apkExtFile.getDirectory().containsFile("resources.arsc");
            if (hasManifest) {
                ResTable resTable = androlibResources.getResTable(apkExtFile, hasResources);
                Duo<ResFileDecoder, AXmlResourceParser> duo = androlibResources.getResFileDecoder();
                ResFileDecoder fileDecoder = duo.m1;
                ResAttrDecoder attrDecoder = duo.m2.getAttrDecoder();

                attrDecoder.setCurrentPackage(resTable.listMainPackages().iterator().next());
                fileDecoder.decodeManifest(apkExtFile.getDirectory(), "AndroidManifest.xml", fakeDirectory, "AndroidManifest.xml");
            }
        } catch (AndrolibException e) {
            Log.e("APKToolHelper", "Error in Androlib: " + e.toString());
        } catch (DirectoryException e) {
            Log.e("APKToolHelper", "Directory exception: " + e.toString());
        }

        return fakeDirectory.toByteArray();
    }
}
