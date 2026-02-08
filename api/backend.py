import socket

def run_server(port=3030):
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(('', port))
    server.listen(5)
    print(f"Server läuft auf Port {port}\n")
    
    while True:
        client, addr = server.accept()
        print(f"\n{'='*60}\nVerbindung von {addr}\n{'='*60}")
        
        data = client.recv(65536)
        print(data.decode('utf-8', errors='ignore'))
        
        # Sende JSON Response
        response = b"HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n{}"
        client.send(response)
        client.close()

if __name__ == '__main__':
    run_server()