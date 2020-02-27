package ugent.waves.healthrecommendersystems.persistance;

import androidx.room.Dao;
import androidx.room.Insert;

@Dao
public interface activityAPIDataDao {
    @Insert
    void insertActivityAPIData(activityAPIData data);
}
