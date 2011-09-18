from Tkinter import *
import socket
import threading

class VOIPServer:
  pass

class ServerGUI:
  def __init__(self, master):
    frame = Frame(master)
    frame.pack()

    self.text = Text(frame)
    self.text.pack(side=LEFT)

    self.text.config(state=NORMAL)
    self.text.insert(END, "some text")
    self.text.config(state=DISABLED)  


#    self.text.grid(row=1,column=1,sticky=W)

    self.userlist = Listbox(frame)
    self.userlist.pack(side=LEFT, fill=Y)

    self.userlist.insert(END, "entry")

    #userlist.delete(0, END)

#    self.userlist.grid(row=1,column=2,sticky=W)



if __name__ == '__main__':
  root = Tk()
  app = ServerGUI(root)
  root.mainloop()
    


