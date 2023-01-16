package ugent.waves.healthrecommenderapp.Persistance;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Session.class, SessionActivity.class, Recommendation.class, Mistake.class, User.class}, version = 27)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SessionDao sessionDao();
    public abstract ActivityDao activityDao();
    public abstract RecommendationDao recommendationDao();
    public abstract MistakeDao mistakeDao();
    public abstract UserDao userDao();
}
