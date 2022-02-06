sudo ssh -i /home/jervelund/.ssh/id_rsa jervelund@20.82.138.50 "rm -rf Clojure_project"
sudo scp -r -i /home/jervelund/.ssh/id_rsa /mnt/f/OneDrive/Dokumenter/GitHub/thesis/Clojure_project/ jervelund@20.82.138.50:Clojure_project
sudo ssh -i /home/jervelund/.ssh/id_rsa jervelund@20.82.138.50 "cd Clojure_project && lein test"