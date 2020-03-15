import uuid

import joblib
import pandas as pd
import numpy as np
import math
import statistics
from datetime import datetime, timedelta
from sklearn.model_selection import train_test_split
from sklearn import preprocessing
from sklearn.ensemble import RandomForestClassifier, ExtraTreesClassifier, AdaBoostClassifier
from sklearn.model_selection import GridSearchCV
from sklearn.linear_model import SGDClassifier
from sklearn.naive_bayes import GaussianNB, MultinomialNB
from sklearn.neighbors import KNeighborsClassifier
from sklearn.svm import SVC, LinearSVC
from sklearn import svm
from sklearn.neural_network import MLPClassifier
from sklearn.metrics import confusion_matrix, classification_report
from sklearn.preprocessing import StandardScaler, LabelEncoder, normalize
import matplotlib.pyplot as plt
import seaborn as sn
from scipy.signal import savgol_filter, find_peaks, correlate
from sklearn.decomposition import PCA
import scipy.fftpack as FFT
from sklearn.utils import shuffle
import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
import time

#DATA QUALITY ASSESSMENT: inconsistent values (convert to same type), duplicate values (remove duplicates), missing values (remove na)
from sklearn.tree import DecisionTreeClassifier

def convert_to_float(x):
    try:
        return np.float(x)
    except:
        return np.nan


def convert_to_datetime(x):
    dt = datetime.fromtimestamp(x // 1000000000)
    s = dt.strftime('%Y-%m-%d %H:%M:%S')
    s += '.' + str(int(x % 1000000000)).zfill(9)
    return s


def preprocess(data, activity, drop_interval_begin=3, drop_interval_end=3):
    column_names_org = ['time', 'x', 'y', 'z']
    # convert nanoseconds to date
    #data['time'] = data['time'].apply(convert_to_datetime)
    data['time'] = pd.to_datetime(data['time'])

    # convert to same data type
    for i in range(1, 4):
        data[column_names_org[i]] = data[column_names_org[i]].apply(convert_to_float)

    # drop rows with NaN values
    data.dropna(axis=0, how='any', inplace=True)  # TODO: invullen met mean, mod of median / interpolatie

    # drop duplicates
    data.drop_duplicates(subset=None, keep='first', inplace=True)

    # drop first and last 3 sec
    indexFirst = data[(data['time'].iloc[0] + pd.to_timedelta(drop_interval_begin, unit='s')) > data['time']].index
    data.drop(indexFirst, inplace=True)
    indexLast = data[(data['time'].iloc[-1] - pd.to_timedelta(drop_interval_end, unit='s')) < data['time']].index
    data.drop(indexLast, inplace=True)

    # add activity label
    if activity:
        data['activity'] = activity

    return data


# feature extraction
def get_mean_window(df):
    return df.mean()


def get_min_window(df):
    return df.min()


def get_max_window(df):
    return df.max()


def get_std_window(df):
    return df.std()


def get_med_window(df):
    return df.median()


# TODO: scipy integrate
# TODO: voor elke as apart??
# measure of activity level (m/s²)
def get_signal_magnitude_area(df):
    sum = 0
    for i in range(0, len(df)):
        sum += (abs(df['x'].iloc[i]) + abs(df['y'].iloc[i]) + abs(df['z'].iloc[i]))
    return sum / len(df)


# result = integrate.quad(lambda t: df['x'].apply(lambda n : abs(n)) + df['y'].apply(lambda n : abs(n)) + df['z'].apply(lambda n : abs(n)), 0, len(df))

# TODO: voor elke as apart??
# degree of movement intensity (m/s²)
def get_signal_magnitude_vector(df):
    sum = 0
    for i in range(0, len(df)):
        sum += math.sqrt(
            df['x'].iloc[i] * df['x'].iloc[i] + df['y'].iloc[i] * df['y'].iloc[i] + df['z'].iloc[i] * df['z'].iloc[i])
    return sum


# average angle (radian) between accelerometer vector and x as (parallel with arm)
def get_tilt_angle(df):
    df_cos = pd.DataFrame(columns=["tilt_ang"])
    df_dot = df['x']
    for i in range(0, len(df)):
        s = pd.Series({"tilt_ang": (df_dot.iloc[i]) / (math.sqrt(
            df['x'].iloc[i] * df['x'].iloc[i] + df['y'].iloc[i] * df['y'].iloc[i] + df['z'].iloc[i] * df['z'].iloc[
                i]))})
        df_cos = df_cos.append(s, ignore_index=True)
    df_angle = np.arccos(df_cos)
    return df_angle.mean()['tilt_ang']


def get_power_spectral_density(df):
    df_psd = np.abs(df) ** 2
    return df_psd.sum()


# TODO: datatype is object en niet compex nr
def get_entropy(df):
    entropy = []
    pdf = df / df.sum()
    for i in range(1, len(pdf.columns)):
        entropy.append(np.complex(-np.nansum(pdf.iloc[:, i] * np.log2(pdf.iloc[:, i]))))
    return entropy


# generate windows with 50% overlap
def windows(df, time, overlap):
    start = df.iloc[0]
    while (start + pd.to_timedelta(time, unit='s')) < df.iloc[-1]:
        yield start, (start + pd.to_timedelta(time, unit='s'))
        if overlap:
            start += pd.to_timedelta(time / 2, unit='s')
        else:
            start += pd.to_timedelta(time, unit='s')


def feature_extraction_segmentation(data, window, overlap):
    column_names = ["x_mean", "y_mean", "z_mean", "x_min", "y_min", "z_min", "x_max", "y_max", "z_max",
                    "x_std", "y_std", "z_std", "x_med", "y_med", "z_med"]
    df = pd.DataFrame(columns=column_names)

    for (start, end) in windows(data['time'], window, overlap):
        vw1 = data['time'] >= start
        vw2 = data['time'] < end
        mean = get_mean_window(data[vw1 & vw2])
        min = get_min_window(data[vw1 & vw2])
        max = get_max_window(data[vw1 & vw2])
        std = get_std_window(data[vw1 & vw2])
        med = get_med_window(data[vw1 & vw2])
        sma = get_signal_magnitude_area(data[vw1 & vw2])
        smv = get_signal_magnitude_vector(data[vw1 & vw2])
        tilt = get_tilt_angle(data[vw1 & vw2])
        # fourrier transform
        t_x = data[vw1 & vw2][['time', 'x']].set_index('time')
        t_y = data[vw1 & vw2][['time', 'y']].set_index('time')
        t_z = data[vw1 & vw2][['time', 'z']].set_index('time')

        df_f = pd.DataFrame(columns=['f', 'x_f', 'y_f', 'z_f'])

        # TODO: determine sampling rate (datapoints per second)
        sampling_rate = 18

        df_f['x_f'] = FFT.fft(t_x).ravel()
        df_f['y_f'] = FFT.fft(t_y).ravel()
        df_f['z_f'] = FFT.fft(t_z).ravel()
        df_f['f'] = FFT.fftfreq(len(df_f['x_f'])) * sampling_rate

        psd = get_power_spectral_density(df_f)
        # entropy = get_entropy(df_f)

        df = df.append(pd.Series({'x_mean': mean['x'], 'y_mean': mean['y'], 'z_mean': mean['z'], "x_min": min['x'],
                                  "y_min": min['y'], "z_min": min['z'], "x_max": max['x'], "y_max": max['y'],
                                  "z_max": max['z'],
                                  "x_std": std['x'], "y_std": std['y'], "z_std": std['z'], "x_med": med['x'],
                                  "y_med": med['y'], "z_med": med['z'],
                                  "sma": sma, "smv": smv, "tilt": tilt, "x_psd": psd['x_f'], "y_psd": psd['y_f'],
                                  "z_psd": psd['z_f']}),
                       ignore_index=True)  # "x_entropy" : entropy[0], "y_entropy" : entropy[1], "z_entropy" : entropy[2],
    return df

def pca(merged):
    pca = PCA(n_components=6)
    merged = pd.DataFrame(pca.fit_transform(merged))

    return merged


#FIREBASE
veryLightZone = 1
lightZone = 2
moderateZone = 3
hardZone = 4
maximumZone = 5
#TODO: wat doen met persoonlijke geg?
MAXHR = 220 -21
#TODO: hoe week bepalen?
week=0

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

#TODO: verschil in nanoseconden, nu altijd 0
def met_points(df):
    score = 0
    #MPA = ligthzone, MPV = moderate zone
    timeMPA = timeMPV = 0
    sumMETmin = 0
    for index, row in df.iterrows():
        zone = getHeartRateZone(row["heart_rate"])
        if (zone == getHeartRateZone(df["heart_rate"].iloc[index-1])):
            if (zone == lightZone):
                timeMPA += pd.Timedelta(row["time"] - df["time"].iloc[index-1]).total_seconds()*1000000
            elif (zone == moderateZone):
                timeMPV += pd.Timedelta(row["time"] - df["time"].iloc[index-1]).total_seconds()*1000000

    timeMPA = (timeMPA * math.pow(10, -3)) / 60
    timeMPV = (timeMPV * math.pow(10, -3)) / 60
    sumMETmin += 4 * timeMPA + 8 * timeMPV

    return sumMETmin

def get_trantitions(df_org, pred, labels):
    trantitions = pd.DataFrame()
    index=0
    for (start, end) in windows(df_org['time'], 1, False):
        activity = labels[pred[index]]
        trantitions = trantitions.append(pd.Series({"start": start, "end": end, "activity": activity}),
                       ignore_index=True)
        index += 1

    return trantitions

def number_of_turns(df):
    period = df.iloc[10:11]
    return find_peaks(correlate(period, df), height=200)[0].size

#TODO
def write_goal(user, week):
    #TODO: alleen laatste 3 weken bijhouden
    sessionRef = store.collection(u'users/' + user.id + '/sessionCalculations/')
    sessions = sessionRef.where(u'week', u'in', [week-1, week-2, week-3]).stream()
    sum = 0
    for s in sessions:
        sum += s.to_dict()["met_points"]

    data = {
        'goal': sum/3
    }

    store.document(u'users/' + user.id + '/goals/' + week).set(data)

    return sum/3

#TODO: ranken
def generate_recommendations(user):
    #next weeks goal
    #goal = store.document(u'users/' + user.id + '/goals/' + week).to_dict()["goal"]
    goal = 300
    #delete previous recommendations
    recommendations = store.collection(u'users/' + user.id + '/recommendations').stream()
    for r in recommendations:
        r.reference.delete()
    sessionRef = store.collection(u'users/' + user.id + '/sessionCalculations').stream()
    sessions = pd.DataFrame()
    for s in sessionRef:
        activityRef = store.collection(u'users/' + user.id + '/sessionCalculations/' + s.id + '/activities').stream()
        for a in activityRef:
            sessions = sessions.append(pd.Series(a.to_dict()), ignore_index=True)
    print(sessions)
    #count
    sessions['count'] = sessions.groupby('activity')['activity'].transform('count')

    #duration
    sessions['duration'] = 1
    for index, row in sessions.iterrows():
        sessions['duration'].iloc[index] = pd.Timedelta(row['end'] - row['start']).total_seconds()

    print(sessions['duration'])
    #mets/min
    sessions['mets/h'] = sessions["met_points"] / sessions["duration"] #TODO: in uren

    #mean mets/min
    sessions["mean_mets/h"] = sessions.groupby('activity')['mets/h'].transform('mean') #TODO: wat als mean mets 0 is

    recommended_mets = 0
    #recommendations
    while (recommended_mets < goal):
        act = np.random.choice(sessions["activity"])
        #duration in hours
        duration = np.random.uniform(low=0.2, high=3)
        mets = duration * sessions[sessions["activity"] == act]["mean_mets/h"].iloc[0]
        recommendation = {
                "activity": act, "duration": duration, "mets": mets
            }
        store.document(u'users/' + user.id + '/recommendations/' + str(uuid.uuid1())).set(recommendation) #TODO: random doc id
        recommended_mets += mets


# load model + labelEncoder
model = joblib.load(r"C:\Users\Elise\Documents\unif\master\semester2\masterproef\gitProject\thesis\rope_skipping_model.sav")
le = joblib.load(r"C:\Users\Elise\Documents\unif\master\semester2\masterproef\gitProject\thesis\rope_skipping_label_encoder.plk")

#connection with firestore
cred = credentials.Certificate("./healthrecommendersystems-firebase-adminsdk-psl7m-6f276b3d23.json")
app = firebase_admin.initialize_app(cred)

store = firestore.client()

users_ref = store.collection(u'users')
docs = users_ref.stream()

#dict with dataframes for al the users
users = {}

for user in docs:
    r'''
    sessions = store.collection('users/'+ user.id+'/sessions/rope_skipping_accelerometer/session_ids').stream()
    for session in sessions:
        id = session.to_dict()["id"]
        #sessionAccelerometerData = store.collection('users/' + user.id + '/sessions/rope_skipping_accelerometer/'+id).stream()
        #sessionHeartRateData = store.collection('users/' + user.id + '/sessions/rope_skipping_heart_rate/' + id).stream()
        accelorometer_df = pd.read_csv(r"C:\Users\Elise\Documents\unif\master\semester2\masterproef\gitProject\thesis\data\jump_fast.csv", sep=';', header=0, skipinitialspace=True)
        print(accelorometer_df.head())
        heart_rate_df = pd.read_csv(r"C:\Users\Elise\Documents\unif\master\semester2\masterproef\gitProject\thesis\data\jump_fast - heart_rate.csv", sep=';', header=0, skipinitialspace=True)
        heart_rate_df["time"] = pd.to_datetime(heart_rate_df["time"])
        
        for dataPoint in sessionAccelerometerData:
            accelorometer_df = accelorometer_df.append(pd.Series(dataPoint.to_dict()),
                           ignore_index=True)
        for dataPoint in sessionHeartRateData:
            heart_rate_df = heart_rate_df.append(pd.Series(dataPoint.to_dict()),
                           ignore_index=True)
        print(heart_rate_df.head())

        #opdelen in windows en predictions
        accelorometer_df = preprocess(accelorometer_df, None, 0, 0)
        accelorometer_df_extraction = feature_extraction_segmentation(accelorometer_df, 1, False)
        if accelorometer_df.shape[0] > 0:
            accelorometer_df_pca = pca(accelorometer_df_extraction)
            pred = model.predict(accelorometer_df_pca.to_numpy())
            trantitions = get_trantitions(accelorometer_df, pred, le.inverse_transform([0,1,2,3,4]))
            print(trantitions)
            #make bigger windows of activity performed
            trantitions_ = pd.DataFrame()
            activity = trantitions["activity"].iloc[0]
            start = trantitions["start"].iloc[0]
            for i in range(1, trantitions.shape[0]):
                if trantitions["activity"].iloc[i] != trantitions["activity"].iloc[i-1]:
                    end = trantitions["end"].iloc[i-1]
                    trantitions_ = trantitions_.append(pd.Series({"start": start, "end": end, "activity": activity}), ignore_index=True)
                    start = trantitions["start"].iloc[i]
                    activity = trantitions["activity"].iloc[i]
            print(trantitions_)

            number_turns = 0
            for index, row in trantitions_.iterrows():
                # calculate met points
                points = met_points(heart_rate_df[(heart_rate_df["time"] >= row["start"]) & (heart_rate_df["time"] < row["end"])])

                #turns
                turns_x = number_of_turns(accelorometer_df[(accelorometer_df["time"] >= row["start"]) & (accelorometer_df["time"] < row["end"])]["x"])
                turns_y = number_of_turns(accelorometer_df[(accelorometer_df["time"] >= row["start"]) & (accelorometer_df["time"] < row["end"])]["y"])
                turns_z = number_of_turns(accelorometer_df[(accelorometer_df["time"] >= row["start"]) & (accelorometer_df["time"] < row["end"])]["z"])

                number_turns += int((turns_x+turns_y+turns_z)/3)
                print(number_turns)
                data = {
                    'start': row["start"],
                    'end': row["end"],
                    "activity": row["activity"],
                    'met_points': points
                }

                store.document(u'users/' + user.id + '/sessionCalculations/' + id + '/activities/' + row["start"].strftime("%H:%M:%S")).set(data)
                print("gelukt")

            # TODO: time to nanosec
            data = {
                    'week': week,
                    'start': trantitions["start"].iloc[0],
                    'end': trantitions["end"].iloc[-1],
                    'turns': number_turns
            }

            store.document(u'users/' + user.id + '/sessionCalculations/' + id).set(data)
    '''
    generate_recommendations(user)
#TODO: functie om elke week recommendations te generenen
