package ugent.waves.healthrecommenderapp.Persistance;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface MistakeDao {
    @Query("SELECT * FROM Mistake")
    Mistake[] getAllMistakes();

    @Query("SELECT * FROM Mistake WHERE sessionId == :id")
    Mistake[] getMistakesForSession(int id);

    @Insert
    void insertMistake(Mistake mistake);
}
