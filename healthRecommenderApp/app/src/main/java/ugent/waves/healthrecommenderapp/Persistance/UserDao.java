package ugent.waves.healthrecommenderapp.Persistance;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UserDao {

    @Insert
    void insertUser(User user);

    @Query("SELECT * FROM User WHERE uid = :id")
    User getUser(String id);

    @Query("SELECT * FROM User WHERE current == :current")
    User getCurrent(boolean current);

    @Update
    int updateUser(User user);
}
