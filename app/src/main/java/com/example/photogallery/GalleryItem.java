package com.example.photogallery;

import com.google.gson.annotations.SerializedName;

public class GalleryItem {


    private String mCaption;
    private String mId;
    private String mUrl;

    public GalleryItem() {
    }

    @Override
    public String toString() {
        return mCaption;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getCaption() {
        return mCaption;
    }

    public void setCaption(String caption) {
        mCaption = caption;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getId() {
        return mId;
    }
}
