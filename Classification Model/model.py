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
from sklearn.metrics import make_scorer

DATA_PATH = "C:\\Users\\dluis\\Documents\\Docs\\Universidade\\M 2 ano\\Thesis\\DISS\\LiveRefactoring\\src\\main\\java\\com\\datamining\\data\\extracted_metrics.csv"

def load_data(path):
    df = pd.read_csv(path)
    
    # keeping only the needed features - 1st column (id) and 19-36st columns (after changes metrics)
    df.drop(df.columns[[0, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36]], axis=1, inplace=True)
    
    #print(df.head())
    
    return df

def models(data):
	X = data.values

	X_train, X_test = train_test_split(X, test_size=0.2, random_state=42)
 
	grid_search(X_train)
 
	""" svm_model = OneClassSVM(kernel='rbf', nu=0.1)
	svm_model.fit(X_train)

	isolation_forest_model = IsolationForest(contamination=0.1, random_state=42)
	isolation_forest_model.fit(X_train)
	
	elliptic_envelope_model = EllipticEnvelope(contamination=0.1)
	elliptic_envelope_model.fit(X_train) """
	
	return 

def custom_nof_scorer(estimator, X, y=None):
    if hasattr(estimator, 'decision_function'):
        nof_scores = estimator.decision_function(X)
    elif hasattr(estimator, 'negative_outlier_factor_'):
        nof_scores = -estimator.negative_outlier_factor_
    else:
        raise ValueError("The estimator does not have decision_function or negative_outlier_factor_ attribute.")
    return np.mean(nof_scores)

    
def grid_search(X_train):
    """ param_grid_svm = {
    	'nu': [0.01, 0.1, 0.5],
    	'kernel': ['linear', 'rbf', 'poly']
	}
    
    y_train = [1 for i in range(len(X_train))] # 1 for inliers
    
    svm_grid_search = GridSearchCV(OneClassSVM(), param_grid_svm, cv=5, scoring='recall', verbose=1)
    svm_grid_search.fit(X_train, y_train)
    
    best_svm_model = svm_grid_search.best_estimator_
    best_svm_params = svm_grid_search.best_params_
    
    print(best_svm_model)
    print(best_svm_params) """
    
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
				'contamination': [0.1, 0.2]
			}
		}
	}
    
    kf = RepeatedStratifiedKFold(n_splits=10, n_repeats=10, random_state=0)
    y_train = [1 for i in range(len(X_train))] # 1 for inliers
    
    scores = []
    
    for model_name, mp in model_params.items():
        print(mp['params'])
        grid_search = GridSearchCV(mp['model'],
									param_grid=mp['params'],
									return_train_score=False,
									cv=None,
									n_jobs=-1,
									verbose=1,
         							scoring="recall")
        grid_search.fit(X_train, y_train)
        scores.append({
			'model': model_name,
			'best_score': grid_search.best_score_,
			'best_params': grid_search.best_params_
		})
  
  
  

    


if __name__ == '__main__':
    data = load_data(DATA_PATH)
    models(data)