import tensorflow
from rest_framework import views
from rest_framework.response import Response
from rest_framework import status

import pandas as pd
import numpy as np
from datetime import datetime

from scipy.signal import savgol_filter, find_peaks

from .serializers import DataSerializer

class YourView(views.APIView):

    model = tensorflow.keras.models.load_model('model21april2020_fs25.h5')

    def processData(self,data):
        print(data)

    def post(self, request, *args, **kwargs):
        print("received")
        self.processData(request.data)
        #yourdata= [{"likes": 10, "comments": 0}, {"likes": 4, "comments": 23}]
        #results = DataSerializer(yourdata, many=True).data
        return Response(status=status.HTTP_200_OK)

    def convert_to_float(self, x):
        try:
            return np.float(x)
        except:
            return np.nan

    def convert_to_datetime(self, x):
        dt = datetime.fromtimestamp(x // 1000000000)
        s = dt.strftime('%Y-%m-%d %H:%M:%S')
        s += '.' + str(int(x % 1000000000)).zfill(9)
        return s

    def preprocess(self,data,drop_interval_begin=3, drop_interval_end=3):
        column_names_org = ['time', 'x', 'y', 'z']
        # convert nanoseconds to date
        data['time'] = data['time'].apply(self.convert_to_datetime)
        data['time'] = pd.to_datetime(data['time'])

        # convert to same data type
        for i in range(1, 4):
            data[column_names_org[i]] = data[column_names_org[i]].apply(self.convert_to_float)

        # drop rows with NaN values
        data.dropna(axis=0, how='any', inplace=True)  # TODO: invullen met mean, mod of median / interpolatie

        # drop duplicates
        data.drop_duplicates(subset=None, keep='first', inplace=True)

        # drop first and last 3 sec
        indexFirst = data[(data['time'].iloc[0] + pd.to_timedelta(drop_interval_begin, unit='s')) > data['time']].index
        data.drop(indexFirst, inplace=True)
        indexLast = data[(data['time'].iloc[-1] - pd.to_timedelta(drop_interval_end, unit='s')) < data['time']].index
        data.drop(indexLast, inplace=True)

        return data

    # generate windows with 50% overlap
    def windows(df, time, overlap):
        start = df.iloc[0]
        while (start + pd.to_timedelta(time, unit='s')) < df.iloc[-1]:
            yield start, (start + pd.to_timedelta(time, unit='s'))
            if overlap:
                start += pd.to_timedelta(time / 2, unit='s')
            else:
                start += pd.to_timedelta(time, unit='s')
        # last samples
        yield (df.iloc[-1] - pd.to_timedelta(time, unit='s')), df.iloc[-1]

    def get_turns(self, df, activity):
        if activity == "side_swing":
            for i in range(0, 5):
                df['x'] = savgol_filter(df['x'].to_numpy(), 101, 5)
                df['y'] = savgol_filter(df['y'].to_numpy(), 101, 5)
                df['z'] = savgol_filter(df['z'].to_numpy(), 101, 5)
                df.plot(x='time', subplots=True)

            wx = find_peaks(df['x'])
            wy = find_peaks(df['y'])
            wz = find_peaks(df['z'])
            return (len(wx[0]) + len(wy[0]) + len(wz[0])) / 3

    def predict(self, segments):
        return model.predict(segments)

    #start en end toevoegen aan label
    def get_activities(self, df_org, pred, labels, w):
        trantitions = pd.DataFrame()
        index = 0
        for (start, end) in windows(df_org['time'], w, False):
            activity = labels[pred[index]]
            trantitions = trantitions.append(pd.Series({"start": start, "end": end, "activity": activity}),
                                             ignore_index=True)
            index += 1

        return trantitions

    #TODO: eventueel 1 segments anders laten wegvallen
    def get_trantitions(selfs, a):
        trantitions_ = pd.DataFrame()
        activity = a["activity"].iloc[0]
        start = a["start"].iloc[0]
        for i in range(1, a.shape[0]):
            if a["activity"].iloc[i] != a["activity"].iloc[i - 1]:
                end = a["end"].iloc[i - 1]
                trantitions_ = trantitions_.append(pd.Series({"start": start, "end": end, "activity": activity}),
                                                   ignore_index=True)
                start = a["start"].iloc[i]
                activity = a["activity"].iloc[i]


