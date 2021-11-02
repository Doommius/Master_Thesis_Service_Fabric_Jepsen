sudo ssh -i /home/jervelund/.ssh/id_rsa jervelund@20.67.241.178 "cd Clojure_project && bash install_and_run_2.sh"
sudo scp -i /home/jervelund/.ssh/id_rsa -r jervelund@20.67.241.178:Clojure_project/store /mnt/f/thesis/result
sudo cp -r /mnt/f/thesis/ /mnt/f/OneDrive/tmp/