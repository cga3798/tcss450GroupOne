package group1.tcss450.uw.edu.a450groupone;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import group1.tcss450.uw.edu.a450groupone.utils.Weather;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link WeatherFragment.OnWeatherFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class WeatherFragment extends Fragment implements View.OnClickListener {

    private static final float TEXT_VIEW_WEIGHT = .5f;
    private static final int TOTAL_HOURS_TO_DISPLAY = 24;


    private OnWeatherFragmentInteractionListener mListener;

    private Bundle data;


    public WeatherFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("WEATHER", "now in weather fragment");
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_weather, container, false);

        FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        fab.setVisibility(View.GONE);

        //Bundle args = new Bundle();
        FloatingActionButton b = (FloatingActionButton) v.findViewById(R.id.selectCityFloatingButton);
        b.setOnClickListener(this::onSelectCClicked);

        // listener of current5 location
        v.findViewById(R.id.weatherCurrentLocationButton).setOnClickListener(view -> getWeatherData(v, true));

        SharedPreferences prefs = getActivity().getSharedPreferences(
                            getString(R.string.keys_shared_prefs),
                            Context.MODE_PRIVATE);

        boolean wentToSelectCity = prefs.getBoolean(getString(R.string.keys_prefs_selected_city), false);
        if (wentToSelectCity) {
            prefs.edit().putBoolean(getString(R.string.keys_prefs_selected_city), false).apply();
            getWeatherFromSelect(v);
        } else { // came from home or side bar
            getWeatherData(v, false);
        }

        return v;
    }

    private void getWeatherData(View fragmentView, boolean wantCurrentLocation) {

        Weather.RetrieveData asyncTask = new Weather.RetrieveData(getContext(), R.id.fragmentWeather ,new Weather.AsyncResponse() {
            public void processFinish(Bundle args) {
                data = args;
                setWeatherData(fragmentView);
            }
        });

        SharedPreferences prefs = getActivity().getSharedPreferences(
                getString(R.string.keys_shared_prefs),
                Context.MODE_PRIVATE);

        if(wantCurrentLocation) {
            Location currentLocation = null;

            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                currentLocation = LocationServices.FusedLocationApi.getLastLocation(
                        ((NavigationActivity) getActivity()).getmGoogleApiClient());
            }

            if (currentLocation != null) {
                // use current location
                asyncTask.execute(String.valueOf(currentLocation.getLatitude()),
                        String.valueOf(currentLocation.getLongitude()));
                prefs.edit().putString(getString(R.string.keys_prefs_selected_city_lat),
                        String.valueOf(currentLocation.getLatitude())).apply();
                prefs.edit().putString(getString(R.string.keys_prefs_selected_city_lon),
                        String.valueOf(currentLocation.getLongitude())).apply();
            }

        } else {

            String lat = prefs.getString(getString(R.string.keys_prefs_selected_city_lat), "_");
            String lon = prefs.getString(getString(R.string.keys_prefs_selected_city_lon), "_");

            if (lat.charAt(0) == '_') {
                // must be first time opening app
                // use Tacoma as default
                asyncTask.execute("47.25288", "-122.44429");
                prefs.edit().putString(getString(R.string.keys_prefs_selected_city_lat),
                                    "47.25288").apply();
                prefs.edit().putString(getString(R.string.keys_prefs_selected_city_lon),
                                    "-122.44429").apply();

            } else { // use last selected in prefs
                asyncTask.execute(lat, lon);
            }
        }
    }

    private void getWeatherFromSelect(View fragmentView) {
        Weather.RetrieveData asyncTask = new Weather.RetrieveData(getContext(), R.id.selectCityFragment ,new Weather.AsyncResponse() {
            public void processFinish(Bundle args) {
                data = args;
                // TODO: save city in prefs
                if (data.getBoolean(Weather.K_FOUND)) {
                    saveCity();
                    setWeatherData(fragmentView);
                } else {
                    TextView tv = (TextView) fragmentView.findViewById(R.id.weatherCityTextview);
                    tv.setText("Data not found! \n Try searching again!");
                }
            }
        });
        SharedPreferences prefs = getActivity().getSharedPreferences(
                getString(R.string.keys_shared_prefs),
                Context.MODE_PRIVATE);

        String zip = prefs
                .getString(getString(R.string.keys_prefs_selected_zip), "_");
        String lat = prefs.getString(getString(R.string.keys_prefs_selected_city_lat), "_");
        String lon = prefs.getString(getString(R.string.keys_prefs_selected_city_lon), "_");

        if (zip.charAt(0) == '_') {
            asyncTask.execute(lat, lon);
        } else { // entered zip
            asyncTask.execute("_", zip);
        }
        // clear zip in prefs.. just replace by '_'
        prefs.edit().putString(getString(R.string.keys_prefs_selected_zip), "_");

    }

    private void saveCity() {
        SharedPreferences prefs = getActivity().getSharedPreferences(
                getString(R.string.keys_shared_prefs),
                Context.MODE_PRIVATE);
        try {
            JSONArray preferedCities = new JSONArray(
                    prefs.getString(
                            getString(R.string.keys_prefs_fave_cities), "[]"));

            Log.d("ADDINgCITY", " current array = " + preferedCities.toString());
            String cityToAddName = data.getString(Weather.K_CITY);
            Log.d("CHECKING city ", "city is  " + cityToAddName);

            boolean found = false;
            // search if city exits in array
            for (int i = 0; i < preferedCities.length(); i++) {
                if (preferedCities.getJSONObject(i).getString(Weather.K_CITY)
                        .equals(cityToAddName)) {
                    found = true;
                }
            }

            if (!found) {
                // make city object
                JSONObject city = new JSONObject();
                city.put(Weather.K_CITY, data.getString(Weather.K_CITY));
                city.put(Weather.K_LAT, data.getString(Weather.K_LAT));
                city.put(Weather.K_LON, data.getString(Weather.K_LON));
                // add to array
                preferedCities.put(city);
                //save in prefs
                prefs.edit().putString(getString(R.string.keys_prefs_fave_cities),
                                    preferedCities.toString()).apply();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setWeatherData(View v) {
        Log.d("WEATHER_FRAG", "setting data");
        makeTopWeatherData(v);
        makeHourlyScrollView(v);
        makeDailyScrollView(v);
        makeBottomWeatherData(v);
    }

    private void makeTopWeatherData(View v) {
        Typeface weatherFont = Typeface.createFromAsset(getContext().getAssets(), Weather.FONT_PATH);

        TextView city = (TextView) v.findViewById(R.id.weatherCityTextview);
        TextView weather = (TextView) v.findViewById(R.id.weatherDesc);
        TextView currentTemp = (TextView) v.findViewById(R.id.weatherTemp);
        TextView weatherIcon = (TextView) v.findViewById(R.id.weatherIcon);
        weatherIcon.setTypeface(weatherFont);
        TextView today = (TextView) v.findViewById(R.id.weatherToday);
        TextView maxmin = (TextView) v.findViewById(R.id.weatherTodayMaxMinTemp);

        city.setText(data.getString(Weather.K_CITY));
        weather.setText(data.getString(Weather.K_WEATHER_DESC));
        currentTemp.setText(data.getString(Weather.K_CURRENT_TEMP));
        weatherIcon.setText(Html.fromHtml(data.getString(Weather.K_ICON)));
        today.setText(data.getString(Weather.K_UPDATEDON) + " Today");
        maxmin.setText(data.getString(Weather.K_MAX_TEMP) + "             "
                    + data.getString(Weather.K_MIN_TEMP));

    }

    private void makeHourlyScrollView(View v) {
        LinearLayout hourlyBar = (LinearLayout) v.findViewById(R.id.weatherHourlyBar);
        JSONArray hourlyList = null;
        try {
            hourlyList = new JSONArray(data.getString(Weather.K_HOURLY_DAILY_LIST));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("ha");
        sdf.setTimeZone(TimeZone.getTimeZone(Weather.GMT_PACIFIC));

        for (int i = 0; i < TOTAL_HOURS_TO_DISPLAY; i++) {
            try {
                JSONObject hourJson = hourlyList.getJSONObject(i);
                hourlyBar.addView(makeHourContainer(hourJson, sdf));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private View makeHourContainer(JSONObject hourJson, SimpleDateFormat sdf) throws JSONException {

        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.hour_weather_box, null, false);

        JSONObject weather = hourJson.getJSONArray("weather").getJSONObject(0);
        JSONObject main = hourJson.getJSONObject("main");

        Typeface weatherFont = Typeface.createFromAsset(getContext().getAssets(), Weather.FONT_PATH);

        // fill data
        TextView tv = (TextView) v.findViewById(R.id.weatherTextViewTime);

        tv.setText(sdf.format(new Date(hourJson.getLong("dt") * 1000)));
        tv = (TextView) v.findViewById(R.id.weatherTextViewIcon);
        tv.setTypeface(weatherFont);
        tv.setText(Html.fromHtml(
                Weather.setWeatherIcon(
                        weather.getInt("id"),
                        data.getLong(Weather.K_SUNRISE_LONG),
                        data.getLong(Weather.K_SUNSET_LONG))
        ));
        tv = (TextView) v.findViewById(R.id.weatherTextViewTemp);
        tv.setText(String.valueOf(main.getInt("temp")));

        return v;
    }

    private void makeDailyScrollView(View v) {
        TableLayout table = (TableLayout) v.findViewById(R.id.weatherDailyTable);
        ArrayList<Bundle> days = new ArrayList<>();

        try {
            //final ArrayList<Bundle> days = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("dd");
            sdf.setTimeZone(TimeZone.getTimeZone(Weather.GMT_PACIFIC));
            DateFormat df = new SimpleDateFormat("EEEE");
            df.setTimeZone(TimeZone.getTimeZone(Weather.GMT_PACIFIC));
            JSONArray dailyList = new JSONArray(data.getString(Weather.K_HOURLY_DAILY_LIST));
            processHoursJSON(dailyList, days, sdf, df);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        for (Bundle b : days) {
            table.addView(makeDayRow(b));
        }

    }

    private void processHoursJSON(JSONArray hoursData, ArrayList<Bundle> days, DateFormat sdf, DateFormat df) throws JSONException {

        //ArrayList<Bundle> days = new ArrayList<>();
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        int sumIcon = 0;
        int count = 0;

        for (int i = 1; i < hoursData.length(); i++) {

            JSONObject hourJson = hoursData.getJSONObject(i);
            JSONObject prevHourJson = hoursData.getJSONObject(i-1);

            JSONObject main = hourJson.getJSONObject("main");
            JSONObject weather = hourJson.getJSONArray("weather").getJSONObject(0);
            String currDay = sdf.format(new Date(hourJson.getLong("dt") * 1000));
            String prevDay = sdf.format(new Date(prevHourJson.getLong("dt") * 1000));

            if (Integer.valueOf(currDay) != Integer.valueOf(prevDay) ) {
                // store vals
                String dayOfWeek = df.format(new Date(prevHourJson.getLong("dt") * 1000));

                // if day is not the same as today dont make row
                // there might not be enough data to calculate min-max temp
                if( ! dayOfWeek.equals(data.getString(Weather.K_UPDATEDON))) {

                    int icon = 8;
                    if (count > 0) { // data for day not available
                        icon = sumIcon / count;
                        Bundle b = new Bundle();
                        b.putString(Weather.K_DAY_OF_WEEK, dayOfWeek);
                        b.putInt(Weather.K_ICON, icon);
                        b.putString(Weather.K_MAX_TEMP, String.valueOf(max));
                        b.putString(Weather.K_MIN_TEMP, String.valueOf(min));
                        days.add(b);
                    } else { // say data not available
                        Bundle b = new Bundle();
                        b.putString(Weather.K_DAY_OF_WEEK, dayOfWeek);
                        b.putInt(Weather.K_ICON, icon);
                        b.putString(Weather.K_MAX_TEMP, getString(R.string.data_not_available));
                        b.putString(Weather.K_MIN_TEMP, "");
                        days.add(b);
                    }
                }
                // reset for new day
                max = Integer.MIN_VALUE;
                min = Integer.MAX_VALUE;
                sumIcon = 0;
                count = 0;
            }
            max = (main.getInt("temp_max") > max) ? main.getInt("temp_max") : max;
            min = (main.getInt("temp_min") < min) ? main.getInt("temp_min") : min;

            sumIcon += weather.getInt("id");
            count++;
        }
    }

    private View makeDayRow(Bundle b) {//String day, int icon, String maxtemp, String minTemp) {
        //TableRow tr = new TableRow(getContext());
        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.day_weather_row, null, false);

        Typeface weatherFont = Typeface.createFromAsset(getContext().getAssets(), Weather.FONT_PATH);

        // fill values
        TextView cell = (TextView) v.findViewById(R.id.weatherTextViewRowDay);
        cell.setText(b.getString(Weather.K_DAY_OF_WEEK));
        cell = (TextView) v.findViewById(R.id.weatherTextViewRowIcon);
        cell.setTypeface(weatherFont);
        cell.setText(Html.fromHtml(
                Weather.setWeatherIcon(
                        b.getInt(Weather.K_ICON),0,0)

        ));
        cell = (TextView) v.findViewById(R.id.weatherTextViewRowMaxTemp);
        cell.setText(b.getString(Weather.K_MAX_TEMP));
        cell = (TextView) v.findViewById(R.id.weatherTextViewRowMinTemp);
        cell.setText(b.getString(Weather.K_MIN_TEMP));
        return v;
    }


    private void makeBottomWeatherData(View v) {
        TableLayout table = (TableLayout) v.findViewById(R.id.weatherDailyTable);
        // header
        TableRow aRow = getRowWithStyle();
        aRow.addView(makeTextView(getString(R.string.sunrise), true));
        aRow.addView(makeTextView(getString(R.string.sunset), true));
        table.addView(aRow);
        //data
        aRow = getRowWithStyle();
        aRow.addView(makeTextView(data.getString(Weather.K_SUNRISE), false));
        aRow.addView(makeTextView(data.getString(Weather.K_SUNSET), false));
        table.addView(aRow);

        // header
        aRow = getRowWithStyle();
        aRow.addView(makeTextView(getString(R.string.wind), true));
        aRow.addView(makeTextView(getString(R.string.humidity), true));
        table.addView(aRow);
        //data
        aRow = getRowWithStyle();
        String windInfo = data.getString(Weather.K_WIND_DIR)
                        + "  " + data.getString(Weather.K_WIND_SPEED);
        aRow.addView(makeTextView(windInfo,false));
        aRow.addView(makeTextView(data.getString(Weather.K_HUMIDITY), false));
        table.addView(aRow);

    }

    private TextView makeTextView(String text, boolean header) {
        TextView tv = new TextView(this.getContext());
        tv.setText(text);
        tv.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT,
                TEXT_VIEW_WEIGHT
        ));

        if (header) {
            tv.setTextSize(getResources().getDimensionPixelSize(R.dimen.weather_header_text_size));
        } else {
            tv.setTextSize(getResources().getDimensionPixelSize(R.dimen.weather_data_text_size));
        }

        return tv;
    }

    private TableRow getRowWithStyle() {
        TableRow row = new TableRow(this.getContext());
        row.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));

        int dp = getResources().getDimensionPixelSize(R.dimen.row_padding);
        row.setPadding(dp, dp, dp, dp);

        return row;
    }


    public void onSelectCClicked(View v) {
        mListener.onSelectCityButtonClicked();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnWeatherFragmentInteractionListener) {
            mListener = (OnWeatherFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnWeatherFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        mListener.onSelectCityButtonClicked();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnWeatherFragmentInteractionListener {
        void onSelectCityButtonClicked();
        void onMapButtonClicked();
    }
}
