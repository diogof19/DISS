### TODO

- Fix the delete repositories error
- Add bias to model (sample weights with author):
  - Function that changes weight based on an author or list of authors (DONE - NEEDS MORE TESTING)
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
- Add data to the model after an Extract Method is done:
  - Initial part (DONE)
  - Should it be incremental training? Might be too hard. Maybe just retrain the model with the new data after a certain number of refactorings (10)

- Topics/Questions:
  - If no disk, I can get the data with my code, though it won't be as good (need to do it soon) - The disk is coming
  - Incremental training or retrain the model after a certain number of refactorings? - Retrain now & Future work para incremental training
  - Groups of authors with pre-trained models for easy switch? - Yes
  - What to do after finishing the bias? - Future Work
  - I was thinking of getting data from before and after a refactoring to understand if the metrics are doing what they're supposed to do. - Yes
  - Also, checking the metrics on the before version to understand what threshold values are good. - Yes

- Comparar com o extract method do intellij

Trabalho futuro:
- Adicionar Extract Class
- Aumnentar número de dados - script que utilizada recursos de alta computação
- Incremental training (future work escrito na tese pq é muito difícil)
- Synchronize profiles between different machines