package ugent.waves.healthrecommenderapp.Persistance;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

@Dao
public abstract class SessionDao {

    public void insertActivitiesForSession(Session session, List<SessionActivity> activities){

        for(SessionActivity a : activities){
            a.setSessionId(session.getUid());
        }

        _insertAll(activities);
    }

    @Insert
    abstract void _insertAll(List<SessionActivity> activities);

    @Insert
    public abstract long insertSession(Session session);

    @Query("SELECT * FROM Session WHERE week > :week")
    public abstract Session[] getSessionsFromWeek(int week);

    @Query("SELECT * FROM Session")
    public abstract Session[] loadAllSessions();

    @Transaction
    @Query("SELECT * FROM Session")
    public abstract List<SessionWithActivities> getSessionsWithActivities();
}
