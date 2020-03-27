package ugent.waves.healthrecommenderapp.Persistance;

import androidx.room.Dao;
import androidx.room.Query;

@Dao
public interface ActivityDao {
    @Query("SELECT * FROM Activity")
    SessionActivity[] getAllActivities();
}
