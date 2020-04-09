package ugent.waves.healthrecommenderapp.Persistance;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface ActivityDao {
    @Query("SELECT * FROM Activity")
    SessionActivity[] getAllActivities();

    @Query("SELECT * FROM Activity WHERE sessionId == :id")
    SessionActivity[] getActivitiesForSession(int id);

    @Insert
    void insertActivity(SessionActivity activity);

    @Query("SELECT * FROM Activity WHERE week > :week")
    SessionActivity[] getActivitiesFromWeek(int week);
}
