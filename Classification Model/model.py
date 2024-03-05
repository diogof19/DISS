import pandas as pd
import numpy as np

from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.svm import OneClassSVM
from sklearn.ensemble import IsolationForest
from sklearn.covariance import EllipticEnvelope
from sklearn.model_selection import GridSearchCV
from imblearn.pipeline import Pipeline
from sklearn.model_selection import RepeatedStratifiedKFold
from sklearn.metrics import make_scorer, fbeta_score
from joblib import dump, load

DATA_PATH = "C:\\Users\\dluis\\Documents\\Docs\\Universidade\\M 2 ano\\Thesis\\DISS\\LiveRefactoring\\src\\main\\java\\com\\datamining\\data\\extracted_metrics.csv"

def load_data(path):
    df = pd.read_csv(path)
    
    # keeping only the needed features - 1st column (id) and 19-36st columns (after changes metrics)
    df.drop(df.columns[[0, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36]], axis=1, inplace=True)
    
    return df

def models(data):
	X = data.values

	X_train, X_test = train_test_split(X, test_size=0.2, random_state=42)
	
	#TODO: Add feature selection and (maybe) scaling - standard scaler was not working well with elliptic envelope
 
	df = grid_search(X_train)
 
	one_class_svm = OneClassSVM(kernel=df.iloc[0]['best_params']['kernel'], nu=df.iloc[0]['best_params']['nu'], gamma=df.iloc[0]['best_params']['gamma'])
	isolation_forest = IsolationForest(contamination=df.iloc[1]['best_params']['contamination'], n_estimators=df.iloc[1]['best_params']['n_estimators'], max_samples=df.iloc[1]['best_params']['max_samples'], max_features=df.iloc[1]['best_params']['max_features'])
	elliptic_envelope = EllipticEnvelope(contamination=df.iloc[2]['best_params']['contamination'])
	
	one_class_svm.fit(X_train)
	isolation_forest.fit(X_train)
	elliptic_envelope.fit(X_train)
 
	dump(one_class_svm, 'models/one_class_svm.joblib')
	dump(isolation_forest, 'models/isolation_forest.joblib')
	dump(elliptic_envelope, 'models/elliptic_envelope.joblib')
	
	return 
    
def grid_search(X_train):
    
    #TODO: change to the actual parameters
    """ Actual Parameters to be used in the models
    model_params = {
		'OneClassSVM': {
			'model': OneClassSVM(),
			'params': {
				'kernel': ['rbf', 'linear', 'poly', 'sigmoid'],
				'nu': [0.1, 0.2, 0.3, 0.4, 0.5],
				'gamma': ['scale', 'auto']
			}
		},
		'IsolationForest': {
			'model': IsolationForest(),
			'params': {
				'contamination': [0.1, 0.2],
				'n_estimators': [50, 100, 200, 300, 400, 500],
				'max_samples': [100, 200, 300, 400, 500],
				'max_features': [1, 2, 3, 4, 5]
			}
		},
		'EllipticEnvelope': {
			'model': EllipticEnvelope(),
			'params': {
				'contamination': [0.01, 0.02, 0.03, 0.04, 0.05]
			}
		}
	} """
 
	# Testing Parameters
    model_params = {
		'OneClassSVM': {
			'model': OneClassSVM(),
			'params': {
				'kernel': ['rbf', 'linear'],
				'nu': [0.1, 0.2],
				'gamma': ['scale', 'auto']
			}
		},
		'IsolationForest': {
			'model': IsolationForest(),
			'params': {
				'contamination': [0.1, 0.2],
				'n_estimators': [50, 100],
				'max_samples': [100, 200],
				'max_features': [1, 2]
			}
		},
		'EllipticEnvelope': {
			'model': EllipticEnvelope(),
			'params': {
				'contamination': [0.01, 0.02]
			}
		}
	}
    
    kf = RepeatedStratifiedKFold(n_splits=5, n_repeats=5, random_state=0)
    y_train = [1 for i in range(len(X_train))] # 1 for inliers
    
    scores = []
    
    f2_score = make_scorer(fbeta_score, beta=2, pos_label=1)
    
    for model_name, mp in model_params.items():
        grid_search = GridSearchCV(mp['model'],
									param_grid=mp['params'],
									return_train_score=False,
									cv=kf,
									n_jobs=-1,
									verbose=True,
         							scoring=f2_score)
        grid_search.fit(X_train, y_train)
        scores.append({
			'model': model_name,
			'best_score': grid_search.best_score_,
			'best_params': grid_search.best_params_
		})
    
    df = pd.DataFrame(scores, columns=['model', 'best_score', 'best_params'])
    
    return df

def predict(X_test):
    #TODO: change to the final model
	one_class_svm = load('models/one_class_svm.joblib')
	
	one_class_svm_pred = one_class_svm.predict(X_test)
 
	return one_class_svm_pred
  

if __name__ == '__main__':
    #data = load_data(DATA_PATH)
    #models(data)
    inputData = []
    for i in range(18):
        inputData.append(1)
    
    print(predict([inputData]))