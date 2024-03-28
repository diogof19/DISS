### TODO

- Fix the delete repositories error
- Add bias to model (sample weights with author):
  - Function that changes weight based on an author or list of authors (DONE - NEEDS MORE TESTING)
  - Connect the ui to the function
- Add more ui:
  - Try to add a progress bar for the Repository Metrics Extraction
- Selection of authors to bias the model:
  - Connect to the prediction model to retrain it (DONE - NEEDS MORE TESTING)
  - Possibly add the possibility to create a pre-determined profile with a set of authors:
    - We could limit the number of profiles and keep trained models for each (though it would require incremental training for all of them at any commit)
    - ONLY MAKES SENSE IF TRAINED MODEL ISN'T TOO BIG
- Python dependencies:
  - Add requirements file and button that would install them
  - A test to check if the dependencies are installed - if not, show a message with the ones that are missing (maybe a button to install only those)
  - A tooltip to show all the dependencies needed
- After having the data:
  - Check for feature selection
  - Covariance matrix (EllipticEnvelope)