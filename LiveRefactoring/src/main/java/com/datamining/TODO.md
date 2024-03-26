### TODO

- Fix the delete repositories error
- Add bias to model (sample weights with author) - function that changes weight based on an author or list of authors
- Add more ui:
  - A message when anything is done
  - Try to add a progress bar for the Repository Metrics Extraction
  - An error when the python path is not set
  - An error when there is no repository in the path given
  - An error when the branch is not found
  - General errors
  - Selection of authors to bias the model:
    - Not sure if it should be on the existing config page or a new one
    - Maybe a scrollable list of authors with checkboxes
    - Warning that it might take some time to retrain the model
    - An extra button to select all authors (or deselect all)
- Add requirements file for python dependencies and button that would install them