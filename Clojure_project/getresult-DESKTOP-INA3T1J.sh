sudo scp -r -i /home/jervelund/.ssh/id_rsa  jervelund@20.67.241.178:Clojure_project/store /mnt/f/OneDrive/Dokumenter/GitHub/thesis/result/
sudo ssh -i /home/jervelund/.ssh/id_rsa jervelund@20.67.241.178 "rm -rf Clojure_project/store"