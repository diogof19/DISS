### TODO

- Fix the delete repositories error
- Add bias to model (sample weights with author) - function that changes weight based on an author or list of authors (DONE - NEEDS MORE TESTING)
- Add more ui:
  - A message when anything is done
  - Try to add a progress bar for the Repository Metrics Extraction
  - An error when the python path is not set
  - An error when there is no repository in the path given
  - An error when the branch is not found
  - General errors
- Selection of authors to bias the model:
  - Connect to the prediction model to retrain it (DONE - NEEDS MORE TESTING)
- Add requirements file for python dependencies and button that would install them
- After having the data:
  - Check for feature selection
  - Covariance matrix (EllipticEnvelope)