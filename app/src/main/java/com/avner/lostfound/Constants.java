package com.avner.lostfound;

import android.os.Environment;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by avner on 26/04/2015.
 * This class holds all common constants used by different components.
 */
public class Constants {

    public static final int REQUEST_CODE_SIGN_UP = 1;
    public static final int REQUEST_CODE_FACEBOOK_LOGIN = 3;
    public static final int REQUEST_CODE_PICK_LOCATION = 4;
    public static final int REQUEST_CODE_REPORT_FORM = 5;
    public static final int REQUEST_CODE_SETTINGS = 6;
    public static final int REQUEST_CODE_CAMERA = 10;
    public static final int REQUEST_CODE_SELECT_FILE = 11;

    public static final int RESULT_CODE_LOGOUT = 1;

    public static final String signed_up = "signed_up";
    public static final String USER_NAME= "user_name";
    public static final String PASSWORD = "password";

    public static final java.lang.String LONGITUDE = "longitude";
    public static final java.lang.String LATITUDE = "latitude";
    public static final String APP_IMAGE_DIRECTORY_NAME = "/lostfound";

    public static final String USER_IMAGE_FILE_NAME = "userImage.png";

    public static final String USER_IMAGE_FILE_PATH = Environment.getExternalStorageDirectory()
            + Constants.APP_IMAGE_DIRECTORY_NAME + "/" + Constants.USER_IMAGE_FILE_NAME;
    /**
     * tag for logger.
     */
    public static final String LOST_FOUND_TAG = "LOST_FOUND_TAG";
    /**
     * for lists filtering
     */
    public static final long NO_DISTANCE_FILTER = -1;
    public static final long NO_TIME_FILTER = -1;
    public static final String NO_CONTENT_FILTER = "";
    public static final long MILLI_SECONDS_PER_DAY = 1000* 60 * 60 * 24;
    public static final Map<String, Long> daysFactor = initDaysFactorMap();
    public static final int MIN_CONTENT_FILTER_SIZE = 2;
    public static final String FOUND_SHORTCUT = "F";
    public static final String LOST_SHORTCUT = "L";
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_PASSWORD_LENGTH = 12;
    public static final int LOST_ITEM_IMAGE = R.drawable.lost_item;
    public static final int FOUND_ITEM_IMAGE = R.drawable.found_item;
    public static final String POSSIBLE_MATCHES = "PossibleMatches";
    public static final int MIN_DESCRIPTION_LENGTH = 10;


    public class TabTexts {
        public static final String FOUND = "Found";
        public static final String LOST = "Lost";
        public static final String MY_WORLD = "My World";
        public static final String STATS = "Stats";
    }




    private static Map<String, Long> initDaysFactorMap() {
        Map<String, Long> map = new HashMap<>();

        map.put("today", 1L);
        map.put("week", 7L);
        map.put("month", 30L);
        map.put("year", 365L);

        return map;
    }

    public class ParseReport{
        public static final String updatedAt = "updatedAt";
        public static final String objectId = "objectId";
        public static final String ITEM_NAME = "itemName";
        public static final String ITEM_DESCRIPTION = "itemDescription";
        public static final String TIME = "time";
        public static final String LOCATION = "location";
        public static final String LOCATION_STRING = "locationString";
        public static final String USER_ID = "userId";
        public static final String ITEM_IMAGE = "itemImage";
        public static final String USER_DISPLAY_NAME = "name";
        public static final String IS_LOST = "isLost";
        public static final String IS_ALIVE = "alive";
        public static final String POSSIBLE_MATCHES = "possibleMatches";
    }

    public class ParseQuery{
        public static final String CREATED_AT = "createdAt";
        public static final String OBJECT_ID = "objectId";
    }

    public class ParseObject{
        public static final String PARSE_LOST = "ParseLost";
        public static final String PARSE_CONVERSATION= "ParseConversation";
        public static final String PARSE_MESSAGE= "ParseMessage";
    }

    public class ParseUser{
        public static final String USER_DISPLAY_NAME = "name";
    }

    public class Conversation{
        public static final String ITEM_ID = "itemId";
        public static final String RECIPIENT_ID = "recipientId";
        public static final String RECIPIENT_NAME = "recipientName";
        public static final String SHOW_COMPLETE_CONVERSATION_ICON = "showCompleteConversationIcon";
        public static final String SHOW_COMPLETE_CONVERSATION_REQUEST_DIALOG = "showCompleteConversationRequest";
    }

    public class ParseConversation{
        public static final String MY_USER_ID = "myUserId";
        public static final String RECIPIENT_USER_ID = "recipientUserId";
        public static final String RECIPIENT_USER_NAME = "recipientUserName";
        public static final String ITEM = "item";
        public static final String UNREAD_COUNT = "unreadCount";
        public static final String SENT_COMPLETE = "sentComplete";
        public static final String RECEIVED_COMPLETE = "receivedComplete";
    }

    public class ParseMessage{
        public static final String SENDER_ID = "senderId";
        public static final String RECIPIENT_ID = "recipientId";
        public static final String MESSAGE_TEXT = "messageText";
        public static final String ITEM_ID = "itemId";
        public static final String CREATED_AT = "createdAt";
    }

    public class ReportForm{

        public static final String IS_LOST_FORM = "lost";
        public static final String IS_EDIT_FORM = "edit";
        public static final String ITEM = "item";
    }

    public class Geocoder {
        public static final String DESCRIPTION_NOT_AVAILABLE = "Description not Available";
        public static final String NO_LOCATION_AVAILABLE = "No location available";
    }

    public class ParsePush{
        public static final String EXTRA_NAME = "com.parse.Data";
        public static final String PUSH_TYPE = "pushType";
        public static final String SENDER_ID = "senderId";
        public static final String SENDER_NAME = "senderName";
        public static final String ITEM_ID = "itemId";
        public static final String REPORTED_ITEM = "reportedItem";

        public static final String TYPE_LOST = "ParseLost";
        public static final String TYPE_FOUND = "ParseFound";
        public static final String TYPE_MESSAGE = "ParseMessage";
        public static final String TYPE_DELETE_CONVERSATION = "ParseDeleteConversation";

        public static final String COMPLETE_CONVERSATION_REQUEST = "ParseCompleteConversationRequest";
        public static final String COMPLETE_CONVERSATION_REPLY = "ParseCompleteConversationReply";
        public static final String TYPE_CONVERSATION = "ParseConversation";
        public static final String TYPE_MY_MESSAGE = "ParseMyMessage";
        public static final String TYPE_POTENTIAL_MATCHES = "ParsePotentialMatches";
    }


    public class ParseCloudMethods {
        public static final String COMPLETE_CONVERSATION_REQUEST = "completeConversationRequest";
        public static final String COMPLETE_CONVERSATION_REPLY = "completeConversationReply";
        public static final String LOOK_FOR_MATCHES = "lookForMatches";
    }

    public enum UIActions
    {
        uiaItemSaved,
        uiaConversationSaved,
        uiaMessageSaved,
        uiaCompleteConversationSent,
    }

    public class ParseCloud {

        public static final String REPORT_ID = "reportId";
        public static final String PUBLISHER_ID = "publisherId";
        public static final String IS_LOST = "isLost";
    }
}
