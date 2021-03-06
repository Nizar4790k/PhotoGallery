package com.example.photogallery;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public class PhotoGalleryFragment extends VisibleFragment {

    private RecyclerView mPhotoRecyclerView;
    private ProgressBar mProgressBar;
    private List<GalleryItem> mItems = new ArrayList<>();
    private boolean loading = true;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;



    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        updateItems();

        PollService.setServiceAlarm(getActivity(),true);


        Handler responseHandler = new Handler();

        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);

        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.
                ThumbnailDownloadListener<PhotoHolder>() {

            @Override
            public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {
                Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                photoHolder.bindDrawable(drawable);
               }
            }
        );


        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mPhotoRecyclerView = (RecyclerView) view.findViewById(R.id.photo_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        mProgressBar = view.findViewById(R.id.progress_bar);





        mPhotoRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {



            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {

                GridLayoutManager layoutManager =(GridLayoutManager) mPhotoRecyclerView.getLayoutManager();

               int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int pastVisiblesItems = layoutManager.findFirstVisibleItemPosition();

                if (loading) {
                    if (dy > 0) //check for scroll down
                    {
                        visibleItemCount = layoutManager.getChildCount();
                        totalItemCount = layoutManager.getItemCount();
                        pastVisiblesItems = layoutManager.findFirstVisibleItemPosition();

                        if ((visibleItemCount + pastVisiblesItems) >= totalItemCount) {


                            FetchItemsTask fetchItemsTask = new FetchItemsTask();
                            fetchItemsTask.execute();

                            mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
                            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));








                            Log.v("...", " Reached Last Item");


                        }

                    }
                }
            }



        });



        setupAdapter();
        return view;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");


    }

    private void setupAdapter() {
        if (isAdded()) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class FetchItemsTask extends AsyncTask<Void,Void,List<GalleryItem>> {

       private String mQuery;

       public FetchItemsTask(){

       }

       public FetchItemsTask(String query){
           mQuery = query;
       }

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {


            if(mQuery==null){
                return new FlickrFetchr().fetchRecentPhotos();
            }else {
                return new FlickrFetchr().searchPhotos(mQuery);
            }


        }


        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems=items;
            setupAdapter();
        }
    }


    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView mImageView ;
        private  GalleryItem mGalleryItem;

        public PhotoHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView;
            itemView.setOnClickListener(this);
        }

        public void bindDrawable (Drawable drawable){

            mImageView.setImageDrawable(drawable);
        }

        public void bindGalleryItem(GalleryItem galleryItem) {
            mGalleryItem = galleryItem;

        }


        @Override
        public void onClick(View v) {

            Intent intent = PhotoPageActivity.newIntent(getContext(),mGalleryItem.getPhotoPageUri());
            startActivity(intent);


        }


    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> mGalleryItems;
        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }
        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

            View view = LayoutInflater.from(getContext()).inflate(R.layout.list_item_gallery,viewGroup
                    ,false);

            return new PhotoHolder(view);
        }
        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            photoHolder.bindGalleryItem(galleryItem);
            Drawable placeHolder = getResources().getDrawable(R.drawable.linux_icon);
            photoHolder.bindDrawable(placeHolder);
            mThumbnailDownloader.queueThumbnail(photoHolder,galleryItem.getUrl());





        }





        @Override
        public int getItemCount() {
            return mGalleryItems.size();


        }


    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();mThumbnailDownloader.clearQueue();
    }


    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater menuInflater) {

        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.fragment_photo_gallery, menu);


        final MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String s) {

                Log.d(TAG, "QueryTextSubmit: " + s);
                QueryPreferences.setStoredQuery(getActivity(), s);
                hideKeyboardFrom(getContext(),searchView);

                MenuItemCompat.collapseActionView(searchItem);

                updateItems();

                return true;
            }

                @Override
                public boolean onQueryTextChange(String s) {
                Log.d(TAG, "QueryTextChange: " + s);
                return false;

            }

        });


        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
                searchItem.collapseActionView();

            }

        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);updateItems();
                return true;

            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                getActivity().invalidateOptionsMenu();
                return true;

                default:
                    return super.onOptionsItemSelected(item);
        }
    }



    private void updateItems() {

        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query).execute();

    }


    public static void hideKeyboardFrom(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

}


