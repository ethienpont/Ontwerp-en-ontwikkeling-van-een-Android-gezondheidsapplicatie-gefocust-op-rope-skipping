package ugent.waves.healthrecommenderapp.Persistance;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface RecommendationDao {
    @Query("SELECT * FROM Recommendation")
    Recommendation[] getAllRecommendations();

    @Insert
    void insertRecommendation(Recommendation recommendation);
}
