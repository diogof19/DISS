import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.svm import OneClassSVM
from sklearn.ensemble import IsolationForest
from sklearn.covariance import EllipticEnvelope
from sklearn.model_selection import GridSearchCV
from imblearn.pipeline import Pipeline
from sklearn.model_selection import RepeatedStratifiedKFold

DATA_PATH = "C:\\Users\\dluis\\Documents\\Docs\\Universidade\\M 2 ano\\Thesis\\DISS\\LiveRefactoring\\src\\main\\java\\com\\datamining\\data\\extracted_metrics.csv"

def load_data(path):
    df = pd.read_csv(path)
    
    # keeping only the needed features - 1st column (id) and 19-36st columns (after changes metrics)
    df.drop(df.columns[[0, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36]], axis=1, inplace=True)
    
    return df

def models(data):
	X = data.values

	X_train, X_test = train_test_split(X, test_size=0.2, random_state=42)

	scaler = StandardScaler()
	X_train_scaled = scaler.fit_transform(X_train)

	svm_model = OneClassSVM(kernel='rbf', nu=0.1)
	svm_model.fit(X_train_scaled)

	isolation_forest_model = IsolationForest(contamination=0.1, random_state=42)
	isolation_forest_model.fit(X_train)

	elliptic_envelope_model = EllipticEnvelope(contamination=0.1)
	elliptic_envelope_model.fit(X_train_scaled)
	
	return 
    
    
def grid_search():
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
				'contamination': [0.0],
				'n_estimators': [50, 100, 200, 300, 400, 500],
				'max_samples': [100, 200, 300, 400, 500],
				'max_features': [1, 2, 3, 4, 5]
			}
		},
		'EllipticEnvelope': {
			'model': EllipticEnvelope(),
			'params': {
				'contamination': [0.0]
			}
		}
	}
    
    kf = RepeatedStratifiedKFold(n_splits=10, n_repeats=10, random_state=0)
    
    scores = []
    
    for model_name, mp in model_params.items():
        pipeline = Pipeline([('classifier', mp['model'])])
        grid_search = GridSearchCV(pipeline,
									param_grid=mp['params'],
									return_train_score=False,
									cv=kf,
									n_jobs=-1,
									verbose=1)
        grid_search.fit(X_train, None)
        scores.append({
			'model': model_name,
			'best_score': grid_search.best_score_,
			'best_params': grid_search.best_params_
		})
  

    


if __name__ == '__main__':
    data = load_data(DATA_PATH)
    models(data)