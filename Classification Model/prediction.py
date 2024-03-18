from joblib import load
import warnings
import numpy as np

model_path = 'C:\\Users\\dluis\\Documents\\Docs\\Universidade\\M 2 ano\\Thesis\\DISS\\LiveRefactoring\\src\\main\\resources\\datamining\\model.joblib'

def predict(X_test):
    warnings.filterwarnings("ignore", category=UserWarning)
    model = load(model_path)
	
    pred = model.predict(X_test)
 
    return pred[0]

if __name__ == '__main__':
    data = [0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0]
    
    print(predict([data]))
    
    
    