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
    print 'client',self.client
    self.address = address
    print 'address',self.address
    self.size = 1024
    self.running = 1

  def run(self): #when the client thread is started
    data = self.client.recv(self.size) #the first thing it receives
    print 'received',data
    connections.append(data)
    print 'conns',connections
    self.emit(QtCore.SIGNAL("updateUserlist"), data) #send data as test

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

    print 'updated',connections

    for i in connections:
      item = QtGui.QListWidgetItem(str(i))
      self.ui.listWidget.addItem(item) 
    
  def updateText(self):
    pass


if __name__ == '__main__':

  connections = []

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

