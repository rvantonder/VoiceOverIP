import socket
import threading
import sys
from mxitgui import ClientForm
from PyQt4 import QtCore, QtGui

global connections

class VOIPServer:
  def __init__(self, port):
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

  def run(self):
    input = [self.socket,sys.stdin]
    running = 1

    while running:
        inputready,outputready,exceptready = select.select(input,[],[])

        for s in inputready: #the polling loop, between sockets and stdin

            if s == self.socket:
                c = Client(self.socket.accept())
                c.setDaemon(True)
                c.start()
                self.threads.append(c)

            elif s == sys.stdin:
                junk = sys.stdin.readline()
                running = 0


class Client(threading.Thread):
  def __init__(self,(client,address)):
    threading.Thread.__init__(self)
    self.client = client #client, hostname?
    print 'client',self.client
    self.address = address
    print 'address',self.address
    self.size = 1024
    self.running = 1

  def run(self): #when the client thread is started
    data = self.client.recv(self.size) #the first thing it receives
    print 'data',data

class ServerGUI:
  def __init__(self, master):
    frame = Frame(master)
    frame.bind("",self.updateUserList)
    frame.bind("",self.updateText)
    frame.pack()

    
    self.text = Text(frame)
    self.text.pack(side=LEFT)

    self.text.config(state=NORMAL)
    self.text.insert(END, "some text")
    self.text.config(state=DISABLED)  

    self.userlist = Listbox(frame)
    self.userlist.pack(side=LEFT, fill=Y)

    self.userlist.insert(END, "entry")
    
    self.voipServer = VOIPServer(3000)
    self.voipServer.run()


  def updateUserList(self):
    pass

  def updateText(self):
    pass


if __name__ == '__main__':
  try:
    app = QtGui.QApplication(sys.argv)
    gui = ServerGUI() 
    gui.show()
    sys.exit(app.exec_())
  except IndexError:
    print 'Usage: ...'

