import select
import socket
import threading
import sys
from PyQt4 import QtCore, QtGui
from serverwindow import Ui_Form

global connections

class Client(QtCore.QThread):
  def __init__(self,(client,address)):
    QtCore.QThread.__init__(self,None) #parent = none
    self.client = client #client, hostname?
    self.address = address[0] 
    self.port = str(address[1])
    self.size = 1024
    self.running = 1

  def run(self): #when the client thread is started
    connections.append(self.address)
    self.emit(QtCore.SIGNAL("updateUserlist"), None)

    try:
      print 'sent userlist upon prompt'
      self.client.send("ul__ "+' '.join(connections)+"\n")
    except socket.error:
      print 'failed sending userlist'

    while self.running:
      try:
        data = self.client.recv(self.size)
        self.client.send("echo from server "+data+'\n')
        #print 'data verbatim'
        #print data
      except socket.error as (number,msg):
        print number,msg
        print 'Socket error on receive'
        connections.remove(self.address) #potentially dangerous?
        self.emit(QtCore.SIGNAL("updateUserlist"), None) #send data as test
        self.emit(QtCore.SIGNAL("updateText"), (self.address + " has disconnected"))
        return

      if data:
        print "valid data",data+" from "+self.address
        try:
          cmd, host = self.parse(data)
        except:
          cmd = "None"
          host = "None"
        
        if cmd == r'\call':
          self.emit(QtCore.SIGNAL("updateText"), (self.address + " wants to call " + host))
          if host in connections:
            self.emit(QtCore.SIGNAL("updateText"), (host + " found"))
            self.client.send("Connect to host " + self.address + " with port " + self.port + "\n")
            #connect procedure
          else:
            self.emit(QtCore.SIGNAL("updateText"), (host + " not found"))
            #?
        else:
          pass
   
          
      else:
        connections.remove(self.address)
        self.emit(QtCore.SIGNAL("updateUserlist"), None) #send data as test
        self.emit(QtCore.SIGNAL("updateText"), (self.address + " has disconnected"))
        self.running = 0

  def parse(self, data):
    
    cmd = None
    host = None
    try:
      cmd, host = data.split(" ")
      host.rstrip()
    except:
      print 'could not split' 

    if not cmd == r'\call':
      self.emit(QtCore.SIGNAL("updateText"), ("command " + cmd + " from " + self.address + " not valid"))
    else:
      return cmd, host
       
class ServerGUI(QtGui.QWidget):
  def __init__(self,port):

    super(ServerGUI, self).__init__()

    self.ui = Ui_Form()
    self.ui.setupUi(self)
    
    self.host = ''
    self.port = port
    self.backlog = 5
    self.size = 1024
    self.socket = None
    self.threads = []

    try:
      self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
      self.socket.bind((self.host,self.port))
      self.socket.listen(self.backlog)
    except socket.error:
      print "Could not open port. Aborting."
      sys.exit(1)

    self.c = None

  def run(self):
    input = [self.socket,sys.stdin]
    running = 1

    while running:
        inputready,outputready,exceptready = select.select(input,[],[])

        for s in inputready: #the polling loop, between sockets and stdin

            if s == self.socket:
                c = Client(self.socket.accept())
                self.connect(c, QtCore.SIGNAL("updateUserlist"), self.updateUserlist)
                self.connect(c, QtCore.SIGNAL("updateText"), self.updateText)
                c.start()
                self.threads.append(c)

            elif s == sys.stdin:
                junk = sys.stdin.readline()
                running = 0

  def updateUserlist(self):
    self.ui.listWidget.clear()

    for i in connections:
      item = QtGui.QListWidgetItem(str(i))
      self.ui.listWidget.addItem(item) 
    
  def updateText(self, msg):
    self.ui.textEdit.append(msg)
    self.ui.textEdit.ensureCursorVisible()


if __name__ == '__main__':

#  connections = [] 
  connections = ["1.1.1.1", "2.2.2.2"]

  try:
    app = QtGui.QApplication(sys.argv)
    gui = ServerGUI(int(sys.argv[1]))
    t = threading.Thread(target=gui.run)
    t.setDaemon(True)
    t.start()
    gui.show()
    sys.exit(app.exec_())
  except IndexError:
    print 'Usage: ...'

