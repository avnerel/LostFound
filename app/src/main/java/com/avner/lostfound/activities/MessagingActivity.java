package com.avner.lostfound.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.avner.lostfound.Constants;
import com.avner.lostfound.LostFoundApplication;
import com.avner.lostfound.R;
import com.avner.lostfound.adapters.MessageAdapter;
import com.avner.lostfound.utils.IUIUpdateInterface;
import com.avner.lostfound.utils.SignalSystem;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class MessagingActivity extends Activity implements TextWatcher, View.OnClickListener, DialogInterface.OnClickListener, IUIUpdateInterface {
    private String recipientId;
    private EditText messageBodyField;
    private String currentUserId;
    private ListView messagesList;
    private MessageAdapter messageAdapter;
    private ImageButton sendButton;
    private String itemId;
    private MenuItem action_complete;
    private String recipientName;
    private AlertDialog completeConversationDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_messaging);

        //get recipientId from the intent
        Intent intent = getIntent();
        recipientId = intent.getStringExtra(Constants.Conversation.RECIPIENT_ID);
        itemId = intent.getStringExtra(Constants.Conversation.ITEM_ID);
        recipientName = intent.getStringExtra(Constants.Conversation.RECIPIENT_NAME);
        currentUserId = ParseUser.getCurrentUser().getObjectId();

        messageBodyField = (EditText) findViewById(R.id.messageBodyField);
        messageBodyField.addTextChangedListener(this);

        SignalSystem.getInstance().registerUIUpdateChange(this);
        initSendButton();
        initMessageList();

    }

    @Override
    protected void onStart() {
        super.onStart();
        updateCompleteConversation();
        ((LostFoundApplication)getApplication()).updateMessagingStatus(this, itemId, recipientId);
    }

    private void updateCompleteConversation() {

        ParseQuery<ParseObject> innerQuery = new ParseQuery(Constants.ParseObject.PARSE_LOST);
        innerQuery.fromLocalDatastore();
        innerQuery.whereEqualTo(Constants.ParseQuery.OBJECT_ID, itemId);

        ParseQuery<ParseObject> conversationQuery = new ParseQuery(Constants.ParseObject.PARSE_CONVERSATION);
        conversationQuery.fromLocalDatastore();
        conversationQuery.whereEqualTo(Constants.ParseConversation.MY_USER_ID, ParseUser.getCurrentUser().getObjectId());
        conversationQuery.whereEqualTo(Constants.ParseConversation.RECIPIENT_USER_ID, recipientId);
        conversationQuery.whereMatchesQuery(Constants.ParseConversation.ITEM, innerQuery);

        conversationQuery.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject conversationObject, ParseException e) {

                if (e != null || conversationObject == null) {
                    Log.e(Constants.LOST_FOUND_TAG, "couldn't retrieve conversation from local data store.");
                    return;
                }
                if (conversationObject.get(Constants.ParseConversation.SENT_COMPLETE) != true &&
                        ((ParseObject)conversationObject.get(Constants.ParseConversation.ITEM)).get(Constants.ParseReport.IS_ALIVE) == true) {

                    if (null != action_complete) {
                        action_complete.setVisible(true);
                    }
                }
                if (conversationObject.get(Constants.ParseConversation.RECEIVED_COMPLETE) == true) {
                    showCompleteConversationDialog();
                }

                if (conversationObject.get(Constants.ParseConversation.UNREAD_COUNT) != 0) {
                    conversationObject.put(Constants.ParseConversation.UNREAD_COUNT, 0);
                    conversationObject.pinInBackground();
                }
            }
        });
    }

    private void showCompleteConversationDialog() {

        if(completeConversationDialog == null){

            completeConversationDialog = new AlertDialog.Builder(this)
                    .setTitle("Complete Conversation request has been sent from " + recipientName)
                    .setMessage("Are you sure that the correct item was found?")
                    .setPositiveButton(R.string.yes, this)
                    .setNegativeButton(R.string.no, this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .create();
            completeConversationDialog.show();
        }
    }

    private void initMessageList() {
        messagesList = (ListView) findViewById(R.id.lv_messages_list);
        messageAdapter = new MessageAdapter(this);
        messagesList.setAdapter(messageAdapter);

        updateMessages();
    }

    private void updateMessages() {
        String[] userIds = {currentUserId, recipientId};
        ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.ParseObject.PARSE_MESSAGE);
        query.whereContainedIn(Constants.ParseMessage.SENDER_ID, Arrays.asList(userIds));
        query.whereContainedIn(Constants.ParseMessage.RECIPIENT_ID, Arrays.asList(userIds));
        query.whereEqualTo(Constants.ParseMessage.ITEM_ID, itemId);
        query.orderByAscending(Constants.ParseMessage.CREATED_AT);
        query.fromLocalDatastore();
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> messageList, ParseException e) {
                if (e == null) {
                    messageAdapter.clear();
                    for (int i = 0; i < messageList.size(); i++) {
                        String message = messageList.get(i).get(Constants.ParseMessage.MESSAGE_TEXT).toString();
                        if (messageList.get(i).get(Constants.ParseMessage.SENDER_ID).toString().equals(currentUserId)) {
                            messageAdapter.addMessage(message, MessageAdapter.DIRECTION_OUTGOING);
                        } else {
                            messageAdapter.addMessage(message, MessageAdapter.DIRECTION_INCOMING);
                        }
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_messaging, menu);
        action_complete = menu.findItem(R.id.action_complete);
        getActionBar().setTitle(recipientName);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {

        if(item.equals(action_complete)){

            Log.d(Constants.LOST_FOUND_TAG, "user clicked on end conversation");
            action_complete.setVisible(false);
            Toast.makeText(this, "Complete Conversation has been sent. Awaiting response", Toast.LENGTH_SHORT).show();

            HashMap<String, Object> params = new HashMap();
            params.put(Constants.ParseMessage.RECIPIENT_ID, recipientId);
            params.put(Constants.ParseMessage.ITEM_ID, itemId);
            params.put("senderName", ParseUser.getCurrentUser().get(Constants.ParseUser.USER_DISPLAY_NAME));
            params.put("recipientName", recipientName);

            ParseCloud.callFunctionInBackground(Constants.ParseCloudMethods.COMPLETE_CONVERSATION_REQUEST, params);
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void initSendButton() {

        sendButton = (ImageButton) findViewById(R.id.sendButton);
        //listen for a click on the send button
        sendButton.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {

        if(view.getId() == sendButton.getId()){

            ConnectivityManager cm =
                    (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnected();

            if(isConnected){
                String messageBody = messageBodyField.getText().toString();
//                messageService.sendMessage(recipientId, messageBody,itemId);
                sendMessage(messageBody);
                //reset message
                messageBodyField.setText("");
            }else{
                Log.d(Constants.LOST_FOUND_TAG, "tried to send message but no connection.");
                Toast.makeText(this, "Message not sent, Please check your connection", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendMessage(String messageBody) {

//        final WritableMessage writableMessage = new WritableMessage(recipientId, messageBody);

        final ParseObject parseMessage = new ParseObject(Constants.ParseObject.PARSE_MESSAGE);
        parseMessage.put(Constants.ParseMessage.SENDER_ID, currentUserId);
        parseMessage.put(Constants.ParseMessage.RECIPIENT_ID, recipientId);
        parseMessage.put(Constants.ParseMessage.MESSAGE_TEXT, messageBody);
        parseMessage.put(Constants.ParseMessage.ITEM_ID, itemId);
        parseMessage.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e== null){
//                    parseMessage.pinInBackground();
                }else{
                    Toast.makeText(MessagingActivity.this, "Couldn't send message to " + recipientName + ", please check your connection", Toast.LENGTH_SHORT).show();
                    // update UI
                    Intent intent = new Intent();
                    intent.putExtra(Constants.ParseMessage.RECIPIENT_ID, recipientId);
                    intent.putExtra(Constants.ParseMessage.ITEM_ID, itemId);
                    SignalSystem.getInstance().fireUpdateChange(Constants.UIActions.uiaMessageSaved, false, intent);
                }
            }
        });

        messageAdapter.addMessage(messageBody, MessageAdapter.DIRECTION_OUTGOING);
    }

    @Override
    protected void onStop() {
        ((LostFoundApplication)getApplication()).updateMessagingStatus(null, null, null);
        super.onStop();
    }

    //unbind the service when the activity is destroyed
    @Override
    public void onDestroy() {
        SignalSystem.getInstance().unRegisterUIUpdateChange(this);
        clearUnreadCount();
        super.onDestroy();
    }

    private void clearUnreadCount() {

        ParseQuery<ParseObject> innerQuery = new ParseQuery(Constants.ParseObject.PARSE_LOST);
        innerQuery.fromLocalDatastore();
        innerQuery.whereEqualTo(Constants.ParseQuery.OBJECT_ID, itemId);

        ParseQuery<ParseObject> conversationQuery = new ParseQuery(Constants.ParseObject.PARSE_CONVERSATION);
        conversationQuery.fromLocalDatastore();
        conversationQuery.whereEqualTo(Constants.ParseConversation.MY_USER_ID, ParseUser.getCurrentUser().getObjectId());
        conversationQuery.whereEqualTo(Constants.ParseConversation.RECIPIENT_USER_ID, recipientId);
        conversationQuery.whereMatchesQuery(Constants.ParseConversation.ITEM, innerQuery);

        conversationQuery.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject conversationObject, ParseException e) {

                if (e != null || conversationObject == null) {
                    Log.e(Constants.LOST_FOUND_TAG, "couldn't retrieve conversation from local data store.");
                    return;
                }
                Log.d(Constants.LOST_FOUND_TAG, "retrieved conversation from local data store and resetting unread count.");
                if (conversationObject.get(Constants.ParseConversation.UNREAD_COUNT) != 0) {
                    Log.d(Constants.LOST_FOUND_TAG, "conversation unread count has to be reset.");

                    conversationObject.put(Constants.ParseConversation.UNREAD_COUNT, 0);
                    conversationObject.pinInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            Log.d(Constants.LOST_FOUND_TAG, "conversation unread count was reset.");
                            SignalSystem.getInstance().fireUpdateChange(Constants.UIActions.uiaConversationSaved);
                        }
                    });
                }
            }
        });

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

        if(s.toString().isEmpty()){

            sendButton.setVisibility(ImageButton.INVISIBLE);
        }else{
            sendButton.setVisibility(ImageButton.VISIBLE);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {}

    @Override
    public void onClick(DialogInterface dialog, int which) {

        boolean positiveReply = DialogInterface.BUTTON_POSITIVE == which;
        Log.d(Constants.LOST_FOUND_TAG, "sent reply for complete conversation: " + positiveReply + ". which: " + which);

        completeConversationDialog = null;
        // hide complete button.
        if(positiveReply){
            action_complete.setVisible(false);
        }
        HashMap<String, Object> params = new HashMap();
        params.put(Constants.ParseMessage.RECIPIENT_ID, recipientId);
        params.put(Constants.ParseMessage.ITEM_ID, itemId);
        params.put("senderName", ParseUser.getCurrentUser().get(Constants.ParseUser.USER_DISPLAY_NAME));
        params.put("reply", positiveReply);
        params.put("recipientName", recipientName);

        ParseCloud.callFunctionInBackground(Constants.ParseCloudMethods.COMPLETE_CONVERSATION_REPLY, params);
    }

    @Override
    public void onDataChange(Constants.UIActions action, boolean bSuccess, Intent data) {

        switch (action){

            case uiaMessageSaved:

                String senderId = data.getStringExtra(Constants.ParseMessage.RECIPIENT_ID);
                String itemId = data.getStringExtra(Constants.ParseMessage.ITEM_ID);
                if((senderId.equals(this.recipientId) || senderId.equals(currentUserId)) && itemId.equals(this.itemId)){
                    updateMessages();
                }
                break;

            case uiaConversationSaved:
                updateCompleteConversation();
                break;

            case uiaCompleteConversationSent:
                senderId = data.getStringExtra(Constants.ParseMessage.RECIPIENT_ID);
                itemId = data.getStringExtra(Constants.ParseMessage.ITEM_ID);
                if(senderId.equals(this.recipientId) && itemId.equals(this.itemId)){
                    showCompleteConversationDialog();
                }
                break;
        }

    }


}