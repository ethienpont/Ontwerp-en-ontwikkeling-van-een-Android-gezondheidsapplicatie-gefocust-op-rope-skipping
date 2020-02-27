package ugent.waves.healthrecommendersystems.persistance;

import androidx.room.Dao;
import androidx.room.Insert;

@Dao
public interface accelerometerDataDao {
    @Insert
    void insertAccelerometerData(accelerometerData data);
}
