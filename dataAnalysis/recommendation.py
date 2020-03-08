import firebase_admin
from Tools.scripts.treesync import raw_input
from firebase_admin import credentials
from firebase_admin import firestore
import pandas as pd
from datetime import datetime
import numpy as np

'''
#connection with firestore
cred = credentials.Certificate("./healthrecommendersystems-firebase-adminsdk-psl7m-6f276b3d23.json")
app = firebase_admin.initialize_app(cred)

store = firestore.client()

users_ref = store.collection(u'users')
docs = users_ref.stream()

#dict with dataframes for al the users
users = {}

for doc in docs:
    users[doc.id] = pd.DataFrame(columns=['start', 'end', 'activity', 'mets_score'])
    sessionHistory = store.collection('users', doc.id, 'sessions')
    for s in sessionHistory.stream():
        users[doc.id] = users[doc.id].append(pd.Series(s.to_dict()), ignore_index=True)

#maak voor elke user recommendations
for user in users:
    #bereken aantal van elke activiteit
    print(user)
'''
import json
import httplib2

from datetime import datetime
from googleapiclient.discovery import build
from oauth2client.client import OAuth2WebServerFlow

# Copy your credentials from the Google Developers Console
CLIENT_ID = '726533384380-33ji70a800udhknqghrl09eea11s8ms2.apps.googleusercontent.com'
CLIENT_SECRET = 'zH8viPrqLRNTy-K_R_ZmdsW3'

# Check https://developers.google.com/fit/rest/v1/reference/users/dataSources/datasets/get
# for all available scopes
OAUTH_SCOPE = 'https://www.googleapis.com/auth/fitness.activity.read'

# DATA SOURCE
DATA_SOURCE = "derived:com.google.step_count.delta:com.google.android.gms:estimated_steps"

# The ID is formatted like: "startTime-endTime" where startTime and endTime are
# 64 bit integers (epoch time with nanoseconds).
DATA_SET = "1051700038292387000-1451700038292387000"

# Redirect URI for installed apps
REDIRECT_URI = 'urn:ietf:wg:oauth:2.0:oob'

def retrieve_data():
    """
    Run through the OAuth flow and retrieve credentials.
    Returns a dataset (Users.dataSources.datasets):
    https://developers.google.com/fit/rest/v1/reference/users/dataSources/datasets
    """
    flow = OAuth2WebServerFlow(CLIENT_ID, CLIENT_SECRET, OAUTH_SCOPE, REDIRECT_URI)
    authorize_url = flow.step1_get_authorize_url()
    print('Go to the following link in your browser:')
    print(authorize_url)
    code = raw_input('Enter verification code: ').strip()
    credentials = flow.step2_exchange(code)

    # Create an httplib2.Http object and authorize it with our credentials
    http = httplib2.Http()
    http = credentials.authorize(http)

    fitness_service = build('fitness', 'v1', http=http)

    return http.request("https://www.googleapis.com/fitness/v1/users/me/sessions")

print(retrieve_data().decode())