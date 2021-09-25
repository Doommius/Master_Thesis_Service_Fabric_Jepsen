ssh -i /home/jervelund/.ssh/id_rsa jervelund@20.67.241.178 "cd /tmp/ && ls -tp | grep -v /$ | head -1"
ssh -i /home/jervelund/.ssh/id_rsa jervelund@20.67.241.178 "cd /tmp/ &&  ls -tp | grep -v /$ | head -1 | xargs cat"
