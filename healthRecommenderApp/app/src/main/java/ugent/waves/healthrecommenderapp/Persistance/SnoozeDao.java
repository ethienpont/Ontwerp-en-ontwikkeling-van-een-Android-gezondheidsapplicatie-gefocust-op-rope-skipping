package ugent.waves.healthrecommenderapp.Persistance;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface SnoozeDao {
    @Query("DELETE FROM Snooze WHERE week < :week")
    void deleteAllFromWeek(int week);

    @Query("SELECT * FROM Snooze WHERE weekday = :weekday AND hour = :h")
    Snooze[] getSnoozeForDayAndHour(int weekday, int h);

    @Insert
    void insertSnooze(Snooze snooze);
}
