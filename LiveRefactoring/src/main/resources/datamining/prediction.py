from joblib import load
import sys
import warnings
import numpy as np

#TODO: change to the final model
model_path = "src/main/resources/datamining/model.joblib"

def predict(X_test):
    warnings.filterwarnings("ignore", category=UserWarning)
    model = load(model_path)
	
    pred = model.predict(X_test)
 
    return pred[0]

if __name__ == '__main__':
    data = [float(arg) for arg in sys.argv[1:]]

    #Something weird happens when passing the data, so I need to do this (don't really understand why)
    data = np.array(data).reshape(1, -1)

    print(predict(data))