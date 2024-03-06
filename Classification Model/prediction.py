from joblib import load
import warnings

def predict(X_test):
    #TODO: change to the final model
    
    warnings.filterwarnings("ignore", category=UserWarning)
    one_class_svm = load("C:\\Users\\dluis\\Documents\\Docs\\Universidade\\M 2 ano\\Thesis\\DISS\\Classification Model\\models\\one_class_svm.joblib")
	
    one_class_svm_pred = one_class_svm.predict(X_test)
 
    return one_class_svm_pred[0]

if __name__ == '__main__':
    inputData = []
    for i in range(18):
        inputData.append(1)
    
    print(predict([inputData]))