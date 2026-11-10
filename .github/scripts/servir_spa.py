import http.server
import os
import sys

raiz = os.path.abspath(sys.argv[1])
porta = int(sys.argv[2]) if len(sys.argv) > 2 else 4200


class Spa(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=raiz, **kwargs)

    def send_head(self):
        caminho = self.translate_path(self.path)
        if not os.path.isfile(caminho):
            self.path = "/index.html"
        return super().send_head()

    def log_message(self, *args):
        pass


http.server.ThreadingHTTPServer(("127.0.0.1", porta), Spa).serve_forever()
