import requests
strings = ["api","API","Jepsen","JepsenAPI","swagger","Swagger","Swagger.index","index","132760200576164030","fc716f0b-5bc0-47c7-aa2f-65274a7bd970", "JepsenAPIStore", "ReliableCollectionsWebAPI", "jepsen", "Votes", "VoteData",  "ReliableConcurrentQueue", "ReliableDictionary","ReliableQueue", "ReliableConcurrent"]

hosts = ["http://10.0.0.4:35112", "http://10.0.0.7:35102", "http://10.0.0.4:35847"]
responses = []
for host in hosts:
	url = host
	response = requests.get(url)
	if response.status_code == 200:
		print("URL:   "+ url)
		print("r.sta: "+str(response.status_code))
		print("R.url: "+response.url)
		print("R.txt: "+response.content)
	if response.status_code != 404:
		responses.append(response)
	for i in strings:
		url = host+"/"+i
		response = requests.get(url)
		if response.status_code == 200:
			print("URL:   "+ url)
			print("r.sta: "+str(response.status_code))
			print("R.url: "+response.url)
			print("R.txt: "+response.content)
		if response.status_code != 404:
			responses.append(response)
		for j in strings:
			url = host+"/"+i+"/"+j
			response = requests.get(url)
			if response.status_code == 200:
				print("URL:   "+ url)
				print("r.sta: "+str(response.status_code))
				print("R.url: "+response.url)
				print("R.txt: "+response.content)
			if response.status_code != 404:
				responses.append(response)
			for h in strings:
				url = host+"/"+i+"/"+j+"/"+h
				response = requests.get(url)
				if response.status_code == 200:
					print("URL:   "+ url)
					print("r.sta: "+str(response.status_code))
					print("R.url: "+response.url)
					print("R.txt: "+response.content)
				if response.status_code != 404:
					responses.append(response)
				for g in strings:
					url = host+"/"+i+"/"+j+"/"+h+"/"+g
					response = requests.get(url)
					if response.status_code == 200:
						print("URL:   "+ url)
						print("r.sta: "+str(response.status_code))
						print("R.url: "+response.url)
						print("R.txt: "+response.content)
					if response.status_code != 404:
						responses.append(response)


for i in responses:
 print(i.url)
 print(i.content)

