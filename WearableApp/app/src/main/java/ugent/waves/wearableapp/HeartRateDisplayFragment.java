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

//Display heartrate during session
public class HeartRateDisplayFragment extends Fragment {

    private TextView t;
    private wearableAppApplication app;

    public static Fragment newInstance() {
        HeartRateDisplayFragment fragment = new HeartRateDisplayFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.heart_rate_display_fragment, container, false);

        app = (wearableAppApplication) getContext().getApplicationContext();

        t = (TextView) view.findViewById(R.id.hr);

        //When clicked on stop button -> call method from parent activity
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

    public void showHeartRate(float hr){
        t.setText((int) hr+"");
        int HRMAX = app.getAge() == 0 ? 195 : 220 - app.getAge();
        if(hr > HRMAX){
            t.setTextColor(Color.RED);
        } else{
            t.setTextColor(Color.WHITE);
        }
    }

}
