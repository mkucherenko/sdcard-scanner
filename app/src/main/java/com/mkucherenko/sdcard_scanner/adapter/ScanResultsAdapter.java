package com.mkucherenko.sdcard_scanner.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mkucherenko.sdcard_scanner.R;
import com.mkucherenko.sdcard_scanner.model.ScanResults;
import com.mkucherenko.sdcard_scanner.utils.TextUtils;

import java.io.File;
import java.util.Map;

/**
 * Created by Zim on 4/24/2016.
 */
public class ScanResultsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private static final int VIEW_TYPE_AVG_HEADER = 0;
    private static final int VIEW_TYPE_SIZE_HEADER = 1;
    private static final int VIEW_TYPE_EXT_HEADER = 2;
    private static final int VIEW_TYPE_ITEM = 3;

    private ScanResults mScanResults;
    private LayoutInflater mInflater;
    private Context mContext;

    public ScanResultsAdapter(Context context, ScanResults scanResults){
        mScanResults = scanResults;
        mInflater = LayoutInflater.from(context);
        mContext = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_AVG_HEADER) {
            View view = mInflater.inflate(R.layout.header_avg_size, parent, false);
            return new HeaderViewHolder(view);
        }else if(viewType == VIEW_TYPE_ITEM) {
            View view = mInflater.inflate(R.layout.item_file_size, parent, false);
            return new FileSizeViewHolder(view);
        }else if(viewType == VIEW_TYPE_EXT_HEADER){
            View view = mInflater.inflate(R.layout.item_filesize_header, parent, false);
            return new HeaderViewHolder(view);
        }else{
            View view = mInflater.inflate(R.layout.item_filesize_header, parent, false);
            return new HeaderViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        position = position - 1;
        if(viewType == VIEW_TYPE_ITEM) {
            FileSizeViewHolder viewHolder = (FileSizeViewHolder)holder;
            if (position < getLargestFilesCount()) {
                position = position - 1;
                File largeFile = mScanResults.getLargestFiles().get(position);
                viewHolder.mTitleTextView.setText(largeFile.getName());
                viewHolder.mDescTextView.setText(TextUtils.formatFileSize(mContext, largeFile.length()));
            } else {
                position = position - getLargestFilesCount();
                position = position - 1;
                Map.Entry<String, Integer> ext = mScanResults.getMostRecentExt().get(position);

                String extName = ext.getKey();
                if(!android.text.TextUtils.isEmpty(extName)){
                    viewHolder.mTitleTextView.setText('.' + ext.getKey());
                }else {
                    viewHolder.mTitleTextView.setText(R.string.label_no_ext);
                }

                viewHolder.mDescTextView.setText(Integer.toString(ext.getValue()));
            }
        }else if(viewType == VIEW_TYPE_SIZE_HEADER){
            HeaderViewHolder viewHolder = (HeaderViewHolder)holder;
            viewHolder.mTitleTextView.setText(R.string.label_largest_files);
            viewHolder.mNameTextView.setText(R.string.label_filename);
            viewHolder.mDescTextView.setText(R.string.label_size);
        }else if(viewType == VIEW_TYPE_EXT_HEADER){
            HeaderViewHolder viewHolder = (HeaderViewHolder)holder;
            viewHolder.mTitleTextView.setText(R.string.label_recent_ext);
            viewHolder.mNameTextView.setText(R.string.label_extension);
            viewHolder.mDescTextView.setText(R.string.label_count);
        }else if(viewType == VIEW_TYPE_AVG_HEADER){
            HeaderViewHolder viewHolder = (HeaderViewHolder)holder;
            viewHolder.mDescTextView.setText(TextUtils.formatFileSize(mContext, mScanResults.getAvgFileSize()));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(position == 0){
            return VIEW_TYPE_AVG_HEADER;
        }else if(getFilesFirstPosition() == position){
            return VIEW_TYPE_SIZE_HEADER;
        }else if(getExtFirstPosition() == position){
            return VIEW_TYPE_EXT_HEADER;
        }else {
            return VIEW_TYPE_ITEM;
        }
    }

    private int getFilesFirstPosition(){
        return getLargestFilesCount() > 0 ? 1 : -1;
    }

    private int getLargestFilesCount(){
        int largestFilesCount = mScanResults.getLargestFiles().size();
        largestFilesCount = largestFilesCount > 0 ? largestFilesCount + 1 : 0;
        return largestFilesCount;
    }

    private int getExtFirstPosition(){
        int filesPosition = getFilesFirstPosition();
        int position = -1;
        if(filesPosition >= 0){
            position = filesPosition + getLargestFilesCount();
        }
        return getMostRecentExtCount() > 0 ? position : -1;
    }

    private int getMostRecentExtCount(){
        int fileExtCount = mScanResults.getMostRecentExt().size();
        fileExtCount = fileExtCount > 0 ? fileExtCount + 1 : 0;
        return fileExtCount;
    }

    @Override
    public int getItemCount() {
        return getLargestFilesCount() + getMostRecentExtCount() + 1;
    }

    private class FileSizeViewHolder extends RecyclerView.ViewHolder{
        TextView mTitleTextView;
        TextView mDescTextView;

        public FileSizeViewHolder(View itemView) {
            super(itemView);
            mTitleTextView = (TextView)itemView.findViewById(R.id.filename_textview);
            mDescTextView = (TextView)itemView.findViewById(R.id.size_textview);
        }
    }

    private class HeaderViewHolder extends RecyclerView.ViewHolder{
        TextView mTitleTextView;
        TextView mNameTextView;
        TextView mDescTextView;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            mTitleTextView = (TextView)itemView.findViewById(R.id.title_textview);
            mNameTextView = (TextView)itemView.findViewById(R.id.name_textview);
            mDescTextView = (TextView)itemView.findViewById(R.id.desc_textview);
        }
    }
}
