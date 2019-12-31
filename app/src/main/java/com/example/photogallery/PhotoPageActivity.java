package com.example.photogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;

import androidx.fragment.app.Fragment;

public class PhotoPageActivity extends SingleFragmentActivity {


    private Fragment mFragment;

    public static Intent newIntent(Context context, Uri photoPageUri) {
        Intent i = new Intent(context, PhotoPageActivity.class);
        i.setData(photoPageUri);
        return i;
    }


    @Override
    protected Fragment createFragment() {

        mFragment = PhotoPageFragment.newInstance(getIntent().getData());
        return mFragment ;
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();

        PhotoPageFragment photoPageFragment= (PhotoPageFragment) mFragment;
        WebView webView = photoPageFragment.getWebView();

        if(webView.canGoBack()){
            webView.goBack();
        }

    }
}
