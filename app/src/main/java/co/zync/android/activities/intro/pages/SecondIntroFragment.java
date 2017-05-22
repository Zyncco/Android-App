package co.zync.android.activities.intro.pages;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import co.zync.android.R;
import co.zync.android.ZyncApplication;
import co.zync.android.activities.SignInActivity;

public class SecondIntroFragment extends Fragment implements View.OnClickListener {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_intro_two, container, false);
        rootView.findViewById(R.id.start_app_button).setOnClickListener(this);
        // do whatever more here
        return rootView;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.start_app_button) {
            ((ZyncApplication) getActivity().getApplication()).getConfig().setSeenIntro(true);

            Intent intent = new Intent(getContext(), SignInActivity.class);
            intent.putExtra("intro_direct", true);
            startActivity(intent);
            getActivity().finish();
        }
    }
}
