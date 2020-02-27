links = pd.read_csv('data/sensors_links_draaien.csv', sep=';')
rechts = pd.read_csv('data/sensors_rechts_draaien.csv', sep=';')
volledig_links = pd.read_csv('data/sensors_volledig_links.csv', sep=';')
test = volledig_links.loc[:, 'x':'z']
links['target'] = 'links'
rechts['target'] = 'rechts'
df = links.append(rechts)

X = df.loc[:, 'x':'z']
#
Y = MultiLabelBinarizer().fit_transform(df['target'])
print(Y)
clf = MLPClassifier(solver='lbfgs', alpha=1e-5, hidden_layer_sizes=(5,2), random_state=1)
clf.fit(X,Y)
print(clf.predict(test))
#plt.show(df.plot())