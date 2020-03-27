package ugent.waves.healthrecommenderapp.Persistance;

import java.util.List;

import androidx.room.Embedded;
import androidx.room.Relation;

public class SessionWithActivities {
    @Embedded
    public Session session;

    @Relation(
            parentColumn = "uid",
            entityColumn = "sessionId",
            entity = SessionActivity.class
    )
    public List<SessionActivity> activities;
}
