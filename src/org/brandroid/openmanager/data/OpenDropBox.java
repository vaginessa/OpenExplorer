package org.brandroid.openmanager.data;

import java.io.IOException;

import android.net.Uri;
import android.os.AsyncTask;

public class OpenDropBox extends OpenNetworkPath implements OpenPath.ListHandler {

    @Override
    public void list(ListListener listener) {
        //new Thread()
    }

    @Override
    public boolean syncUpload(OpenFile f, NetworkListener l) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean syncDownload(OpenFile f, NetworkListener l) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isConnected() throws IOException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public OpenNetworkPath[] getChildren() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPath() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getAbsolutePath() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long length() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public OpenPath getParent() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OpenPath getChild(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OpenPath[] list() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OpenPath[] listFiles() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Boolean isDirectory() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Boolean isFile() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Boolean isHidden() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri getUri() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Long lastModified() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Boolean canRead() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Boolean canExecute() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Boolean exists() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Boolean delete() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Boolean mkdir() {
        // TODO Auto-generated method stub
        return null;
    }

}
