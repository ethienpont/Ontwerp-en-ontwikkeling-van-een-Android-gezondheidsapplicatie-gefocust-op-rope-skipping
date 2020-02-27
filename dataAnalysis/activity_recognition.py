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
from sklearn.ensemble import RandomForestClassifier
from sklearn.svm import SVC
from sklearn import svm
from sklearn.neural_network import MLPClassifier
from sklearn.metrics import confusion_matrix, classification_report
from sklearn.preprocessing import StandardScaler, LabelEncoder, normalize
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import MultiLabelBinarizer

#DATA QUALITY ASSESSMENT: inconsistent values (convert to same type), duplicate values (remove duplicates), missing values (remove na)

def read_data(file_path, column_names):
    data = pd.read_csv(file_path, sep=';', header=0, skipinitialspace=True)

    #convert nanoseconds to date
    data['time'] = data['time'].apply(convert_to_datetime)
    data['time'] = pd.to_datetime(data['time'])

    #convert to same data type + normalize feature
    for i in range(1, 4):
        data[column_names[i]] = data[column_names[i]].apply(convert_to_float)
        data [column_names[i]] = feature_normalize(data [column_names[i]])

    #drop rows with NaN values
    data.dropna(axis=0, how='any', inplace=True) #TODO: invullen met mean, mod of median / interpolatie

    #drop duplicates
    data.drop_duplicates(subset=None, keep='first', inplace = True)

    #set index to time
    #data.set_index('time', inplace=True)

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

#TODO: 2 methodes geven andere waarden, beter onderzoeken
def get_power_spectral_density(df):
    '''
    psd = []
    for i in range(1, len(df.columns)):
        f, Pxx_den = signal.welch(df.iloc[:,i], 18)
        psd.append(Pxx_den.sum())
    return psd'''
    df_psd = np.abs(df)**2
    return df_psd.sum()

#TODO: datatype is object en niet compex nr
def get_entropy(df):
    entropy = []
    pdf = df / df.sum()
    for i in range (1, len(pdf.columns)):
        entropy.append(np.complex(-np.nansum(pdf.iloc[:,i] * np.log2(pdf.iloc[:,i]))))
    return entropy

def get_energy(df):
    return (np.abs(df)**2).sum()

column_names_org = ['time', 'x', 'y', 'z']

data = read_data('data/smartwatch_data.csv', column_names_org)

column_names = ["start", "end", "x_mean", "y_mean", "z_mean", "x_min", "y_min", "z_min", "x_max", "y_max", "z_max",
                "x_std", "y_std", "z_std", "x_med", "y_med", "z_med", "sma", "smv", "tilt", "x_psd", "y_psd", "z_psd",
                "x_entropy", "y_entropy", "z_entropy" , "x_energy", "y_energy", "z_energy"]
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
                              , "x_entropy" : entropy[0], "y_entropy" : entropy[1], "z_entropy" : entropy[2], "x_energy" : energy['x_f'], "y_energy" : energy['y_f'], "z_energy" : energy['z_f']}), ignore_index=True)

#print_columns(df, column_names)
print(df.head())