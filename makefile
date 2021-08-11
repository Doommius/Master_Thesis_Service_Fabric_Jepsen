TEX = pdflatex -shell-escape -interaction=nonstopmode -file-line-error
PRE =  $(TEX) -ini -job-name="preamble" "&pdflatex preamble.tex\dump"
BIB = bibtex

.PHONY: all view

all : main.pdf

view :
    open main.pdf

main.pdf : main.tex preamble.fmt main.bbl main.blg
    $(TEX) main.tex

main.bbl main.blg : main.bib main.aux
    $(BIB) main

main.aux : main.tex
    $(TEX) main.tex

main.bib : main.tex
    $(TEX) main.tex

preamble.fmt : preamble.tex
    $(PRE) preamble.tex
