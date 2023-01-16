package ugent.waves.healthrecommenderapp.HelpClasses;

public class Constants {
    //Default goal
    public static final double GOAL = 600;
    //Low bound recommendation duration
    public static final int DEFAULT1 = 120;
    //High bound recommendation duration
    public static final int DEFAULT2 = 300;

    public static final String ACTIVITY_ID = "ACTIVITY_ID";
    public static final String RECOMMENDATION_ID = "RECOMMENDATION_ID";

    //Access database
    public static final String GET_ALL = "GET_ALL";
    public static final String GET = "GET";
    public static final String UPDATE = "UPDATE";

    //MessageClient communication paths
    public static final String START = "/START";
    public static final String ACCELEROMETER = "/ACCELEROMETER";
    public static final String STOP = "/STOP";
    public static final String HEARTRATE = "/HEARTRATE";
    public static final String AGE = "/AGE";
    public static final String PATH = "/SESSION_START";

    //Heartrate constants
    public static final int veryLightZone = 1;
    public static final int lightZone = 2;
    public static final int moderateZone = 3;
    public static final int hardZone = 4;
    public static final int maximumZone = 5;

    public static final int SEGMENT_SIZE = 52;
    public static final int WINDOW = 1;

    public static final String ARG_DATA = "sessionId";

    //Google sign in
    public static final int RC_SIGN_IN = 1;
    public static final int PERMISSION_REQUEST = 2;

    //SharedPreferences
    public static final String PREFS_NAME = "prefs";
    public static final String PREF_FIRST_RUN = "version_code";
    public static final int DOESNT_EXIST = -1;
}
