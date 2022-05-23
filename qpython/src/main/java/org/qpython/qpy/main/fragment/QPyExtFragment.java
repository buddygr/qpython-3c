package org.qpython.qpy.main.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.qpython.qpy.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link QPyExtFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class QPyExtFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public QPyExtFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment QPyExtFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static QPyExtFragment newInstance(String param1, String param2) {
        QPyExtFragment fragment = new QPyExtFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_qpy_ext, container, false);
    }

    private static void viewWebSite(Activity activity,int resId) {
        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(activity.getString(resId))));
    }

    public static void viewPage(Activity activity, int pos){
        /*new AlertDialog.Builder(activity, R.style.MyDialog)
                .setTitle(R.string.notice)
                .setMessage(R.string.old_qpypi_invalid)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> {*/
        switch (pos) {
            case 1:
                viewWebSite(activity, R.string.qpython_sl4a_gui_gitee);
                break;
            case 2:
                viewWebSite(activity, R.string.qpython_3c_release_gitee);
        }
                /*})
                .setNeutralButton(R.string.cancel, (dialogInterface, i) -> {
                })
                .setOnCancelListener(dialogInterface -> {
                }).show();*/

    }
}