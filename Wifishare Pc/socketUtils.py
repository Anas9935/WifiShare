import socket
import time
import os
import threading
from _thread import *
import multiprocess as mp

from constants import *
from wifiUtils import *
from send import *
from receive import *

#from interface import scan_wifi

#Tk().withdraw()

currentPortAddress=getPortAddress()
globalUpl=0
globalDown=0

p_count=mp.cpu_count()


def send(s,msg,encd=''):
	if(encd!=''):
		
		s.send(msg.encode(encd))
	else:
		s.send(msg)
def receive(s,buffer,decd=''):
	if(decd!=''):
		msg=(s.recv(buffer)).decode(decd)
		return msg
	else:
		msg=s.recv(buffer)
		return msg
		
def recvGetline(sMain):
	msg=""
	while True:
		ch=receive(sMain,1,'utf-8')
		msg+=ch
		if(ch =='\n'):
			break
	return msg
def gotoRecvFile(sMain):
	msg=recvGetline(sMain)
	
	msg=msg.split(SEPARATOR)
	num_files=int(msg[1])
	send(sMain,RECEIVING_ACK,'utf-8')
	fileinfo=[]
	for i in range(num_files):
		fileinfo.append(recvGetline(sMain))
	return fileinfo
	
	send(sMain,RECEIVING_ACK,'utf-8')
	for i in range(num_files):
		f=fileinfo[i].split(SEPARATOR)
		filename=f[0]
		filesize=int(f[1])
		port=int(f[2])
		#create thread for each file
		pool.apply_async(receiveFile,args=(filename,filesize,port,i))
	
	pool.close()
	pool.join()
	return True
	
def receiveFile(filename,filesize,port,i):
	try:
		print(filename,port)
		import os
		import socket
		from constants import BASEPATH,BUFFER_SIZE_MEDIUM,REQUEST_TO_SEND,SEPARATOR,ALLOW_TO_RECV
		from wifiUtils import getIpAddress
		from socketUtils import send,receive,recvGetline
		from kivy.uix.label import Label
		from kivy.uix.boxlayout import BoxLayout
		f=open(BASEPATH+filename,"wb")
		subSock=socket.socket(socket.AF_INET,socket.SOCK_STREAM)
		subSock.bind((getIpAddress(),port))
		subSock.listen()
		print("Socket for ",i," created")
		conn,addr=subSock.accept()
		print("connection created ",addr)
		#msg="REQUEST_TO_SEND"+SEPARATOR+str(i)+"\n"
		#send(conn,msg,'utf-8')
		msg=recvGetline(conn)
		#print(msg)
		msg=msg.split(SEPARATOR)
		msg[0]+="\n"
		if(msg[0]==REQUEST_TO_SEND and int(msg[1])==i):
			print("Request arrived")
			msg="ALLOW_TO_RECV"+SEPARATOR+str(i)+"\n"
			send(conn,msg,'utf-8')
		
			iter=filesize
			uptonow=0
			per=0
		#	perTv=layout.ids.prec_id
		#	perTv.text=str(per)+"%"
			while True:
				bytes_read=receive(conn,BUFFER_SIZE_MEDIUM)#conn.recv(BUFFER_SIZE_MEDIUM)
				iter-=len(bytes_read)
				f.write(bytes_read)
				uptonow+=len(bytes_read)
				newper=int((uptonow*100)/filesize)
				if(newper!=per):
					per=newper
		#			perTv.text=str(per)+"%"
				if (iter<=0):
					break
			print("File is saved ",i)
			f.close()
			return filename
		else:
			return None
		subSock.close()
			
	except Exception as ex:
		print(ex)
		return None


def gotoSendFile(sMain,filepaths):
	global currentPortAddress
	#filepaths=askopenfilenames()
	num_files=len(filepaths)
	send(sMain,"Length"+SEPARATOR+str(num_files)+"\n",'utf-8')
	print("sending Request")
	#msg=receive(sMain,BUFFER_SIZE_VERY_SMALL,'utf-8')
	req=recvGetline(sMain)
	
	ports=[]
	if(req==RECEIVING_ACK):
		for filepath in filepaths:
			filename=os.path.basename(filepath)
			filesize=int((os.stat(filepath)).st_size)
			currentPortAddress+=2
			fileInfo=filename+SEPARATOR+str(filesize)+SEPARATOR+str(currentPortAddress)+"\n"
			ports.append(currentPortAddress)
			send(sMain,fileInfo,'utf-8')
		msg=receive(sMain,BUFFER_SIZE_SMALL,'utf-8')
		if(msg==RECEIVING_ACK):
			#file data is sent, now send the files on separate threads
			return ports
			
		return True
	print("File cant  be reached: FILEOPENERROR")
	return False
def sendFile(filepath,i,port,filesize):
	try:
		import socket
		import os
		from constants import BUFFER_SIZE_MEDIUM,SEPARATOR
		from wifiUtils import getIpAddress
		from main import send
		from main import receive
		print("Port :",port)
		subsock=socket.socket(socket.AF_INET,socket.SOCK_STREAM)
		subsock.bind((getIpAddress(),port))
		
		subsock.listen()
		print("Waiting",i)
		conn,addr=subsock.accept()
		print("Connection established",i,addr)
		msg="REQUEST_TO_SEND"+SEPARATOR+str(i)+"\n"
		send(conn,msg,'utf-8')
		recv=""
		while True:
			ch=receive(conn,1,'utf-8')
			if(ch!='\n'):
				recv+=ch
			else:
				recv+=ch
				break
		exp="ALLOW_TO_RECV"+SEPARATOR+str(i)+"\n"
		if(recv==exp):
			print("request accepted",i)
			#sending file by bytes
			f=open(filepath,"rb")
			byte=f.read(BUFFER_SIZE_MEDIUM)
			count=0
			a=0
			print("Sending in progress",end=" ")
			while byte:
				send(conn,byte)
				byte=f.read(BUFFER_SIZE_MEDIUM)
				count+=len(byte)
				#print(".",end="")
			rem=filesize%BUFFER_SIZE_MEDIUM
			byte=f.read(rem)
			send(conn,byte)
			conn.close()
			return i
		else:
			print("receiving not allowed")
			conn.close()
			return -1
	except Exception as ex:
		print(ex)
		return -1
			

def createSocket():
	
	sMain=socket.socket(socket.AF_INET,socket.SOCK_STREAM)
#	sMain.setsockopt(socket.SOL_SOCKET,socket.SO_SNDBUF,BUFFER_SIZE_SMALL)
	PORT=getPortAddress()
	currentPortAddress=PORT
	try:
		sMain.bind((getIpAddress(),PORT))
	except Exception as e:
		print(PORT,getIpAddress())
		print("OSEXCCEPTION")
		print(e)
		return None
	print("Server is Up and Ready to Connect")
	sMain.listen()
	conn,addr=sMain.accept()
	print("Client Connected",addr)
	#print("Checking Connection")
	return conn
	
