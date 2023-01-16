package ugent.waves.healthrecommenderapp.Persistance;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface SessionDao {

    @Insert
    long insertSession(Session session);

    @Query("SELECT * FROM Session WHERE week > :week AND userId = :user")
    Session[] getSessionsFromWeek(int week, String user);

    @Query("SELECT * FROM Session WHERE userId = :user")
    Session[] loadAllSessions(String user);

    @Query("DELETE FROM Session WHERE week < :week AND userId = :user")
    void deleteSessions(int week, String user);
}
