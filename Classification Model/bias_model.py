import pandas as pd
import numpy as np
import sys

from joblib import dump, load
from sklearn.base import clone
from sklearn.model_selection import train_test_split

def bias(authors, model_path, data_path):
    df = pd.read_csv(data_path)
    df['author'] = df['author'].apply(lambda x: 1 if x in authors else 0)
    
    sample_weights = np.where(df['author'] == 1, 2, 1)
    X = df.values
    
    old_model = load(model_path)
    model = clone(old_model)
    
    model.fit(X, sample_weight=sample_weights)
    
    dump(model, model_path)
    return

if __name__ == "__main__":
    model_path = sys.argv[1]
    data_path = sys.argv[2]
    authors = sys.argv[3:]
    
    bias(authors, model_path, data_path)