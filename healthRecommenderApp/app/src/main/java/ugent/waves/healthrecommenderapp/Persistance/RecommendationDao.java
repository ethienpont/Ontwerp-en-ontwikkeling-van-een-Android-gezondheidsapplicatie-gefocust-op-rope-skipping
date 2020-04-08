package ugent.waves.healthrecommenderapp.Persistance;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface RecommendationDao {

    @Query("DELETE FROM Recommendation")
    void deleteAllRecommendations();

    @Query("SELECT * FROM Recommendation WHERE nr = :rank")
    Recommendation getRecommendationWithNr(int rank);

    @Query("SELECT * FROM Recommendation")
    Recommendation[] getAllRecommendations();

    @Query("SELECT * FROM Recommendation WHERE uid = :id")
    Recommendation getRecommendationForId(int id);

    @Query("SELECT * FROM Recommendation WHERE pending = :pending")
    Recommendation[] getPendingRecommendation(boolean pending);

    @Update
    void updateRecommendation(Recommendation recommendation);

    @Insert
    void insertRecommendation(Recommendation recommendation);
}
