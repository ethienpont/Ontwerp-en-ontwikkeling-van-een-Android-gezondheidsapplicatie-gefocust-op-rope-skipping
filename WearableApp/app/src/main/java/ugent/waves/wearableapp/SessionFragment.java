package ugent.waves.wearableapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import androidx.fragment.app.Fragment;

public class SessionFragment extends Fragment {
    private static final String ARG_DATA = "TURNS";
    private String turns;

    public static Fragment newInstance(Map<String,Object> data) {
        SessionFragment fragment = new SessionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DATA, (String) data.get("turns"));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        turns = getArguments().<SessionFragment>getString(ARG_DATA);
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.session_fragment, container, false);
    }
}
