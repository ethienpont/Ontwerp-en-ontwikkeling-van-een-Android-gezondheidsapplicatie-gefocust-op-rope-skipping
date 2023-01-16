package ugent.waves.healthrecommenderapp.Persistance;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface ActivityDao {
    @Query("SELECT * FROM Activity WHERE userId = :user")
    SessionActivity[] getAllActivities(String user);

    @Query("SELECT * FROM Activity WHERE sessionId == :id AND userId = :user")
    SessionActivity[] getActivitiesForSession(int id, String user);

    @Insert
    void insertActivity(SessionActivity activity);

    @Query("SELECT * FROM Activity WHERE week > :week AND userId = :user")
    SessionActivity[] getActivitiesFromWeek(int week, String user);

    @Query("DELETE FROM Activity WHERE week < :week AND userId = :user")
    void deleteActivities(int week, String user);
}
