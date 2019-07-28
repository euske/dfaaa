#!/usr/bin/env python
import sys
import re
from graph import DFGraph, load_klasses

# methods:
#   words overlap (jaccard)
#   override overridden
#   same arguments
#   same return type

# refs:
#   words overlap (jaccard)
#   same head
#   same type

def jaccard(s1, s2):
    if not s1 or not s2: return 0
    return len(s1.intersection(s2)) / len(s1.union(s2))

def avg(a):
    assert a
    return sum(a) / len(a)

NAME = re.compile(r'\w+$', re.U)
def stripid(name):
    m = NAME.search(name)
    if m:
        return m.group(0)
    else:
        return None

def stripmethodid(x):
    assert '(' in x, x
    (name,_,x) = x.partition('(')
    assert ')' in x, x
    (args,_retype) = x.partition(')')
    if name.endswith(';.<init>'):
        name = name[:-8]
    return (stripid(name), args, retype)

def stripgeneric(name):
    (base,_,_) = name.partition('<')
    return base

WORD = re.compile(r'[a-z]+[A-Z]?|[A-Z]+|[0-9]+')
def splitwords(s):
    """
    >>> splitwords('name')
    ['name']
    >>> splitwords('this_is_name_!')
    ['name', 'is', 'this']
    >>> splitwords('thisIsName')
    ['name', 'is', 'this']
    >>> splitwords('SomeXMLStuff')
    ['stuff', 'xml', 'some']
"""
    if s is None: return []
    n = len(s)
    r = ''.join(reversed(s))
    return [ s[n-m.end(0):n-m.start(0)].lower() for m in WORD.finditer(r) ]


##  ClassDB
##
class ClassDB:

    def __init__(self):
        self.klasses = {}
        self.methods = {}
        self.fields = {}
        return

    def add_klass(self, klass):
        assert klass.name not in self.klasses
        self.klasses[klass.name] = klass
        for method in klass.get_methods():
            assert method.name not in self.methods
            self.methods[method.name] = method
        for (fname,ftype) in klass.fields:
            assert fname not in self.fields
            self.fields[fname] = ftype
        return

    def cmp_methods(self, id1, id2):
        method1 = self.methods[id1]
        method2 = self.methods[id2]
        (name1,args1,retype1) = stripmethodid(method1.name)
        (name2,args2,retype2) = stripmethodid(method2.name)
        overriden = (method1.name in method2.overrides or
                     method2.name in method1.overrides)
        sameargs = (args1 == args2)
        sameretype = (retype1 == retype2)
        words1 = splitwords(name1)
        words2 = splitwords(name2)
        namesim = jaccard(set(words1), set(words2))
        return (namesim, overriden, sameargs, sameretype)

    def cmp_fields(self, ref1, ref2):
        name1 = stripid(ref1)
        name2 = stripid(ref2)
        words1 = splitwords(name1)
        words2 = splitwords(name2)
        namesim = jaccard(set(words1), set(words2))
        type1 = self.fields.get(ref1)
        type2 = self.fields.get(ref2)
        samehead = (words1[0] == words2[0])
        sametype = (type1 == type2)
        return (namesim, samehead, sametype)


# main
def main(argv):
    import fileinput
    import getopt
    def usage():
        print('usage: %s [-d] [graph ...]' % argv[0])
        return 100
    try:
        (opts, args) = getopt.getopt(argv[1:], 'd')
    except getopt.GetoptError:
        return usage()
    debug = 0
    for (k, v) in opts:
        if k == '-d': debug += 1
    if not args: return usage()

    path = args.pop(0)
    print('Loading: %r...' % path, file=sys.stderr)
    db = ClassDB()
    with open(path) as fp:
        for klass in load_klasses(fp):
            db.add_klass(klass)

    path = args.pop(0)
    print('Loading: %r...' % path, file=sys.stderr)
    feats = {}
    with open(path) as fp:
        a = None
        for line in fp:
            if line.startswith('! '):
                data = eval(line[2:])
                if data[0] == 'REF':
                    item = data[1]
                    if item in feats:
                        a = feats[item]
                    else:
                        a = feats[item] = set()
            elif line.startswith('+ '):
                data = eval(line[2:])
                assert a is not None
                #feat = (data[0], data[1], data[2], data[3])
                feat = (data[0], data[2], data[3])
                a.add(feat)
    #
    items = list(feats.keys())
    r = []
    for (i,item0) in enumerate(items):
        feats0 = feats[item0]
        base0 = stripgeneric(item0)
        for item1 in items[i+1:]:
            base1 = stripgeneric(item1)
            if base0 == base1: continue
            feats1 = feats[item1]
            sim = jaccard(feats0, feats1)
            r.append((sim, item0, item1))
    r.sort(reverse=True)
    for (sim,item0,item1) in r[:len(r)//2]:
        score = db.cmp_fields(item0,item1)
        print(sim, stripid(item0), stripid(item1), score)
    return 0

if __name__ == '__main__': sys.exit(main(sys.argv))