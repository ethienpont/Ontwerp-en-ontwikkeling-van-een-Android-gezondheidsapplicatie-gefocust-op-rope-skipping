package ugent.waves.healthrecommenderapp.Persistance;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Session.class, SessionActivity.class, Recommendation.class, Mistake.class, Snooze.class}, version = 13)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SessionDao sessionDao();
    public abstract ActivityDao activityDao();
    public abstract RecommendationDao recommendationDao();
    public abstract MistakeDao mistakeDao();
    public abstract SnoozeDao snoozeDao();
}
