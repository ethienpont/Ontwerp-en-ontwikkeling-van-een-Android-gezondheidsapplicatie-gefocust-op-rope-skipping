package ugent.waves.wearableapp;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import java.util.Map;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

public class BluetoothDialogFragment extends DialogFragment {

    public static BluetoothDialogFragment newInstance() {
        BluetoothDialogFragment fragment = new BluetoothDialogFragment();
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Please enable bluetooth on smartphone for optimal experience.")
                .setNegativeButton("ignore", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ((SessionActivity)getActivity()).chooseNode();
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
