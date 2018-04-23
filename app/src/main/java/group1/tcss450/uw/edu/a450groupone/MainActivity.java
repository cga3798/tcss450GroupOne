package group1.tcss450.uw.edu.a450groupone;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.support.v7.widget.RecyclerView;


public class MainActivity extends AppCompatActivity implements WeatherFragment.OnWeatherFragmentInteractionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(savedInstanceState == null) {
            if (findViewById(R.id.fragmentContainer) != null) {
                getSupportFragmentManager().beginTransaction()
                        // Uncommment other line to run other fragment
                        .add(R.id.fragmentContainer, new WeatherFragment())
                        //.add(R.id.fragmentContainer, new SuccessRegistrationFragment())
                        .commit();
            }
        }
    }

    @Override
    public void onFragmentInteraction() {

    }
}
