import socket
import time
import os
import threading
from _thread import *
import multiprocess as mp
import time
from constants import *
from wifiUtils import *
from send import *
from receive import *
from tkinter import Tk
from tkinter.filedialog import askopenfilenames
from interface import scan_wifi

Tk().withdraw()

currentPortAddress=getPortAddress()
globalUpl=0
globalDown=0
p_count=mp.cpu_count()

def checkUpNDown():
	if(globalUpl==1 and globalDown==1):
		return True
	else:
		print("UPS and/or Downs are not available")
		return False
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
def check_conn(s):
	send(s,CONNECTION_ESTABLISHED_SERVER,'utf-8')
	ack=receive(s,BUFFER_SIZE_VERY_SMALL,'utf-8')
	if(ack==RECEIVING_ACK):
		#the sent packet is acknowledged
		print("UPLOADING SUCCESS")
		globalUpl=1
	else:
		print("UPLOADING FAILED")
		globalUpl=0
		s.close()
	msg=receive(s,BUFFER_SIZE_VERY_SMALL,'utf-8')
	if(msg==CONNECTION_ESTABLISHED_CLIENT):
		#the message is received
		send(s,RECEIVING_ACK,'utf-8')
		print("DOWNLOADING SUCCESS")
		globalDown=1
	else:
		print("DOWNLOADING FAILED")
		globalDown=0
		s.close()


def recvGetline(sMain):
	msg=""
	while True:
		ch=receive(sMain,1,'utf-8')
		msg+=ch
		if(ch =='\n'):
			break
	return msg
def gotoRecvFile(pool,sMain):
	msg=recvGetline(sMain)
	
	msg=msg.split(SEPARATOR)
	num_files=int(msg[1])
	send(sMain,RECEIVING_ACK,'utf-8')
	fileinfo=[]
	for i in range(num_files):
		fileinfo.append(recvGetline(sMain))
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
		import os
		import socket
		from constants import BASEPATH,BUFFER_SIZE_MEDIUM,REQUEST_TO_SEND,SEPARATOR,ALLOW_TO_RECV
		from wifiUtils import getIpAddress
		from main import send,receive,recvGetline
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
			print("iter",iter)
			while True:
				bytes_read=receive(conn,BUFFER_SIZE_MEDIUM)#conn.recv(BUFFER_SIZE_MEDIUM)
				iter-=len(bytes_read)
				f.write(bytes_read)
				if (iter<=0):
					break
			print("File is saved ",i)
		subSock.close()
	except Exception as ex:
		print(ex)


def gotoSendFile(pool,sMain):
	global currentPortAddress
	filepaths=askopenfilenames()
	num_files=len(filepaths)
	send(sMain,"Length"+SEPARATOR+str(num_files)+"\n",'utf-8')
	print("send",num_files)
	#msg=receive(sMain,BUFFER_SIZE_VERY_SMALL,'utf-8')
	req=""
	while True:
		msg=receive(sMain,1,'utf-8')
		print(msg,end="")
		if(msg!='\n'):
			req+=msg
		else:
			req+=msg
			break
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
			i=0
#			print("its done ipto sending data")
			for filepath in filepaths:
				filesize=int((os.stat(filepath)).st_size)
				pool.apply_async(sendFile,args=(filepath,i,ports[i],filesize,))
				i+=1
				
		pool.close()
		pool.join()
		currentPortAddress-=2*num_files
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
		else:
			print("receiving not allowed")
			conn.close()
	except Exception as ex:
		print(ex)
			

def main():
	#scan_res=scan_wifi()
	scan_res="LOL"
	if(scan_res==CONNECTION_ERROR):
		print("Error in connecting this Network.")
		return
	sMain=socket.socket(socket.AF_INET,socket.SOCK_STREAM)
#	sMain.setsockopt(socket.SOL_SOCKET,socket.SO_SNDBUF,BUFFER_SIZE_SMALL)
	PORT=getPortAddress()
	currentPortAddress=PORT
	try:
		sMain.bind((getIpAddress(),PORT))
	except(OSError):
		print("OSEXCCEPTION")
	print("Server is Up and Ready to Connect")
	sMain.listen()
	conn,addr=sMain.accept()
	print("Client Connected",addr)
	print("Checking Connection")
	check_conn(conn)
	if(checkUpNDown):
		while(True):
			print("Choose your Action\n-------------------- ")
			print("1.Send")
			print("2.Recieve")
			print("3.Exit")
			choice=int(input(">"))
			if(choice==3):
				break
			elif (choice==1):
				pool=mp.Pool(p_count)
				stat=gotoSendFile(pool,conn)
				
				if(stat):
					print("\nSending a File is Successful")
				else:
					print("\nSending a file is Failed")
			elif (choice==2):
				pool=mp.Pool(p_count)
				stat=gotoRecvFile(pool,conn)
				if(stat):
					print("File is Recieved Successfully")
				else:
					print("File is Failed to Recieved")
			else:
				print("Bad Choice. Retry.")
	sMain.close()


'''
def main(instance):

	sMain=socket.socket(socket.AF_INET,socket.SOCK_STREAM)
#	sMain.setsockopt(socket.SOL_SOCKET,socket.SO_SNDBUF,BUFFER_SIZE_SMALL)
	PORT=getPortAddress()
	currentPortAddress=PORT
	try:
		sMain.bind(("127.0.0.1",PORT))
	except(OSError):
		print("OSEXCCEPTION")
	instance.text="Server Up"
	print("Server is Up and Ready to Connect")
	sMain.listen()
	conn,addr=sMain.accept()
	print("Client Connected",addr)
	print("Checking Connection")
	check_conn(conn)
	if(checkUpNDown):
		while(True):
			print("Choose your Action\n-------------------- ")
			print("1.Send")
			print("2.Recieve")
			print("3.Exit")
			choice=int(input(">"))
			if(choice==3):
				break
			elif (choice==1):
				pool=mp.Pool(p_count)
				stat=gotoSendFile(pool,conn)
				
				if(stat):
					print("\nSending a File is Successful")
				else:
					print("\nSending a file is Failed")
			elif (choice==2):
				pool=mp.Pool(p_count)
				stat=gotoRecvFile(pool,conn)
				if(stat):
					print("File is Recieved Successfully")
				else:
					print("File is Failed to Recieved")
			else:
				print("Bad Choice. Retry.")
	sMain.close()
'''


if __name__=="__main__":
	main()