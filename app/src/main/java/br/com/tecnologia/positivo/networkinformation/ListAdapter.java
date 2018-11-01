package br.com.tecnologia.positivo.networkinformation;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * General adapter for recycle view.
 * If other items need to be handle, just create new constructor with items list and add onItemClick(Object item) at interface
 */

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.GeneralViewHolder> {

    private final List<String> items;

    ListAdapter(List<String> itemList) {
        items = itemList;
    }

    @Override
    public GeneralViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View view = inflater.inflate(R.layout.lay_row, viewGroup, false);

        return new GeneralViewHolder(view);
    }

    @Override
    public void onBindViewHolder(GeneralViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    void updateItems(HashMap<String, String> map) {
        items.clear();
        for (String key: map.keySet()) {
            String item = map.get(key);
            if (TextUtils.isEmpty(item)) {
                items.add(key);
            }else
                items.add(key+": "+ item);
        }
        notifyDataSetChanged();
    }


    static class GeneralViewHolder extends RecyclerView.ViewHolder {

        private final TextView tviText;

        GeneralViewHolder(View itemView) {
            super(itemView);

            tviText = itemView.findViewById(R.id.tvi_text);

        }

        void bind(final String item) {
            tviText.setText(item);
        }
    }
}
