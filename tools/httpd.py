#!/usr/bin/env python
import sys
from graph import BASEDIR, run_fgyama
from graph2gv import run_dot
from tempfile import NamedTemporaryFile
from cgi import FieldStorage
from http import HTTPStatus
from http.server import HTTPServer
from http.server import BaseHTTPRequestHandler

def q(s):
    return (s.replace('&','&amp;')
            .replace('>','&gt;')
            .replace('<','&lt;')
            .replace('"','&quot;'))

class MyHTTPRequestHandler(BaseHTTPRequestHandler):

    def do_GET(self):
        (path,_,_) = self.path.partition('?')
        if path == '/':
            self.send_index()
        else:
            self.send_error(HTTPStatus.NOT_FOUND)
        return

    def do_POST(self):
        (path,_,qs) = self.path.partition('?')
        if path == '/graph':
            self.send_graph(qs)
        else:
            self.send_error(HTTPStatus.NOT_FOUND)
        return

    def do_HEAD(self):
        self.send_error(HTTPStatus.NOT_IMPLEMENTED)
        return

    def send(self, text):
        self.wfile.write(text.encode('utf-8'))
        return

    def send_index(self):
        self.send_response(HTTPStatus.OK)
        self.send_header('Content-Type', 'text/html')
        self.end_headers()
        self.send_text('')
        return

    def send_text(self, text):
        self.send('''<!DOCTYPE html>
<html><head><title>FGyama Grapher</title></head><body>
<h1>FGyama Grapher</h1>
<form method=POST enctype="multipart/form-data" action="/graph">
<textarea name=t cols=80 rows=10>%s</textarea><br>
<input type=file name=f><br>
<input type=submit>
<input type=reset>
</form>
''' % q(text))
        return

    def send_graph(self, qs):
        environ = {
            'REQUEST_METHOD': self.command,
            'QUERY_STRING': qs,
        }
        fs = FieldStorage(self.rfile, self.headers, environ=environ)
        f = fs['f']
        if f is not None and f.file and f.filename:
            text = f.file.read().decode('utf-8')
        else:
            text = fs.getvalue('t', '')
        self.send_response(HTTPStatus.OK)
        self.send_header('Content-Type', 'text/html')
        self.end_headers()
        self.send_text(text)
        if text:
            with NamedTemporaryFile(suffix='.java') as fp:
                fp.write(text.encode('utf-8'))
                fp.flush()
                graphs = run_fgyama(fp.name)
                for graph in graphs:
                    output = run_dot(graph)
                    self.send('<div class=graph>%s</div>' % output)
        self.send('''
<p><a href="/">bacj</a>
</body></html>
''')
        return

def main(argv):
    server_address = ('', 8000)
    handler = MyHTTPRequestHandler
    print('BASEDIR: %r' % BASEDIR)
    with HTTPServer(server_address, handler) as httpd:
        sa = httpd.socket.getsockname()
        serve_message = 'Serving at (http://{host}:{port}/) ...'
        print(serve_message.format(host=sa[0], port=sa[1]))
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print('\nKeyboard interrupt received, exiting.')
            sys.exit(0)
    return 0

if __name__ == '__main__': sys.exit(main(sys.argv))