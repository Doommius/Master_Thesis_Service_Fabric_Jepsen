ssh -i /home/jervelund/.ssh/id_rsa jervelund@20.82.138.50 "cd /tmp/ && ls -tp | grep -v /$ | head -1"
ssh -i /home/jervelund/.ssh/id_rsa jervelund@20.82.138.50 "cd /tmp/ &&  ls -tp | grep -v /$ | head -1 | xargs cat"
