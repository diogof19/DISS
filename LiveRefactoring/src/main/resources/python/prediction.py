from joblib import load
import sys
import warnings
import numpy as np

def predict(X_test, model_path):
    warnings.filterwarnings("ignore", category=UserWarning)
    model = load(model_path)
	
    pred = model.predict(X_test)
 
    return pred[0]

if __name__ == '__main__':
    model_path = sys.argv[1]
    data = [float(arg) for arg in sys.argv[2:]]

    #Something weird happens when passing the data, so I need to do this (don't really understand why)
    data = np.array(data).reshape(1, -1)

    print(predict(data, model_path))