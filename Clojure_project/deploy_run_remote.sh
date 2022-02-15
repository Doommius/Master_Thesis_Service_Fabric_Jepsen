ssh jervelund@20.123.40.2 "rm -rf Clojure_project"
scp -r /mnt/f/OneDrive/Dokumenter/GitHub/thesis/Clojure_project/ jervelund@20.123.40.2:Clojure_project
ssh jervelund@20.123.40.2 "cd Clojure_project && bash install_and_run_2.sh"
sudo scp -r jervelund@20.123.40.2:Clojure_project/store /mnt/f/thesis/result