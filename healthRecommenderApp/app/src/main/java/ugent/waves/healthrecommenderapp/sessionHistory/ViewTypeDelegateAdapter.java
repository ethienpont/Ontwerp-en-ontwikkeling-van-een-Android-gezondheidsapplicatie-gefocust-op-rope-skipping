package ugent.waves.healthrecommenderapp.sessionHistory;

import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

interface ViewTypeDelegateAdapter {

    RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p);

    void onBindViewHolder(RecyclerView.ViewHolder holder, ViewType item);
}
