import os
import subprocess
import binascii
import time
import constants
def getHex(i):
	by=i.encode('utf-8')
	#res=codecs(by,"hex")
	res=binascii.hexlify(by)
	return res.decode('utf-8')
def saveIntoXml(name,password):
	newfile=[]
	f=open("prototype.txt")
	for i in f:
		if("XXX" in i):
			j=i
			j=j.replace("XXX",name)
			newfile.append(j)
		elif("YYY" in i):
			j=i
			j=j.replace("YYY",password)
			newfile.append(j)
		elif("HHH" in i):
			j=i
			j=j.replace("HHH",getHex(name))
			newfile.append(j)
		else:
			newfile.append(i)
	f.close()
	f=open("temp.xml","wb")
	for i in newfile:
		f.write(i.encode('utf-8'))
	f.close()
	return saveInProfile(name)
	#	print(i)
def delay(i):
	#NOTE :when using tkinter as graphical user interface, sleep() 
	#won't do the job - use after() instead: tkinter.Tk.after
	#(yourrootwindow,60000) or yourrootwindow.after(60000)
	print("Connecting",end="")
	for a in range(i):
		time.sleep(a)
		print(".",end="")
	print("")
def saveInProfile(name):
	try:
		here=0
		res=subprocess.check_output(["netsh","wlan","add","profile","filename=","temp.xml"])
		print(res)
		here=1
		res=connect(name)
		os.remove("temp.xml")
		return res
	except Exception as e:
		#print(e)
		print("connection Unavailable or Wrong Password")
		#removing the profile
		if(here==1):
			res=subprocess.check_output(["netsh","wlan","delete","profile","name=",name])
		
		#deleting the temp file
		os.remove("temp.xml")
		#delete the profile saved
		return constants.CONNECTION_ERROR
def checkConnection(name,iter):
	try:
		res=subprocess.check_output(["netsh","wlan","show","interface"])
		res=res.decode('utf-8')
		#res=res.replace("\r","")
		#print(res)
		res=res.split("\n")
		#print(len(res))
		for a in res:
			j=a.split(":")
			k=j[0].replace(" ","")
			if(k=="SSID"):
				if(name in j[1]):
#					print("Connected")
					return "CONNECTED"
				else:
					print("Problem Occurred")
					print("retrying...")
					iter+=1
					if(iter<=2):
						return checkConnection(name,iter+1)
					else:
						print("Can't Connect")
						return constants.CONNECTION_ERROR
					
#				print(j[0])
	except Exception as e:
		print(e)
		return constants.CONNECTION_ERROR
def connect(i):
	try:
		res=subprocess.check_output(["netsh","wlan","connect","name=",i,"ssid=",i])
	except Exception as e:
		print(e)
		#ask for password
		return "NOPASS"
	res=res.decode('utf-8')
	res=res.replace("\r","")
	if(res in "Connection request was completed successfully.\n"):
		delay(3)
		return checkConnection(i,0)
		
		
	
def scan_wifi():
	scan_result=subprocess.check_output(["netsh","wlan","show","networks"])
	scan_result=scan_result.decode('utf-8')
	scan_result=scan_result.replace("\r","")

	lst=scan_result.split("\n")
	lst=lst[4:]
	
#	print(num_conn)
	ssids=[]
	for i in lst:
		if( "SSID" in i):
			j=i.split(":")
			j[1]=j[1][1:]
			ssids.append(j[1])
	print("Connections Available ")
	for i in range(len(ssids)):
		print(str(i+1)+" : "+ssids[i])	
	choice=int(input("Choose the network you want to connect >"))

	if(choice>0 and choice<=len(ssids)):
		return connect(ssids[choice-1])
	else:
		print("Choice Invalid")
		scan_wifi()
	#connect("AndroidAPE6C0")
def getWifiList():
	scan_result=subprocess.check_output(["netsh","wlan","show","networks"])
	scan_result=scan_result.decode('utf-8')
	scan_result=scan_result.replace("\r","")

	lst=scan_result.split("\n")
	lst=lst[4:]
	
#	print(num_conn)
	ssids=[]
	for i in lst:
		if( "SSID" in i):
			j=i.split(":")
			j[1]=j[1][1:]
			ssids.append(j[1])
	return ssids

#saveIntoXml("something","pass")
#getHex("Anas")


