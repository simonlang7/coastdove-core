package de.uni_bonn.detectappscreen.utility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import brut.directory.Directory;
import brut.directory.DirectoryException;
import brut.directory.PathNotExist;

/**
 * Fake brut.directory.Directory that redirects output into a ByteArrayOutputStream
 */
public class FakeDirectory implements Directory {
    private final Map<String, Directory> emptyMap = new HashMap<>();
    private final TreeSet<String> emptySet = new TreeSet<>(new CollatorWrapper());
    private ByteArrayOutputStream byteArrayOutputStream;

    public FakeDirectory() {
        this.byteArrayOutputStream = new ByteArrayOutputStream();
    }

    public FakeDirectory(int byteArraySize) {
        this.byteArrayOutputStream = new ByteArrayOutputStream(byteArraySize);
    }

    @Override
    public String toString() {
        return byteArrayOutputStream.toString();
    }

    public byte[] toByteArray() {
        return byteArrayOutputStream.toByteArray();
    }

    @Override
    public Set<String> getFiles(boolean b) {
        return emptySet;
    }

    @Override
    public Map<String, Directory> getDirs() {
        return emptyMap;
    }

    @Override
    public boolean containsFile(String s) {
        return false;
    }

    @Override
    public boolean containsDir(String s) {
        return false;
    }

    @Override
    public InputStream getFileInput(String s) throws DirectoryException {
        return null;
    }

    @Override
    public OutputStream getFileOutput(String s) throws DirectoryException {
        return byteArrayOutputStream;
    }

    @Override
    public Directory getDir(String s) throws PathNotExist {
        return null;
    }

    @Override
    public Directory createDir(String s) throws DirectoryException {
        return null;
    }

    @Override
    public boolean removeFile(String s) {
        return false;
    }

    @Override
    public void copyToDir(Directory directory) throws DirectoryException {
    }

    @Override
    public void copyToDir(File file) throws DirectoryException {
    }

    @Override
    public void copyToDir(File file, String[] strings) throws DirectoryException {
    }

    @Override
    public void copyToDir(File file, String s) throws DirectoryException {
    }

    @Override
    public int getCompressionLevel(String s) throws DirectoryException {
        return 0;
    }
}
