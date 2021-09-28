sudo apt update
sudo apt upgrade
sudo apt install -y leiningen expect iproute2 gnuplot
echo "SSH KEYS: Start"
if [ -f "/home/jervelund/SF_cluster.key" ]; then
    echo "SSH KEYS: Key exists. assuming ssh keys are already installed"
    echo "SSH KEYS: Aassuming ssh keys are already installed"
else
    echo "SSH KEYS: KEY Generating SSH key and installing it to cluster."
    expect gen_install_sshkey.expect
fi
echo "SSH KEYS: Complete"

echo "Jepsen Test: Start"
lein run test --workload "reliabledict" --username jervelund --password "P&g68KTBG9&eHxY347sO2^eHa" --ssh-private-key ../SF_cluster.key --nodes-file resources/nodes --concurrency 100 --time-limit 500 -r 1 --ops-per-key 500

echo "Jepsen Test: END"

echo "Result copy: Starting upload of results"

echo "Result copy: Completed upload of results"