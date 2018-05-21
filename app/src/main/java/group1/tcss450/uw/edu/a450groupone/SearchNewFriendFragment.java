package group1.tcss450.uw.edu.a450groupone;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import es.dmoral.toasty.Toasty;
import group1.tcss450.uw.edu.a450groupone.utils.ListViewAdapter;
import group1.tcss450.uw.edu.a450groupone.utils.SendPostAsyncTask;

/**
 * A fragment that will handle searching for a new connection and
 * sending a request to them.
 */
public class SearchNewFriendFragment extends Fragment implements SearchView.OnQueryTextListener{

    private ListView list;
    private ListViewAdapter adapter;
    private SearchView searchView;
    private String queryEntered;
    private boolean showInvite = true;
    private String memberidA, fullname;
    public static ArrayList<String> connectionResultList, memberIds, fullnames;

    public SearchNewFriendFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_search_new_friend, container, false);
        SharedPreferences prefs =
                this.getActivity().getSharedPreferences(getString(R.string.keys_shared_prefs),
                        Context.MODE_PRIVATE);
        memberidA =  String.valueOf(prefs.getInt(getString(R.string.keys_prefs_id), -1));

        searchView = (SearchView) v.findViewById(R.id.searchFriendSearchView);
        searchView.setActivated(true);
        searchView.setOnQueryTextListener(this);


        return v;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        onSearch(query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    /**
     * Establish connection to web services to search users.
     * @param query what the user types in the search view.
     */
    private void onSearch(String query) {
        Uri uri = new Uri.Builder()
                .scheme("https")
                .appendPath(getString(R.string.ep_base_url))
                .appendPath(getString(R.string.ep_search_app_users))
                .build();

        JSONObject searchJSON = asJSONObject(query);

        queryEntered = query;
        new SendPostAsyncTask.Builder(uri.toString(), searchJSON)
                .onPostExecute(this::handleSearchOnPost)
                //TODO: add onCancelled handler.
                .onCancelled(this::handleErrorsInTask)
                .build().execute();
    }

    /**
     * Handle any errors that might occur when calling the server
     * @param result response from server.
     */
    private void handleErrorsInTask(String result) {
        Log.e("ASYNCT_TASK_ERROR", result);
    }

    /**
     * Handles the search result that is returned from the server, parse it and
     * add it to a list view to be displayed on the screen.
     *
     * @param result response from server.
     */
    private void handleSearchOnPost(String result) {
        try {
            //results
            JSONObject resultsJSON = new JSONObject(result);
            boolean success = resultsJSON.getBoolean("success");

            if (success) {
                list = (ListView) getActivity().findViewById(R.id.searchFriendListView);

                int length = resultsJSON.getJSONArray("names").length();
                memberIds = new ArrayList<>();
                connectionResultList = new ArrayList<>();
                fullnames = new ArrayList<>();

                if (length < 1) {
                    showInvite = false;
                    connectionResultList.add("\nNo results found for \'" + queryEntered + " \' ");

                } else {
                    showInvite = true;
                    for (int i = 0; i < length; i++) {
                        String first = resultsJSON.getJSONArray("names")
                                .getJSONObject(i).getString("firstname");
                        String last = resultsJSON.getJSONArray("names")
                                .getJSONObject(i).getString("lastname");
                        String user = resultsJSON.getJSONArray("names")
                                .getJSONObject(i).getString("username");

                        memberIds.add(resultsJSON.getJSONArray("names")
                                .getJSONObject(i).getString("memberid"));

                        //array that will be displayed in search result
                        connectionResultList.add(first + " " + last + "\n" + "Username: " + user);
                        fullnames.add(first + " " + last);

                    }
                }

                adapter = new ListViewAdapter(getActivity(), "search");
                list.setAdapter(adapter);

                list.setOnItemClickListener((parent, view, position, id) -> onClickOnSearchResult(position));

            } else {
                Toast.makeText(getActivity(),
                        "Search unsuccessful. Please try again", Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException e) {
            Log.e("JSON_PARSE_ERROR", result
                    + System.lineSeparator()
                    + e.getMessage());
        }
    }

    /**
     * Handles the click on the search results.
     *
     * @param position the position of the item that is being clicked.
     */
    public void onClickOnSearchResult(int position) {
        if (showInvite) {
            onSearchResults(memberIds.get(position), fullnames.get(position));
        }
    }

    /**
     * Send a request to the server to search for the query typed in when the
     * choose to send them an invitation.
     *
     * @param memberidB member ID of the user to invite
     * @param fullnameB
     */
    private void onSearchResults(String memberidB, String fullnameB) {

        fullname = fullnameB;
        Uri uri = new Uri.Builder()
                .scheme("https")
                .appendPath(getString(R.string.ep_base_url))
                .appendPath(getString(R.string.ep_invite_new_friend))
                .build();

        JSONObject searchJSON = asJSONObject(memberidA, memberidB);
        new SendPostAsyncTask.Builder(uri.toString(), searchJSON)
                .onPostExecute(this::handleInviteOnPost)
                //TODO: add onCancelled handler.
                .onCancelled(this::handleErrorsInTask)
                .build().execute();
    }


    private void handleInviteOnPost(String result) {
        Log.d("invite result", result);

        Map<Integer, String> messages = new HashMap<Integer, String>();
        messages.put(1, "An invitation to " + fullname + " has been sent!");
        //TODO: Go to new chat with friend when 2.
        messages.put(2, "You and " + fullname + " are already friends. Please check your friend list");
        messages.put(3, fullname + " has already sent you an invitation. Please check your invitations");
        messages.put(4, "You cannot add yourself as a friend"); // TODO: change this so you don't appear in search.
        messages.put(5, "You have already sent an invitation to " + fullname + ".");

        try {
            JSONObject resultsJSON = new JSONObject(result);
            boolean success = resultsJSON.getBoolean("success");
            if (success) {
                int msg = resultsJSON.getInt("message");
                if (msg == 1) {
                    for (int i=0; i < 1; i++) {
                        Toasty.success(getActivity(), messages.get(1), Toast.LENGTH_LONG, true).show();
                    }
                } else {
                    for (int i=0; i < 1; i++)
                        Toasty.normal(getActivity(), messages.get(msg), Toast.LENGTH_LONG).show();
                }
            } else {
                // Not success.
            }
        } catch (JSONException e) {
            Log.e("JSON_PARSE_ERROR", result
                    + System.lineSeparator()
                    + e.getMessage());
        }
    }


    private JSONObject asJSONObject(String query) {
        //build the JSONObject
        JSONObject msg = new JSONObject();

        query = query.trim();
        //TODO: DON'T SEND CURRENT USER INFO. CAN'T SEE SELF IN RESULT.
        try {
            if (query.contains("@") ) {
                msg.put("email", query);
            } else if (query.contains(" ")){
                String[] fullname = query.split("\\s+");
                msg.put("first", fullname[0]);
                msg.put("last", fullname[1]);
            }  else {
                msg.put("username", query);
            }

        } catch (JSONException e) {
            Log.wtf("QUERY ", "Error creating JSON: " + e.getMessage());
        }
        return msg;
    }

    private JSONObject asJSONObject(String memberidA, String memberidB) {
        //build the JSONObject
        JSONObject msg = new JSONObject();
        try {
            msg.put("memberidA", memberidA);
            msg.put("memberidB", memberidB);
        } catch (JSONException e) {
            Log.wtf("QUERY ", "Error creating JSON: " + e.getMessage());
        }
        return msg;
    }
}
