package ugent.waves.healthrecommenderapp.Persistance;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface MistakeDao {
    @Query("SELECT * FROM Mistake WHERE userId = :user")
    Mistake[] getAllMistakes(String user);

    @Query("SELECT * FROM Mistake WHERE sessionId == :id AND userId = :user")
    Mistake[] getMistakesForSession(int id, String user);

    @Insert
    void insertMistake(Mistake mistake);

    @Query("SELECT * FROM Mistake WHERE week > :week AND userId = :user")
    Mistake[] getMistakesFromWeek(int week, String user);

    @Query("DELETE FROM Mistake WHERE week < :week AND userId = :user")
    void deleteMistakes(int week, String user);
}
