package ugent.waves.wearable;

import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.service.FitnessSensorService;
import com.google.android.gms.fitness.service.FitnessSensorServiceRequest;

import java.util.List;

public class accelerometerService extends FitnessSensorService {
    @Override
    public List<DataSource> onFindDataSources(List<DataType> list) {
        return null;
    }

    @Override
    public boolean onRegister(FitnessSensorServiceRequest fitnessSensorServiceRequest) {
        return false;
    }

    @Override
    public boolean onUnregister(DataSource dataSource) {
        return false;
    }
}
