import pandas as pd
import numpy as np
import math
import scipy.integrate as integrate
import scipy.fftpack as FFT
from scipy import signal
import statistics
from datetime import datetime
import seaborn as sns
import matplotlib.pyplot as plt
from sklearn.ensemble import RandomForestClassifier, ExtraTreesClassifier, AdaBoostClassifier, \
    GradientBoostingClassifier
from sklearn.linear_model import SGDClassifier
from sklearn.naive_bayes import GaussianNB, MultinomialNB
from sklearn.neighbors import KNeighborsClassifier
from sklearn.svm import SVC, LinearSVC
from sklearn import svm
from sklearn.neural_network import MLPClassifier
from sklearn.metrics import confusion_matrix, classification_report
from sklearn.preprocessing import StandardScaler, LabelEncoder, normalize
from sklearn.model_selection import train_test_split
from sklearn import preprocessing
from sklearn import svm
from sklearn.preprocessing import PolynomialFeatures
from sklearn.preprocessing import MultiLabelBinarizer
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import GridSearchCV

#DATA QUALITY ASSESSMENT: inconsistent values (convert to same type), duplicate values (remove duplicates), missing values (remove na)
from sklearn.tree import DecisionTreeClassifier


def read_data(file_path, column_names):
    data = pd.read_csv(file_path, sep=';', header=0, skipinitialspace=True)

    #convert nanoseconds to date
    data['time'] = data['time'].apply(convert_to_datetime)
    data['time'] = pd.to_datetime(data['time'])

    #convert to same data type + normalize feature
    for i in range(1, 4):
        data[column_names[i]] = data[column_names[i]].apply(convert_to_float)
        #data [column_names[i]] = feature_normalize(data [column_names[i]])

    #drop rows with NaN values
    data.dropna(axis=0, how='any', inplace=True) #TODO: invullen met mean, mod of median / interpolatie

    #drop duplicates
    data.drop_duplicates(subset=None, keep='first', inplace = True)

    #TODO: remove test activity
    data['activity'] = np.random.choice(['other', 'rope'], data.shape[0])

    return data

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


def sampling_frequency(df):
    samples = []
    sec = 1000000000 #1/(math.pow(10,-9))
    for index, row in df.iterrows():
        time = row['time']
        interval = df[(df['time'] < (time+sec)) & (df['time'] >= time)]
        if((interval.iloc[-1]['time'] - interval.iloc[0]['time']) > 0.93*sec):
            samples.append(interval.shape[0])
    return statistics.mean(samples)

def feature_normalize(df):
    mu = np.mean(df,axis = 0)
    sigma = np.std(df,axis = 0)
    return (df - mu)/sigma

#generate windows with 50% overlap
def windows(df, time):
    start = df[0]
    while start < df.iloc[-1]:
        yield start, (start + pd.to_timedelta(time, unit='s'))
        start += pd.to_timedelta(time/2, unit='s')

#list of segments
def segment_signal(data,time = 10):
    segments = []
    #labels = np.empty((0))
    for (start, end) in windows(data['time'], time):
        vw1 = data['time'] >= start
        vw2 = data['time'] < end
        segments.append(data[vw1 & vw2].loc[:,'x':'z'].to_numpy())
        #print(np.dstack([x,y,z]))
        #segments = np.vstack([segments,np.dstack([x,y,z])])
        #labels = np.append(labels,stats.mode(data["activity"][start:end])[0][0])
    return segments

#missing values (null)
def draw_missing_data_table(df):
    total = df.isnull().sum().sort_values(ascending=False)
    percent = (df.isnull().sum()/df.isnull().count()).sort_values(ascending=False)
    missing_data = pd.concat([total, percent], axis=1, keys=['Total', 'Percent'])
    return missing_data

def print_columns(df, column_names):
    for i in range(0, len(column_names)):
        print(df[column_names[i]])


#feature extraction
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

#TODO: scipy integrate
#TODO: voor elke as apart??
#measure of activity level (m/s²)
def get_signal_magnitude_area(df):
        sum = 0
        for i in range(0, len(df)):
            sum += (abs(df['x'].iloc[i]) + abs(df['y'].iloc[i]) + abs(df['z'].iloc[i]))
        return sum /len(df)
    #result = integrate.quad(lambda t: df['x'].apply(lambda n : abs(n)) + df['y'].apply(lambda n : abs(n)) + df['z'].apply(lambda n : abs(n)), 0, len(df))

#TODO: voor elke as apart??
#degree of movement intensity (m/s²)
def get_signal_magnitude_vector(df):
    sum = 0
    for i in range(0, len(df)):
        sum += math.sqrt(df['x'].iloc[i] * df['x'].iloc[i] + df['y'].iloc[i] * df['y'].iloc[i] + df['z'].iloc[i] * df['z'].iloc[i])
    return sum

#average angle (radian) between accelerometer vector and x as (parallel with arm)
def get_tilt_angle(df):
    df_cos = pd.DataFrame(columns=["tilt_ang"])
    df_dot = df['x']
    for i in range(0, len(df)):
        s = pd.Series({"tilt_ang" : (df_dot.iloc[i])/(math.sqrt(df['x'].iloc[i]*df['x'].iloc[i] + df['y'].iloc[i]*df['y'].iloc[i] + df['z'].iloc[i]*df['z'].iloc[i]))})
        df_cos=df_cos.append(s, ignore_index=True)
    df_angle = np.arccos(df_cos)
    return df_angle.mean()['tilt_ang']

'''
#TODO: 2 methodes geven andere waarden, beter onderzoeken
def get_power_spectral_density(df):
    df_psd = np.abs(df)**2
    return df_psd.sum()

#TODO: datatype is object en niet compex nr
def get_entropy(df):
    entropy = []
    pdf = df / df.sum()
    for i in range (1, len(pdf.columns)):
        entropy.append(np.complex(-np.nansum(pdf.iloc[:,i] * np.log2(pdf.iloc[:,i]))))
    return entropy


column_names_org = ['time', 'x', 'y', 'z']

data = read_data('data/smartwatch_data.csv', column_names_org)

column_names = ["start", "end", "x_mean", "y_mean", "z_mean", "x_min", "y_min", "z_min", "x_max", "y_max", "z_max",
                "x_std", "y_std", "z_std", "x_med", "y_med", "z_med", "sma", "smv", "tilt", "x_psd", "y_psd", "z_psd",
                "x_entropy", "y_entropy", "z_entropy" , "x_energy", "y_energy", "z_energy", "activity"]
df = pd.DataFrame(columns = column_names)

for (start, end) in windows(data['time'], 10):
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

    #fourrier transform
    t_x = data[vw1 & vw2][['time','x']].set_index('time')
    t_y = data[vw1 & vw2][['time','y']].set_index('time')
    t_z = data[vw1 & vw2][['time', 'z']].set_index('time')

    df_f = pd.DataFrame(columns=['f', 'x_f', 'y_f', 'z_f'])

    #TODO: determine sampling rate (datapoints per second)
    sampling_rate = 18

    df_f['x_f'] = FFT.fft(t_x).ravel()
    df_f['y_f'] = FFT.fft(t_y).ravel()
    df_f['z_f'] = FFT.fft(t_z).ravel()
    df_f['f'] = FFT.fftfreq(len(df_f['x_f'])) * sampling_rate

    psd = get_power_spectral_density(df_f)
    entropy = get_entropy(df_f)
    energy = get_energy(df_f)

    df = df.append(pd.Series({'start': start, 'end': end, 'x_mean': mean['x'], 'y_mean': mean['y'], 'z_mean': mean['z'], "x_min" : min['x'],
                              "y_min" : min['y'], "z_min" : min['z'], "x_max" : max['x'], "y_max" : max['y'], "z_max" : max['z'],
                              "x_std" : std['x'], "y_std" : std['y'], "z_std" : std['z'], "x_med" : med['x'], "y_med" : med['y'], "z_med" : med['z'],
                              "sma" : sma, "smv" : smv, "tilt" : tilt, "x_psd" : psd['x_f'], "y_psd" : psd['y_f'], "z_psd" : psd['z_f']
                              , "x_entropy" : entropy[0], "y_entropy" : entropy[1], "z_entropy" : entropy[2],
                              "x_energy" : energy['x_f'], "y_energy" : energy['y_f'], "z_energy" : energy['z_f'], "activity" : data.loc[0,'activity']}), ignore_index=True)
'''
#print_columns(df, column_names)

column_names_org = ['time', 'x', 'y', 'z']

data = read_data('data/smartwatch_data.csv', column_names_org)

column_names = ["start", "end", "x_mean", "y_mean", "z_mean", "x_min", "y_min", "z_min", "x_max", "y_max", "z_max",
                "x_std", "y_std", "z_std", "x_med", "y_med", "z_med", "activity"]
df = pd.DataFrame(columns = column_names)

for (start, end) in windows(data['time'], 1):
    vw1 = data['time'] >= start
    vw2 = data['time'] < end
    mean = get_mean_window(data[vw1 & vw2])
    min = get_min_window(data[vw1 & vw2])
    max = get_max_window(data[vw1 & vw2])
    std = get_std_window(data[vw1 & vw2])
    med = get_med_window(data[vw1 & vw2])
    #sma = get_signal_magnitude_area(data[vw1 & vw2])
    #smv = get_signal_magnitude_vector(data[vw1 & vw2])
    #tilt = get_tilt_angle(data[vw1 & vw2])
    activity = np.random.choice(['other', 'rope'], 1)
    df = df.append(pd.Series(
        {'start': start, 'end': end, 'x_mean': mean['x'], 'y_mean': mean['y'], 'z_mean': mean['z'], "x_min": min['x'],
         "y_min": min['y'], "z_min": min['z'], "x_max": max['x'], "y_max": max['y'], "z_max": max['z'],
         "x_std": std['x'], "y_std": std['y'], "z_std": std['z'], "x_med": med['x'], "y_med": med['y'],
         "z_med": med['z'], "activity": activity[0]}), ignore_index=True)

#TODO: time belangrijk???
df.drop(['start', 'end'], axis=1, inplace=True)

print(df)

#label activity
le = preprocessing.LabelEncoder()
df['activity'] = le.fit_transform(df['activity'])
print(df)

X_train, X_test, y_train, y_test = train_test_split(df.drop('activity', axis=1), df['activity'], test_size = 0.33, random_state = 42)

#features generating
#TODO: dataset too big???
'''
poly = PolynomialFeatures(X_train.shape[1])
poly.fit_transform(X_train)
'''
#TODO: ook y schalen
scaler = preprocessing.StandardScaler().fit(X_train)
X_train = pd.DataFrame(scaler.transform(X_train), columns=column_names[2:-1])
X_test = pd.DataFrame(scaler.transform(X_test), columns=column_names[2:-1])

#SVM/SVC
'''
#hyperparameter tuning
param_grid = {'C': [0.1,1, 10, 100], 'gamma': [1,0.1,0.01,0.001],'kernel': ['linear','rbf', 'poly', 'sigmoid']}
grid = GridSearchCV(SVC(),param_grid,refit=True,verbose=2)
grid.fit(X_train,y_train)
print(grid.best_estimator_)

svm_model = SVC(kernel='linear', C=1).fit(X_train, y_train)
y_pred = svm_model.predict(X_test)
accuracy = svm_model.score(X_test, y_test)
cm = confusion_matrix(y_test, y_pred)
'''

#linear SVC
'''
#hyperparameter tuning
param_grid = {'C': [0.1,1, 10, 100], 'penalty': ['l1', 'l2'], 'loss': ['hinge', 'squared_hinge'],'multi_class': ['ovr','crammer_singer']}
grid = GridSearchCV(LinearSVC(),param_grid,refit=True,verbose=2)
grid.fit(X_train,y_train)
print(grid.best_estimator_)

clf = LinearSVC(random_state=0, tol=1e-5)
clf.fit(X_train, y_train)
y_pred = clf.predict(X_test)
cm = confusion_matrix(y_test, y_pred)
print(y_pred)
'''

#random forest classifier
'''
#hyperparameter tuning
param_grid = {'max_features': ['sqrt', 'log2', None], 'n_estimators': [1, 10, 100, 1000, 10000],
              'max_depth': [range(1,32)],'min_samples_split': [np.arange(0.10, 1, 0.1)] ,
              'bootstrap': [True, False] ,'oob_score': [True, False],'max_leaf_nodes': [1, 10, 100, 1000, None],'min_samples_leaf': [np.arange(0.10, 1, 0.1)]}
grid = GridSearchCV(RandomForestClassifier(),param_grid,refit=True,verbose=2)
grid.fit(X_train,y_train)
print(grid.best_estimator_)

clf = RandomForestClassifier(n_estimators=10, max_features="sqrt")
clf = clf.fit(X_train, y_train)
y_pred = clf.predict(X_test)
cm = confusion_matrix(y_test, y_pred)
print(y_pred)
'''

#extra trees classifier
'''
#hyperparameter tuning
param_grid = {'criterion': ['gini', 'entropy'], 'n_estimators': [1, 10, 100, 1000, 10000],
              'max_depth': [range(1,32)],'min_samples_split': [np.arange(0.10, 1, 0.1)] ,
              'bootstrap': [True, False] ,'oob_score': [True, False],'max_leaf_nodes': [1, 10, 100, 1000, None],'min_samples_leaf': [np.arange(0.10, 1, 0.1)]}
grid = GridSearchCV(ExtraTreesClassifier(),param_grid,refit=True,verbose=2)
grid.fit(X_train,y_train)
print(grid.best_estimator_)

clf = ExtraTreesClassifier(max_features="sqrt" ,n_estimators=10, max_depth=None,min_samples_split=2, random_state=0)
clf = clf.fit(X_train, y_train)
y_pred = clf.predict(X_test)
cm = confusion_matrix(y_test, y_pred)
print(y_pred)
'''

#Adaboost
'''
#hyperparameter tuning
param_grid = {'learning_rate': [np.arange(0.10, 1, 0.1)], 'n_estimators': [1, 10, 50, 100, 1000, 10000],
              'max_depth': [range(1,32)], 'base_estimator': ['XGboost', 'GBM', 'RandomForest', 'ExtraTree'], 
              'algorithm' : ['SAMME', 'SAMME.R']}
grid = GridSearchCV(AdaBoostClassifier(),param_grid,refit=True,verbose=2)
grid.fit(X_train,y_train)
print(grid.best_estimator_)

bdt_real = AdaBoostClassifier(
    DecisionTreeClassifier(max_depth=2),
    n_estimators=600,
    learning_rate=1)

bdt_discrete = AdaBoostClassifier(
    DecisionTreeClassifier(max_depth=2),
    n_estimators=600,
    learning_rate=1.5,
    algorithm="SAMME")

bdt_real.fit(X_train, y_train)
bdt_discrete.fit(X_train, y_train)
y_pred_real = bdt_real.predict(X_test)
y_pred_discrete = bdt_discrete.predict(X_test)
cm_real = confusion_matrix(y_test, y_pred_real)
cm_discrete = confusion_matrix(y_test, y_pred_discrete)
print(y_pred_discrete)
print(y_pred_real)
'''

#TODO: multiclass??? + welke variant van naive bayes??? A. Gaussian want continu?
#gaussian naive bayes
'''
gnb = GaussianNB()
gnb.fit(X_train, y_train)
y_pred = gnb.predict(X_test)
cm = confusion_matrix(y_test, y_pred)
print(y_pred)
'''

#TODO: multiclass??
#KNEIGHBORS CLASSIFIER
#hyperparameter tuning
'''
param_grid = {'n_neighbors': [range(1,10)], 'weights': ['uniform', 'distance'],
              'algorithm': ['ball_tree', 'kd_tree', 'brute'], 'leaf_size': [range(1,60)], 
              'metric' : ['minkowski']}
grid = GridSearchCV(KNeighborsClassifier(),param_grid,refit=True,verbose=2)
grid.fit(X_train,y_train)
print(grid.best_estimator_)

clf = KNeighborsClassifier(1, weights='uniform')
clf.fit(X_train, y_train)
y_pred = clf.predict(X_test)
cm = confusion_matrix(y_test, y_pred)
print(y_pred)
'''

#TODO: multiclass???
#SGD CLASSIFIER
#hyperparameter tuning
'''
param_grid = {'alpha': [1e-7, 1e-6, 1e-5, 1e-4, 1e-3, 1e-2, 1e-1], 'learning_rate': ['constant', 'optimal', 'invscaling', 'adaptive'], 'max_iter': [1, 10, 100, 1000, np.ceil(10**6 / n)], 
    'loss': ['log', 'hinge', 'modified_huber', 'squared_hinge'], 'penalty': ['l2', 'l1', 'elasticnet'],
    'shuffle' : [True]}
grid = GridSearchCV(SGDClassifier(),param_grid,refit=True,verbose=2)
grid.fit(X_train,y_train)
print(grid.best_estimator_)

clf = SGDClassifier(loss="hinge", penalty="l2", max_iter=5)
clf.fit(X_train, y_train)
y_pred = clf.predict(X_test)
cm = confusion_matrix(y_test, y_pred)
print(y_pred)
'''

#neural network
#hyperparameter tuning
'''
param_grid = {'solver': ['lbfgs', 'sgd', 'adam'], 'alpha': [1e-4, 1e-3, 1e-2, 1e-1, 1e0, 1e1, 1e2, 1e3], 
    'hidden_layer_sizes': [(50,50,50), (50,100,50), (100,)], 'activation': ['identity', 'logistic','tanh', 'relu'],
    'learning_rate': ['invscaling', 'constant','adaptive']}
grid = GridSearchCV(MLPClassifier(),param_grid,refit=True,verbose=2)
grid.fit(X_train,y_train)
print(grid.best_estimator_)

clf = MLPClassifier(solver='lbfgs', alpha=1e-5, hidden_layer_sizes=(5, 2), random_state=1)
clf.fit(X_train, y_train)
y_pred = clf.predict(X_test)
cm = confusion_matrix(y_test, y_pred)
print(y_pred)
'''