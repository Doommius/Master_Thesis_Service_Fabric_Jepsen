sudo apt update
sudo apt upgrade
sudo apt install -y leiningen expect iproute2
echo "SSH KEYS: Start"
if [ -f "/home/jervelund/jepsen.sf/SF_cluster.key" ]; then
    echo "SSH KEYS: Key exists. assuming ssh keys are already installed"
    echo "SSH KEYS: Aassuming ssh keys are already installed"
else
    echo "SSH KEYS: KEY Generating SSH key and installing it to cluster."
    expect gen_install_sshkey.expect
fi
echo "SSH KEYS: Complete"

echo "Jepsen Test: Start"
lein run test --workload "register" --username jervelund --password "P&g68KTBG9&eHxY347sO2^eHa" --ssh-private-key SF_cluster.key --nodes-file resources/nodes

echo "Jepsen Test: END"

echo "Result copy: Starting upload of results"

echo "Result copy: Completed upload of results"