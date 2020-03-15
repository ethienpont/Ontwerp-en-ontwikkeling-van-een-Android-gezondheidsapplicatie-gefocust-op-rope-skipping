import math

import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
import pandas as pd

veryLightZone = 1
lightZone = 2
moderateZone = 3
hardZone = 4
maximumZone = 5
#TODO: wat doen met persoonlijke geg?
MAXHR = 220 -21

def getHeartRateZone(v):
    if (v >= 0.5 * MAXHR) & (v < 0.6 * MAXHR):
        return veryLightZone
    elif (v >= 0.6 * MAXHR) & (v < 0.7 * MAXHR):
        return lightZone
    elif (v >= 0.7 * MAXHR) & (v < 0.8 * MAXHR):
        return moderateZone
    elif (v >= 0.8 * MAXHR) & (v < 0.9 * MAXHR):
        return hardZone
    elif (v >= 0.9 * MAXHR) & (v < MAXHR):
        return maximumZone
    return 0

def met_points(df):
    score = 0
    #MPA = ligthzone, MPV = moderate zone
    timeMPA = timeMPV = 0
    sumMETmin = 0
    for index, row in df.iterrows():
        zone = getHeartRateZone(row["heart_rate"])
        if (zone == getHeartRateZone(df["heart_rate"].iloc[index-1])):
            if (zone == lightZone):
                timeMPA += row["time"] - df["time"].iloc[index-1]
            elif (zone == moderateZone):
                timeMPV += row["time"] - df["time"].iloc[index-1]

    timeMPA = (timeMPA * math.pow(10, -3)) / 60
    timeMPV = (timeMPV * math.pow(10, -3)) / 60
    sumMETmin += 4 * timeMPA + 8 * timeMPV

    return sumMETmin

#connection with firestore
cred = credentials.Certificate("./healthrecommendersystems-firebase-adminsdk-psl7m-6f276b3d23.json")
app = firebase_admin.initialize_app(cred)

store = firestore.client()

users_ref = store.collection(u'users')
docs = users_ref.stream()

#dict with dataframes for al the users
users = {}

for user in docs:
    sessions = store.collection('users/'+ user.id+'/sessions/rope_skipping_accelerometer/session_ids').stream()
    for session in sessions:
        id = session.to_dict()["id"]
        sessionAccelerometerData = store.collection('users/' + user.id + '/sessions/rope_skipping_accelerometer/'+id).stream()
        sessionHeartRateData = store.collection('users/' + user.id + '/sessions/rope_skipping_heart_rate/' + id).stream()
        accelorometer_df = pd.DataFrame()
        heart_rate_df = pd.DataFrame()
        for dataPoint in sessionAccelerometerData:
            accelorometer_df = accelorometer_df.append(pd.Series(dataPoint.to_dict()),
                           ignore_index=True)
        for dataPoint in sessionHeartRateData:
            heart_rate_df = heart_rate_df.append(pd.Series(dataPoint.to_dict()),
                           ignore_index=True)
        print(heart_rate_df.head())

        #calculate met points
        points = met_points(heart_rate_df)

        #opdelen in windows en predictions


        data = {
            'turns': 20,
            'met_points': points
        }

        store.document(u'users/' + user.id + '/sessionCalculations/' + id).set(data)
        print("gelukt")
