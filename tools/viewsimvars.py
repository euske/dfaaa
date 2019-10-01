#!/usr/bin/env python
import sys
import re
import os.path
import json
import time
import random
from srcdb import SourceDB, SourceAnnot
from srcdb import q
from getwords import stripid

CHOICES = [
    ('a', 'MUST HAVE the same name'),
    ('b', 'CAN HAVE the same name'),
    ('c', 'MUST NOT HAVE the same name'),
    ('z', 'UNDECIDABLE'),
]

def dummy(x): return ''

def getrecs(fp):
    rec = {}
    for line in fp:
        line = line.strip()
        if line.startswith('+'):
            (k,_,v) = line[1:].partition(' ')
            rec[k] = json.loads(v)
        elif not line:
            yield rec
            rec = {}
    return


def showhtmlheaders(out, title, script=None):
    out.write('''<!DOCTYPE html>
<html>
<meta charset="utf-8" />
<title>%s</title>
<style>
h1 { border-bottom: 4px solid black; }
h2 { background: #ffccff; padding: 2px; }
h3 { background: #000088; color: white; padding: 2px; }
pre { margin: 0 1em 1em 1em; border: 1px solid gray; }
ul > li { margin-bottom: 0.5em; }
.cat { border: 1px solid black; padding: 2px; background: #eeeeee; margin: 1em; }
.src0 { background:#eeffff; }
.src0 mark { background:#ff88ff; }
.src1 { background:#ffffee; }
.src1 mark { background:#44cc44; }
</style>
''' % q(title))
    if script is None:
        out.write('<body>\n')
        return
    out.write(f'<script>\n{script}\n</script>\n')
    out.write('''<body onload="run('results', '{title}_eval')">
<h1>Similar Variable Tagging Experiment: {title}</h1>
<h2>Your Mission</h2>
<ul>
<li> For each code snippet, look at the variable marked as
    <code class=var0>aa</code> / <code class=var1>bb</code>
    and choose their relationship from the menu.
  <ol type=a>
  <li> They MUST BE the same name.
  <li> They CAN BE the same name.
  <li> They SHOULD BE similar but different.
  <li> They MUST BE completely different.
  </ol>
<li> When it's undecidable after <u>3 minutes</u> with the given snippet,
 choose UNDECIDABLE.
<li> <u>Do not consult others about the code during this experiment.</u>
<li> Your choices are saved in the follwoing textbox:<br>
  <textarea id="results" cols="80" rows="4" spellcheck="false" autocomplete="off"></textarea><br>
  When finished, send the above content (from <code>#START</code> to <code>#END</code>) to
  the experiment organizer.<br>
</ul>
'''.format(title=q(title)))
    return

def main(argv):
    import fileinput
    import getopt
    def usage():
        print(f'usage: {argv[0]} [-o output] [-T title] [-S script] [-t thresholld] [-n limit] [-R] [-c encoding] srcdb [pairs]')
        return 100
    try:
        (opts, args) = getopt.getopt(argv[1:], 'o:T:S:t:n:Rc:')
    except getopt.GetoptError:
        return usage()
    output = None
    title = None
    script = None
    threshold = 0.9
    limit = 10
    randomized = False
    encoding = None
    timestamp = time.strftime('%Y%m%d')
    for (k, v) in opts:
        if k == '-o':
            output = v
        elif k == '-T':
            title = v
        elif k == '-S':
            with open(v) as fp:
                script = fp.read()
        elif k == '-t': threshold = float(v)
        elif k == '-n': limit = int(v)
        elif k == '-R': randomized = True
        elif k == '-c': encoding = v
    if not args: return usage()
    path = args.pop(0)
    srcdb = SourceDB(path, encoding)

    out = sys.stdout
    if output is not None:
        if os.path.exists(output):
            print(f'Already exists: {output!r}')
            return 1
        out = open(output, 'w')

    VARS = ['<mark>aa</mark>', '<mark>bb</mark>']
    OPTIONS = ''.join(
        f'<option value="{v}">{v}. {q(c)}</option>' for (v,c) in CHOICES)

    def showsrc(i, name, srcs):
        pat = re.compile(r'\b'+re.escape(name)+r'\b')
        annot = SourceAnnot(srcdb)
        for (path,s,e) in srcs:
            annot.add(path,s,e)
        for (src,ranges) in annot:
            out.write(f'<div>{q(src.name)} <pre class=src{i}>\n')
            def abody(annos, s):
                s = q(s.replace('\n',''))
                if annos:
                    s = pat.sub(VARS[i], s)
                return s
            for (lineno,line) in src.show(
                    ranges, astart=dummy, aend=dummy, abody=abody):
                if lineno is None:
                    out.write('     '+line+'\n')
                else:
                    out.write(f'{lineno:5d}:{line}\n')
            out.write('</pre></div>\n')
        return

    def showrec(rid, rec):
        key = (f'R{rid:003d}')
        out.write(f'<h3 class=pair>Pair {rid}</h3>\n')
        out.write(
            f'<div class=cat><span id="{key}" class=ui>Choice: <select>{OPTIONS}</select> &nbsp; Comment: <input size="30" /></span></div>\n')
        for (i,(item,srcs)) in enumerate(zip(rec['ITEMS'], rec['SRCS'])):
            name = stripid(item)
            showsrc(i, name, srcs)
        if randomized:
            print(key, rec['SIM'])
        return

    if title is None:
        title = f'{args[0]}_{timestamp}'
    showhtmlheaders(out, title, script)

    rid = 0
    for path in args:
        with open(path) as fp:
            recs = [ rec for rec in getrecs(fp) if threshold <= rec['SIM'] ]
        recs.sort(key=lambda rec:rec['SIM'], reverse=True)
        if randomized:
            n = len(recs)
            recs = [ recs[i*n//limit] for i in range(limit) ]
            random.shuffle(recs)
        else:
            recs = recs[:limit]
        for rec in recs:
            showrec(rid, rec)
            rid += 1

    return 0

if __name__ == '__main__': sys.exit(main(sys.argv))