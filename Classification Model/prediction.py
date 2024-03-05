from joblib import load

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