import google
import pandas as pd
from datetime import datetime
import numpy as np
from sklearn import preprocessing
from sklearn.model_selection import train_test_split
import json
import httplib2
from datetime import datetime
from googleapiclient.discovery import build
from oauth2client.client import OAuth2WebServerFlow
from google.oauth2 import id_token
from google.auth.transport import requests
import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore

def convert_to_float(x):
    try:
        return np.float(x)
    except:
        return np.nan

def preprocess(df):
    # convert to same data type
    df["mets_reached"] = df["mets_reached"].apply(convert_to_float)
    df["mets_goal"] = df["mets_goal"].apply(convert_to_float)

    df['mets_percentage'] = df['mets_reached'] / df['mets_goal']
    df.drop('mets_reached', axis=1, inplace=True)

    X_train, X_test, y_train, y_test = train_test_split(df.loc[:, ['week_number', 'mets_percentage']],
                                                        df['mets_goal'], test_size=0.33, random_state=42)

    #TODO: welke manier van schalen: normalize want aparte samples met elkaar gaan vergelijken

    normalizer = preprocessing.Normalizer().fit(X_train)

    # normalize features
    X_train = pd.DataFrame(normalizer.transform(X_train), columns=['week_number', 'mets_percentage'])

    # drop rows with NaN values
    X_train.dropna(axis=0, how='any', inplace=True)  # TODO: invullen met mean, mod of median / interpolatie

    # drop duplicates
    X_train.drop_duplicates(subset=None, keep='first', inplace=True)

    return X_train, X_test, y_train, y_test

#connection with firestore
cred = credentials.Certificate("./healthrecommendersystems-firebase-adminsdk-psl7m-6f276b3d23.json")
app = firebase_admin.initialize_app(cred)

store = firestore.client()

users_ref = store.collection(u'users')
docs = users_ref.stream()

#dict with dataframes for al the users
users = {}
#determine for which week the new goal is
lastWeek = {}

for doc in docs:
    users[doc.id] = pd.DataFrame(columns=['week_number', 'mets_reached', 'mets_goal'])
    goalHistory = store.collection('users', doc.id, 'goals')
    lastWeek[doc.id] = 0
    for week in goalHistory.stream():
        users[doc.id] = users[doc.id].append(pd.Series(week.to_dict()), ignore_index=True)
        if(lastWeek[doc.id] < int(week.id)):
            lastWeek[doc.id] = int(week.id)

print(lastWeek)
for user in users:
    user_df = users[user]
    X_train, X_test, y_train, y_test = preprocess(user_df)
    #TODO: train model and predict goal for next week
    #TODO: sla goal op in nieuwe subcollectie user

'''
#File Format: week_start; week_end; mets_reached; mets_goal
def read_data(file_path, column_names):
    data = pd.read_csv(file_path, sep=',', header=0, skipinitialspace=True)

    #convert day to date
    #data['day'] = data['day'].apply(convert_to_datetime)
    data['week_start'] = pd.to_datetime(data['week_start'])
    data['week_end'] = pd.to_datetime(data['week_end'])

    #convert to same data type
    for i in range(2, 3):
        data[column_names[i]] = data[column_names[i]].apply(convert_to_float)

    data['mets_percentage'] = data['mets_reached']/data['mets_goal']
    data.drop('mets_reached', axis=1, inplace=True)

    X_train, X_test, y_train, y_test = train_test_split(data.loc[:,['week_start','week_end', 'mets_percentage']], data['mets_goal'], test_size = 0.33, random_state = 42)

    #scaler = preprocessing.StandardScaler().fit(X_train)

    #normalize features
    #scaler.transform(X_train)

    #drop rows with NaN values
    X_train.dropna(axis=0, how='any', inplace=True) #TODO: invullen met mean, mod of median / interpolatie

    #drop duplicates
    X_train.drop_duplicates(subset=None, keep='first', inplace = True)

    return X_train, X_test, y_train, y_test
'''
def convert_to_datetime(x):
    dt = datetime.fromtimestamp(x // 1000000000)
    s = dt.strftime('%Y-%m-%d %H:%M:%S')
    s += '.' + str(int(x % 1000000000)).zfill(9)
    return s


#X_train, X_test, y_train, y_test = read_data('data/goal_test_data.csv', ['week_start', 'week_end', 'mets_reached', 'mets_goal'])

