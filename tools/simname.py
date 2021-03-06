#!/usr/bin/env python
import sys
from srcdb import SourceDB
from graphs import get_graphs

def splitcamel(s):
    a = []
    w = ''
    m = 0
    for c in s:
        if c.isupper():
            if m != 2:
                if w:
                    a.append(w)
                    w = ''
                m = 2
            w += c
        elif c.islower():
            m = 1
            w += c
        else:
            if m != 0:
                if w:
                    a.append(w)
                    w = ''
            m = 0
    if w:
        a.append(w)
    return a

assert splitcamel('!!') == []
assert splitcamel('abcAbcABcABC') == ['abc','Abc','ABc','ABC']
assert splitcamel('_abc_ABC123a_') == ['abc','ABC','a']

# main
def main(argv):
    import fileinput
    import getopt
    def usage():
        print(f'usage: {argv[0]} [-o output] [-B basedir] [-c encoding] '
              'out.graph ...')
        return 100
    try:
        (opts, args) = getopt.getopt(argv[1:], 'o:B:c:')
    except getopt.GetoptError:
        return usage()
    output = None
    srcdb = None
    encoding = None
    for (k, v) in opts:
        if k == '-o': output = v
        elif k == '-B': srcdb = SourceDB(v, encoding)
        elif k == '-c': encoding = v
    if not args: return usage()

    if output is None:
        fp = sys.stdout
    else:
        fp = open(output, 'w')

    names = {}
    for path in args:
        print(f'Loading: {path!r}...', file=sys.stderr)
        for method in get_graphs(path):
            if method.style == 'initializer': continue
            if ';.' in method.name:
                (_,_,name) = method.name.partition(';.')
                (name,_,_) = name.partition('(')
            else:
                name = method.name
            words = splitcamel(name)
            #print(name, words)
            for n in range(1, len(words)):
                k = tuple(words[-n:])
                if k in names:
                    a = names[k]
                else:
                    a = names[k] = []
                a.append(method)

    done = set()
    for k in sorted(names.keys(), key=lambda k:len(k), reverse=True):
        a = [ method for method in names[k] if method not in done ]
        if 2 <= len(a):
            fp.write(f'= {len(a)}\n')
            for method in a:
                fp.write(f'+ {method.name}\n')
                if srcdb is None: continue
                if method.src is None or method.ast is None: continue
                src = srcdb.get(method.src)
                (_,start,end) = method.ast
                fp.write(f'# {method.src}\n')
                ranges = [(start, end, 0)]
                for (lineno,line) in src.show(ranges):
                    if lineno is None:
                        fp.write(line.rstrip()+'\n')
                    else:
                        fp.write(f'{lineno:4d}: {line.rstrip()}\n')
            fp.write('\n')
        done.update(a)

    if fp is not sys.stdout:
        fp.close()
    return 0

if __name__ == '__main__': sys.exit(main(sys.argv))
