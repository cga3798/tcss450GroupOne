package group1.tcss450.uw.edu.a450groupone;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ScrollView;

import org.json.JSONException;
import org.json.JSONObject;

import group1.tcss450.uw.edu.a450groupone.utils.SendPostAsyncTask;

public class ChatActivity extends AppCompatActivity implements ChatFragment.OnChatFragmentInteractionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat1);
        if(savedInstanceState == null) {
            if (findViewById(R.id.chatContainer) != null) {

                int sourceFragment = getIntent().getIntExtra(getString(R.string.keys_open_chat_source), 0);

                // opening chat from clicking on a friend in friends fragment
                if (sourceFragment == R.id.fragmentFriend) {
                    checkFriendHasExistingChat();
                } // opening chat from clicking a chat in home fragment
                else if (sourceFragment == R.id.fragmentHome) {
                    getSupportFragmentManager().beginTransaction()
                            .add(R.id.chatContainer, new ChatFragment())
                            .commit();
                } else {
                    throw new IllegalArgumentException("Invalid chat access! Should not happen");
                }
            }
        }

    }

    // check if user has existing chat with friend or makes new one in backend.
    private void checkFriendHasExistingChat() {
        int friendId = getIntent().getIntExtra(getString(R.string.keys_friend_id), 0);
        String friendFullName = getIntent().getStringExtra(getString(R.string.keys_friend_full_name));

        SharedPreferences prefs = getSharedPreferences(
                getString(R.string.keys_shared_prefs),
                Context.MODE_PRIVATE);
        int myId = prefs.getInt(getString(R.string.keys_prefs_id), 0);
        String myFullName = prefs.getString(getString(R.string.keys_prefs_first_name), "")
                    + " " + prefs.getString(getString(R.string.keys_prefs_last_name), "");

        if (friendId > 0) { // valid id
            // make async task to get chats
            Uri uri = new Uri.Builder()
                    .scheme("https")
                    .appendPath(getString(R.string.ep_base_url))
                    .appendPath(getString(R.string.ep_chats))
                    .appendPath(getString(R.string.ep_chats_get_chat))
                    .build();

            JSONObject body = new JSONObject();
            // provide current user id
            try {
                body.put("friendid", friendId);
                body.put("friendfullname", friendFullName);
                body.put("myid", myId);
                body.put("myfullname", myFullName);
            } catch (JSONException e){
                e.printStackTrace();
            }

            new SendPostAsyncTask.Builder(uri.toString(), body)
                    .onPostExecute(this::openChat)
                    .onCancelled(this::handleError)
                    .build().execute();

        } else {
            throw new IllegalArgumentException("Expecting friend id to start chat!");
        }

    }

    // retrieve existing chat
    private void openChat(String response) {
        try {
            JSONObject res = new JSONObject(response);

            if (res.getBoolean("success")) {
                int chatId;
                // we have chatid in response -> created new chat
                /* example response
                {
                        success:true,
                        chatid: 12,
                        chatname: "friend fullname_my fullname"
                    }
                 */
                if (res.has("chatid")) {
                    chatId = res.getInt("chatid");

                } // look in chats -> found one chat
                /* example response
                {
                    "success": true,
                    "chats": [
                        {
                            "chatid": 12,
                            "name": "friend fullname_my fullname"
                        }
                    ]
                  }
                 */
                else {
                    // use the first match for now
                    chatId = res.getJSONArray("chats")
                            .getJSONObject(0).getInt("chatid");
                }

                openChatFragment(chatId);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void openChatFragment(int chatId) {
        // put chatid to open in prefs
        getSharedPreferences(
                getString(R.string.keys_shared_prefs),
                Context.MODE_PRIVATE)
                .edit()
                .putInt(getString(R.string.keys_prefs_chatId), chatId)
                .apply();
        // open fragment
        getSupportFragmentManager().beginTransaction()
                .add(R.id.chatContainer, new ChatFragment())
                .commit();
    }

    private void handleError(final String msg) {
        Log.e("CHAT_ERROR", msg.toString());
    }

    @Override
    public void onNewChat() {

    }

    @Override
    public void onOpenChat() {

    }
}
