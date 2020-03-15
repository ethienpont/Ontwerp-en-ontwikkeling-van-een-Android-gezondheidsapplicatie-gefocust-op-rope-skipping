package ugent.waves.healthrecommenderapp;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.fitness.data.DataSet;

import java.util.ArrayList;
import java.util.List;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import ugent.waves.healthrecommenderapp.databinding.SessionHistoryFragmentBinding;

public class SessionHistoryFragment extends Fragment {

    private static final String ARG_DATA = "dataSession";
    private static final String TAG = "SessionHistoryFragment";
    private SessionHistoryData data;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        data = getArguments().<SessionHistoryData>getParcelable(ARG_DATA);
        // Inflate the layout for this fragment
        SessionHistoryFragmentBinding binding = DataBindingUtil.inflate(inflater, R.layout.session_history_fragment, container, false);
        View view = binding.getRoot();
        binding.setSessionData(data);
        return view;

    }

    public static SessionHistoryFragment newInstance(SessionHistoryData data) {
        SessionHistoryFragment fragment = new SessionHistoryFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_DATA, data);
        fragment.setArguments(args);
        return fragment;
    }
}
