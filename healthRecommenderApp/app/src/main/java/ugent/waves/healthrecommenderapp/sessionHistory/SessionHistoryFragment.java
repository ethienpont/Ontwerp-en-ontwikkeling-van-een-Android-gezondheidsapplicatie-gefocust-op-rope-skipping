package ugent.waves.healthrecommenderapp.sessionHistory;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ugent.waves.healthrecommenderapp.Asynctasks.ActivityAsyncTask;
import ugent.waves.healthrecommenderapp.Asynctasks.MistakeAsyncTask;
import ugent.waves.healthrecommenderapp.HelpClasses.Constants;
import ugent.waves.healthrecommenderapp.Persistance.ActivityDao;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.Mistake;
import ugent.waves.healthrecommenderapp.Persistance.MistakeDao;
import ugent.waves.healthrecommenderapp.Persistance.SessionActivity;
import ugent.waves.healthrecommenderapp.R;
import ugent.waves.healthrecommenderapp.healthRecommenderApplication;

public class SessionHistoryFragment extends Fragment {

    private int id;
    private healthRecommenderApplication app;

    private AppDatabase appDb;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private TimelineRecyclerAdapter timelineRecyclerAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.session_history_fragment, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);

        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        app = (healthRecommenderApplication) getContext().getApplicationContext();

        appDb = app.getAppDb();

        id = getArguments().getInt(Constants.ARG_DATA);

        timelineRecyclerAdapter = initTimelineData();

        recyclerView.setAdapter(timelineRecyclerAdapter);

        return view;
    }

    private TimelineRecyclerAdapter initTimelineData(){
        TimelineRecyclerAdapter timeline = new TimelineRecyclerAdapter();

        try {
            SessionActivity[] a = new ActivityAsyncTask( appDb, id, app.getAccount().getId(), Constants.GET_ALL).execute().get();
            Mistake[] m = new MistakeAsyncTask( appDb, id,app.getAccount().getId(), Constants.GET_ALL).execute().get();

            int mis = 0;
            for(Mistake j: m){
                if((a[0].getStart() <= j.getTime()) && (a[0].getEnd() > j.getTime())){
                    mis++;
                }
            }
            ActivityItem item = new ActivityItem(a[0].getStart().toString(), a[0].getEnd().toString(), a[0].getActivity(), mis);
            timeline.addActivity(item);

            for(int i=1; i<a.length; i++){
                mis = 0;
                for(Mistake j: m){
                    if((a[i].getStart() <= j.getTime()) && (a[i].getEnd() > j.getTime())){
                        mis++;
                    }
                }
                ActivityItem activityItem = new ActivityItem(a[i].getStart().toString(), a[i].getEnd().toString(), a[i].getActivity(), mis);

                timeline.addTimepoint(new TimePoint(a[i-1].getMET_score()+" points"));
                timeline.addActivity(activityItem);
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return timeline;
    }

    public static SessionHistoryFragment newInstance(int sessionId) {
        SessionHistoryFragment fragment = new SessionHistoryFragment();
        Bundle args = new Bundle();
        args.putInt(Constants.ARG_DATA, sessionId);
        fragment.setArguments(args);
        return fragment;
    }
}
