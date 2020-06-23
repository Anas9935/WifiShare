#frontend
import os
#os.environ["KIVY_NO_CONSOLELOG"] = "1"
from kivy.app import App
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.gridlayout import GridLayout
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.scrollview import ScrollView
from kivy.uix.recycleview import RecycleView
from kivy.uix.recycleboxlayout import RecycleBoxLayout
from kivy.uix.screenmanager import ScreenManager,Screen
from kivy.config import Config
import socketUtils as su
from wifiUtils2 import *
from constants import *
import multiprocess as mp
from tkinter.filedialog import askopenfilenames



from kivy.properties import ObjectProperty # for handling click events

from functools import partial 		#for button clikcs

Config.set('graphics','resizable',False)
Config.set('graphics','width','400')
Config.set('graphics','height','600')

class ScreenMain(Screen):
	def __init__(self,**kwargs):
		super(ScreenMain,self).__init__(**kwargs)
		self.initialiseViews()
	def initialiseViews(self):
		self.serverBtn=self.ids.main_server_btn_id
		self.clientBtn=self.ids.main_client_btn_id
class ScreenServer(Screen):
	def __init__(self,**kwargs):
		super(ScreenServer,self).__init__(**kwargs)
		self.initialiseViews()
		self.mainSocket=""
		self.conn=0
		self.sending=0
		self.receiving=0
	def initialiseViews(self):
		self.status_tv=self.ids.server_status_id
		self.sending_tv=self.ids.server_sending_id
		self.receiving_tv=self.ids.server_receiving_id
		self.box=self.ids.server_wifi_conn_id
	def checkUpnDown(self):
		if(self.sending==1 and self.receiving==1):
			return True
		else:
			return False
	def updateUi(self):
		if(self.conn==1):
			self.status_tv.text="Connected"
		if(self.sending==1):
			self.sending_tv.text="ALLOWED"
		if(self.receiving==1):
			self.receiving_tv.text="ALLOWED"
	def checkConnection(self):
		if(self.mainSocket !=None):
			su.send(self.mainSocket,CONNECTION_ESTABLISHED_SERVER,'utf-8')
			ack=su.receive(self.mainSocket,BUFFER_SIZE_VERY_SMALL,'utf-8')
			if(ack==RECEIVING_ACK):
				#the sent packet is acknowledged
				print("UPLOADING SUCCESS")
				self.sending=1
				self.updateUi()
			else:
				print("UPLOADING FAILED")
				self.sending=0
				self.conn=0
				self.mainSocket.close()
				return
			msg=su.receive(self.mainSocket,BUFFER_SIZE_VERY_SMALL,'utf-8')
			if(msg==CONNECTION_ESTABLISHED_CLIENT):
				#the message is received
				su.send(self.mainSocket,RECEIVING_ACK,'utf-8')
				print("DOWNLOADING SUCCESS")
				self.receiving=1
				self.updateUi()
			else:
				print("DOWNLOADING FAILED")
				self.receiving=0
				self.conn=0
				self.updateUi()
				self.mainSocket.close()
				return

	def connectButtons(instance,value):
		ssid=value.text
		print("Selected Button: ",ssid)
		res=connect(ssid)
		if(res=="NOPASS"):
			#need to auth
			main=instance.parent
			authScreen=main.get_screen('screen_auth')
			authScreen.ids.auth_name_id.text=ssid
			
			main.switch_to(main.get_screen('screen_auth'))
		else:
			res=checkConnection(ssid,0)
			if(res=="CONNECTED"):
				#go further
				print("Connected")
				
			else:
				
				print("Can't Connect To this network")

	def onWifiPressed(instance):
		ssids=getWifiList()
		print(ssids)
		print(len(instance.box.children))
		temp=instance.box.children
		temp2=[]
		for i in temp:
			temp2.append(i)
		for i in temp2:
			instance.box.remove_widget(i)
#		rview=self.ids.wifiRview
#		rview.data=[{'text':str(x) for x in range(10)}]
		for i in ssids:
			btn=Button(text=i,size_hint=(1,None),height=30)
			btn.bind(on_press=instance.connectButtons)
			instance.box.add_widget(btn)
			
	def onCreatePressed(instance):
		instance.mainSocket=su.createSocket()
		if(instance.mainSocket!=None):
			print("Connection Established")
			instance.conn=1
			instance.updateUi()
			instance.checkConnection()
	def onReceivePressed(instance):
		if(instance.checkUpnDown):
			instance.down_item_list=su.gotoRecvFile(instance.mainSocket)
			instance.manager.current='screen_receive'
			#got the list now start downloading
	def onChoosePressed(instance):
		instance.filepaths=askopenfilenames()
		num_files=len(instance.filepaths)
	def onSendPressed(instance):
		fps=instance.filepaths
		if(instance.checkUpnDown and fps!=None):
			print("You are here with files",fps)
			instance.manager.current='screen_send'
	def onClosePressed(instance):
		if(instance.mainSocket!=None):
			instance.conn=0
			instance.sending=0
			instance.receiving=0
			instance.updateUi()
			instance.mainSocket.close()
			print("Socket Closed")
	
class ScreenClient(Screen):
	def __init__(self,**kwargs):
		super(ScreenClient,self).__init__(**kwargs)
class ScreenAuth(Screen):
	def __init__(self,**kwargs):
		super(ScreenAuth,self).__init__(**kwargs)
	def onConnectPressed(instance):
		passwd=instance.ids.auth_password_id
		text=passwd.text
		nameLabel=instance.ids.auth_name_id
		ssid=nameLabel.text
		res=saveIntoXml(ssid,text)
		if(res=="CONNECTED"):
			print("Connected")
		elif(res=="CONNECTION_ERROR"):
			print("CONNECTION_ERROR")
		else:
			print("exception: ",res)
		

class ScreenReceive(Screen):

	def __init__(self,**kwargs):
		super(ScreenReceive,self).__init__(**kwargs)
		
		
	def onProcessEnd(self,name):
		if(name==None):
			print("File Not Received")
			return
		print(name+" Received")

	def on_parent(self,screen,parent):
		if(len(self.ids)!=0):
#			print("here",parent)
			box=self.ids.receive_box_id
			#sock=self.manager.get_screen('screen_server').mainSocket
			man=self.manager
			item_list=man.get_screen('screen_server').down_item_list
			
			sock=man.get_screen('screen_server').mainSocket
			su.send(sock,RECEIVING_ACK,'utf-8')
			a=0
			pool=mp.Pool()
			for i in item_list:
				
				j=i.replace("\n","")
				j=j.split(SEPARATOR)
				nm=j[0]
				size=int(j[1])
				port=int(j[2])
				newLayout=self.ReceiveTodo(nm,100)
				box.add_widget(newLayout)
				pool.apply_async(su.receiveFile,args=(nm,size,port,a,),callback=self.onProcessEnd)
				a+=1
			pool.close()
			pool.join()

	def updateLayout(self,layout,updVal):
		per=layout.ids.perc_id
		per.text=str(updVal)+"%"
	def ReceiveTodo(self,name,progress):
		layout=BoxLayout(spacing=10,padding=16)
		nm=Label(text=name,id="name_id")
		pr=Label(text=str(progress)+"%",pos_hint={'right':1},id="perc_id")
		layout.add_widget(nm)
		layout.add_widget(pr)
		return layout

class ScreenSend(Screen):
	def __init__(self,**kwargs):
		super(ScreenSend,self).__init__(**kwargs)
	def onSendComplete(self,i):
		print(str(i) +" sending completed ")
	def on_parent(self,screen,parent):
		man=self.manager
		server=man.get_screen('screen_server')
		fps=server.filepaths
		box=self.ids.send_box_id
		num_files=len(fps)
		sock=server.mainSocket
		ports=su.gotoSendFile(sock,fps)
		if(ports!=0):
			pool=mp.Pool()
			i=0
	#			print("its done ipto sending data")
			for filepath in fps:
				filesize=int((os.stat(filepath)).st_size)
				filename=os.path.basename(filepath)
				newLayout=self.SendTodo(filename,0)
				box.add_widget(newLayout)
				pool.apply_async(su.sendFile,args=(filepath,i,ports[i],filesize,),callback=self.onSendComplete)
				i+=1
			pool.close()
			pool.join()
			su.currentPortAddress-=2*num_files
	def SendTodo(self,name,progress):
		layout=BoxLayout(spacing=10,padding=16)
		nm=Label(text=name,id="name_id")
		pr=Label(text=str(progress)+"%",pos_hint={'right':1},id="perc_id")
		layout.add_widget(nm)
		layout.add_widget(pr)
		return layout
class MyApp(App):
	def build(self):
		self.load_kv('screens/screenMain.kv')
		self.load_kv('screens/screenServer.kv')
		self.load_kv('screens/screenClient.kv')
		self.load_kv('screens/screenAuth.kv')		
		self.load_kv('screens/screenReceive.kv')
		self.load_kv('screens/screenSend.kv')
		sm=ScreenManager()
		sm.add_widget(ScreenMain(name="screen_main"))
		sm.add_widget(ScreenServer(name="screen_server"))
		sm.add_widget(ScreenClient(name="screen_client"))
		sm.add_widget(ScreenAuth(name="screen_auth"))
		sm.add_widget(ScreenReceive(name="screen_receive"))
		sm.add_widget(ScreenSend(name="screen_send"))
		return sm
	
def main():
	MyApp().run()
if (__name__=="__main__"):
	main()