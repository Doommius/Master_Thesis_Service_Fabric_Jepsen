echo "Setup: Start"

sudo apt update
sudo apt upgrade

if [ -f "/home/jervelund/.ssh/SF_cluster.key" ]; then
  echo "Setup: Already complete, Skipping"
else
  echo "Setup: Start"
  echo "Install: Installing required packages"
  sudo apt install -y leiningen expect iproute2 gnuplot ca-certificates curl apt-transport-https lsb-release gnupg graphviz
  curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash
  curl -sL https://packages.microsoft.com/keys/microsoft.asc |    gpg --dearmor |    sudo tee /etc/apt/trusted.gpg.d/microsoft.gpg > /dev/null
  sudo apt update
  sudo apt upgrade -y
  AZ_REPO=$(lsb_release -cs)
  echo "deb [arch=amd64] https://packages.microsoft.com/repos/azure-cli/ $AZ_REPO main" |    sudo tee /etc/apt/sources.list.d/azure-cli.list
  sudo apt update
  sudo apt upgrade -y
  sudo apt-get install -y azure-cli
  echo "Install: Complete"
  echo "SSH KEYS: Start"
  echo "SSH KEYS: KEY Generating SSH key and installing it to cluster."
  expect gen_install_sshkey.expect
  echo "SSH KEYS: Complete"
  bash install_packages_remote.sh
  echo "Setup: End"
fi

rm -rf target
lein clean
lein install
echo "Jepsen Test: Start reliableAppend"
lein run test --workload "reliableappend" --username jervelund --password "P&g68KTBG9&eHxY347sO2^eHa" --ssh-private-key ../SF_cluster.key  --nodes-file resources/nodes --concurrency 200 --time-limit 7200 -r 100 --ops-per-key 256 --test-count 1 --max-txn-length 5 --nemesis pause,clock,kill
echo "Jepsen Test: Start reliabledict"
#lein run test --workload "reliabledict" --username jervelund --password "P&g68KTBG9&eHxY347sO2^eHa" --ssh-private-key /home/jervelund/.ssh/SF_cluster.key --nodes-file resources/nodes --concurrency 100 --time-limit 3600 -r 100 --ops-per-key 256 --test-count 1 --nemesis pause,clock
echo "Jepsen Test: Start reliablequeue"
#lein run test --workload "reliablequeue" --username jervelund --password "P&g68KTBG9&eHxY347sO2^eHa" --ssh-private-key /home/jervelund/.ssh/SF_cluster.key --nodes-file resources/nodes --concurrency 200 --time-limit 30 -r 100 --ops-per-key 500 --test-count 1 --nemesis pause,clock
echo "Jepsen Test: END"