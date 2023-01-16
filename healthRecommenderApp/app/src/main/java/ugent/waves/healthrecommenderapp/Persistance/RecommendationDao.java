package ugent.waves.healthrecommenderapp.Persistance;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface RecommendationDao {

    @Query("DELETE FROM Recommendation WHERE userId = :user")
    void deleteAllRecommendations(String user);

    @Query("SELECT * FROM Recommendation WHERE done = :done AND userId = :user")
    Recommendation[] getRecommendationWithDone(boolean done, String user);

    @Query("SELECT * FROM Recommendation WHERE userId = :user")
    Recommendation[] getAllRecommendations(String user);

    @Query("SELECT * FROM Recommendation WHERE uid = :id")
    Recommendation getRecommendationForId(int id);

    @Query("SELECT * FROM Recommendation WHERE pending = :pending AND userId = :user")
    Recommendation[] getPendingRecommendation(boolean pending, String user);

    @Update
    int updateRecommendation(Recommendation recommendation);

    @Insert
    void insertRecommendation(Recommendation recommendation);
}
