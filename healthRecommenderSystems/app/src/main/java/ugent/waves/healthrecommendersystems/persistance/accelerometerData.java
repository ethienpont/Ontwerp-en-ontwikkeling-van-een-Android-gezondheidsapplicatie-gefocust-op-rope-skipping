package ugent.waves.healthrecommendersystems.persistance;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class accelerometerData {

    public accelerometerData(float xCoordinate, float yCoordinate, float zCoordinate){
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
        this.zCoordinate = zCoordinate;
    }

    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "x_coordinate")
    public float xCoordinate;

    @ColumnInfo(name = "y_coordinate")
    public float yCoordinate;

    @ColumnInfo(name = "z_coordinate")
    public float zCoordinate;
}
