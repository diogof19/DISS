from joblib import load
import sys
import warnings
import numpy as np

def predict(X, model_path, scaler_path):
    warnings.filterwarnings("ignore", category=UserWarning)

    scaler = load(scaler_path)
    scaled_X = scaler.transform(X.reshape(1, -1))

    model = load(model_path)
	
    pred = model.predict(scaled_X)
 
    return pred[0]

if __name__ == '__main__':
    model_path = sys.argv[1]
    scaler_path = sys.argv[2]
    data = [float(arg) for arg in sys.argv[3:]]

    #Something weird happens when passing the data, so I need to do this (don't really understand why)
    data = np.array(data).reshape(1, -1)

    print(predict(data, model_path, scaler_path))