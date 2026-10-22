import glob
import os
import sys


def escapar(texto):
    return texto.replace("%", "%25").replace("\r", "").replace("\n", "%0A")


origem = sys.argv[1]
texto = sys.stdin.read() if origem == "-" else open(origem, errors="replace").read()
todas = texto.splitlines()
if origem == "-":
    linhas = todas[-120:]
else:
    erros = [l for l in todas if "[ERROR]" in l][:120]
    causas = [l for l in todas if "Caused by" in l][:40]
    linhas = erros + causas or todas[-120:]
for pasta in sys.argv[2:]:
    for arquivo in sorted(glob.glob(os.path.join(pasta, "*.txt"))):
        conteudo = open(arquivo, errors="replace").read()
        if "FAILURE" in conteudo or "<<< ERROR" in conteudo:
            linhas += conteudo.splitlines()[:80]
blocos, atual = [], ""
for linha in linhas:
    linha = linha[:500]
    if len(atual) + len(linha) > 3800:
        blocos.append(atual)
        atual = ""
    atual += linha + "\n"
if atual:
    blocos.append(atual)
for i, bloco in enumerate(blocos[:9]):
    print(f"::error title=log {i + 1}::{escapar(bloco)}")
