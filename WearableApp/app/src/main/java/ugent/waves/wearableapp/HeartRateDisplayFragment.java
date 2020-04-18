package ugent.waves.wearableapp;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.wearable.Node;

import org.w3c.dom.Text;

import java.util.List;
import java.util.Map;

import androidx.fragment.app.Fragment;

public class HeartRateDisplayFragment extends Fragment {

    private TextView t;

    public static Fragment newInstance(Map<String,Object> data) {
        HeartRateDisplayFragment fragment = new HeartRateDisplayFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.heart_rate_display_fragment, container, false);

        t = (TextView) view.findViewById(R.id.hr);

        ImageView stop = (ImageView) view.findViewById(R.id.stop);
        stop.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ((SessionActivity)getActivity()).stopSession();
            }
        });

        return view;
    }

    //TODO: heartrate is 0
    //TODO: als boven 220-age komt -> rood + melding
    public void showHeartRate(float hr){
        t.setText((int) hr+"");
        if(hr > 90){
            t.setTextColor(Color.RED);
        } else{
            t.setTextColor(Color.WHITE);
        }
    }

}
