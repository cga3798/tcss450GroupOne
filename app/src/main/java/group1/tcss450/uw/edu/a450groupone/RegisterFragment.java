package group1.tcss450.uw.edu.a450groupone;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import group1.tcss450.uw.edu.a450groupone.model.Credentials;


/**
 * A simple {@link Fragment} subclass.
 */
public class RegisterFragment extends Fragment {

    private static final int MIN_PASS_LENGTH = 6;

    private OnRegistrationCompleteListener mListener;

    public RegisterFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v =  inflater.inflate(R.layout.fragment_register, container, false);

        Button b = (Button) v.findViewById(R.id.registerButtonRegister);
        b.setOnClickListener(view -> {verifyFieldsInfoAndSend(v);});

        return v;
    }

    /**
     * It checks that the fields are not empty and checks for constraints.
     * TODO: Interaction listener is not implemented yet.
     *
     * @param v the view
     */
    public void verifyFieldsInfoAndSend(View v) {

        EditText firstEt = (EditText) v.findViewById(R.id.registerEditTextFirst);
        EditText lastEt = (EditText) v.findViewById(R.id.registerEditTextLast);
        EditText emailEt = (EditText) v.findViewById(R.id.registerEditTextEmail);
        EditText usernameET = (EditText) v.findViewById(R.id.registerEditTextNickname);
        EditText passEt = (EditText) v.findViewById(R.id.registerEditTextPassword);
        EditText confirmPassEt = (EditText) v.findViewById(R.id.registerEditTextConfirmPass);


        // Check empty fields, matching, length reqs. TODO: verify email doesn't have account already
        if(firstEt.getText().toString().isEmpty() ) {
            firstEt.setError(getString(R.string.se_first_name));
        } else if( lastEt.getText().toString().isEmpty()) {
            lastEt.setError(getString(R.string.se_last_name));
        } else if( emailEt.getText().toString().isEmpty()) {
            emailEt.setError(getString(R.string.se_email));
        } else if( usernameET.getText().toString().isEmpty()) {
            usernameET.setError(getString(R.string.se_username));
        } else if(passEt.getText().toString().isEmpty()) {
            passEt.setError(getString(R.string.se_pass));
        } else if (confirmPassEt.getText().toString().isEmpty()) {
            confirmPassEt.setError(getString(R.string.se_confirm_pass));
        }

        // check user doesnt put '_' (underscore) in their first or last name
        // --- because it's a delimiter for naming chats
        if (firstEt.getText().toString().contains("_")
                || lastEt.getText().toString().contains("_")) {
            firstEt.setError(getString(R.string.se_dont_use_underscore));
        }

        if (passEt.getText().toString().length() < MIN_PASS_LENGTH) {
            passEt.setError(getString(R.string.se_pass_tooshort));
        }  // passwords dont match
        else if ( !passEt.getText().toString().equals(confirmPassEt.getText().toString()) ){
            passEt.setError(getString(R.string.se_pass_notmatch));
        }
        else { // all good
            Credentials credentials = new Credentials.Builder(usernameET.getText().toString(),
                                            passEt.getText())
                                    .addEmail(emailEt.getText().toString())
                                    .addFirstName(firstEt.getText().toString())
                                    .addLastName(lastEt.getText().toString())
                                    .build();
            mListener.onRegistrationSubmitted(credentials);
        }
    }

    public void setError(String err, String errField) {
        if (errField.equals("email"))
            ((TextView) getView().findViewById(R.id.registerEditTextEmail))
                .setError(err);
        else if (errField.equals("username"))
            ((TextView) getView().findViewById(R.id.registerEditTextNickname))
                    .setError(err);
        else if (errField.equals(null))
            Toast.makeText(getActivity(),
                    err, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnRegistrationCompleteListener) {
            mListener = (OnRegistrationCompleteListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnRegistrationCompleteListener {
        void onRegistrationSubmitted(Credentials creds);
    }
}
