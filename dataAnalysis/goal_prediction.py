import sys

import google
import pandas as pd
from datetime import datetime
import numpy as np
from sklearn import preprocessing, metrics
from sklearn.ensemble import RandomForestRegressor
from sklearn.linear_model import SGDRegressor, Lasso, Ridge, ElasticNet
from sklearn.metrics import confusion_matrix
from sklearn.model_selection import train_test_split, GridSearchCV
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
from sklearn.neural_network import MLPRegressor
from sklearn.svm import SVR


def convert_to_float(x):
    try:
        return np.float(x)
    except:
        return np.nan

def preprocess(df):
    df_copy = pd.DataFrame(df)
    # convert to same data type
    df_copy["mets_reached"] = df_copy["mets_reached"].apply(convert_to_float)
    df_copy["mets_goal"] = df_copy["mets_goal"].apply(convert_to_float)

    df_copy['mets_percentage'] = df_copy['mets_reached'] / df_copy['mets_goal']
    df_copy.drop('mets_reached', axis=1, inplace=True)

    X_train, X_test, y_train, y_test = train_test_split(df_copy.loc[:, ['week_number', 'mets_percentage']],
                                                        df_copy['mets_goal'], test_size=0.33, random_state=42)

    #TODO: welke manier van schalen: normalize want aparte samples met elkaar gaan vergelijken? ook ytrain schalen, werkt nog niet!!

    xScaler = preprocessing.StandardScaler().fit(X_train)
    yScaler = preprocessing.StandardScaler().fit([y_train])

    # normalize features
    '''
    X_train = pd.DataFrame(xScaler.transform(X_train), columns=['week_number', 'mets_percentage'])
    X_test = pd.DataFrame(xScaler.transform(X_test), columns=['week_number', 'mets_percentage'])

    y_train = pd.DataFrame(yScaler.transform([y_train]), columns=['mets_goal'])
    y_test = pd.DataFrame(yScaler.transform([y_test]), columns=['mets_goal'])
'''
    # drop rows with NaN values
    X_train.dropna(axis=0, how='any', inplace=True)  # TODO: invullen met mean, mod of median / interpolatie

    # drop duplicates
    X_train.drop_duplicates(subset=None, keep='first', inplace=True)

    return X_train, X_test, y_train, y_test

def test(models, data, iterations = 10):
    highest_score = -sys.float_info.max
    estimator = None
    for i in models:
        r2_test = []
        X_train, X_test, y_train, y_test = preprocess(data)
        # hyperparameter tuning
        grid = GridSearchCV(models[i][0], models[i][1], refit=True, verbose=2)
        grid.fit(X_train, y_train)

        for j in range(iterations):
            r2_test.append(metrics.r2_score(y_test,
                                            grid.best_estimator_.fit(X_train,
                                                         y_train).predict(X_test)))

        if highest_score < np.mean(r2_test):
            highest_score = np.mean(r2_test)
            estimator = grid.best_estimator_

    return estimator

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

#TODO: kijk nr parameters
models = {'Lasso': [Lasso(), {'alpha': [1e-7, 1e-6, 1e-5, 1e-4, 1e-3, 1e-2, 1e-1],
                    'max_iter': [1, 10, 100, 1000],# np.ceil(10**6 / n)
                  }],
         'Ridge': [Ridge(), {'alpha': [1e-7, 1e-6, 1e-5, 1e-4, 1e-3, 1e-2, 1e-1],
                    'max_iter': [1, 10, 100, 1000],# np.ceil(10**6 / n)
                  }],
          'SGD': [SGDRegressor(), {'alpha': [1e-7, 1e-6, 1e-5, 1e-4, 1e-3, 1e-2, 1e-1], # SGD CLASSIFIER #TODO: geef meer gewicht aan recente bij lineaire regressie
                    'max_iter': [1, 10, 100, 1000],# np.ceil(10**6 / n)
                  }],
          'ElasticNet' : [ElasticNet(), {'alpha': [1e-7, 1e-6, 1e-5, 1e-4, 1e-3, 1e-2, 1e-1],
                    'max_iter': [1, 10, 100, 1000],# np.ceil(10**6 / n)
                  }],
          'SVR' : [SVR(), {'C': [0.1,1, 10, 100],
                    'epsilon': [0.1, 0.01, 0.001],
                  }],
          'RandomForest' : [RandomForestRegressor(), {'n_estimators': [1, 10, 100],
              #'max_depth': range(1,20, 5),#'min_samples_split': [0.2, 0.5, 0.8] ,
              'max_leaf_nodes': [2, 10, 100]}
                            ],
          'MLP' : [MLPRegressor(), {
              #'hidden_layer_sizes' : [(50,), (100,), (200,)],
              'alpha': [1e-7, 1e-6, 1e-5, 1e-4, 1e-3, 1e-2, 1e-1], #'max_iter': [100, 1000, 10000]
          }]}


for doc in docs:
    users[doc.id] = pd.DataFrame(columns=['week_number', 'mets_reached', 'mets_goal'])
    goalHistory = store.collection('users', doc.id, 'goals')
    lastWeek[doc.id] = 0
    for week in goalHistory.stream():
        users[doc.id] = users[doc.id].append(pd.Series(week.to_dict()), ignore_index=True)
        if(lastWeek[doc.id] < int(week.id)):
            lastWeek[doc.id] = int(week.id)

for user in users:
    user_df = users[user]
    #Ttrain model and predict goal for next week
    clf=test(models, user_df)
    #predict goal in next week where mets_reached is 100%
    y_pred = clf.predict([[lastWeek[user]+1, 1]])

    print(y_pred)

    doc = lastWeek[user]+1
    store.collection('users', user+"", 'goals').document(str(doc)).set({"week_number": lastWeek[user]+1, "mets_goal": int(y_pred[0])})




