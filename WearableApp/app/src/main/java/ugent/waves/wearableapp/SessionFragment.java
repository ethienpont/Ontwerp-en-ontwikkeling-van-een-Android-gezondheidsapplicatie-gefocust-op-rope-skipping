package ugent.waves.wearableapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import androidx.fragment.app.Fragment;

public class SessionFragment extends Fragment {

    public static Fragment newInstance(Map<String,Object> data) {
        SessionFragment fragment = new SessionFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.session_fragment, container, false);

        ImageView button = (ImageView) view.findViewById(R.id.image);
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ((SessionActivity)getActivity()).showDialog();
            }
        });

        ImageView signOut = (ImageView) view.findViewById(R.id.sign_out);
        signOut.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ((SessionActivity)getActivity()).signOut();
            }
        });
        // Inflate the layout for this fragment
        return view;
    }

}
