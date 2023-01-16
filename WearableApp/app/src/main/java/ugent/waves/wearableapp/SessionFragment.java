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

//Display button to start session
public class SessionFragment extends Fragment {

    public static Fragment newInstance() {
        SessionFragment fragment = new SessionFragment();
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
        // Inflate the layout for this fragment
        return view;
    }

}
