#!/usr/bin/env python

"""
An echo client that allows the user to send multiple lines to the server.
Entering a blank line will exit the client.
"""

import socket
import sys
import select

class Client:

    def __init__(self, ip, port):
        self.host = ip
        self.port = port 
        self.size = 1024
        self.socket = None
        self.username = ''

    def open_socket(self):
        try:
          self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
          self.socket.connect((self.host, self.port))
        except socket.error:
          print "error, server refused connection"

    def close_socket(self):
        self.socket.close()
   
    def send(self, message):
        self.socket.send(message)
         
    def run(self):       
        input = [self.socket, sys.stdin]

        while 1:
            inputready,outputready,exceptready = select.select(input,[],[])
            # read from keyboard
            for item in inputready:
                if item == sys.stdin: #if input from terminal
                    line = sys.stdin.readline()
                    if line == '\n':
                        break
                    self.send(line[:-1])
                else: #if socket
                    response = self.socket.recv(self.size)
                    sys.stdout.write(response)
        self.close_socket()

if __name__ == '__main__':

    c = Client(sys.argv[1], int(sys.argv[2]))
    c.open_socket()

    c.run()

