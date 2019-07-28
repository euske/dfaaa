#!/usr/bin/env python
import math

class VSM:

    """
>>> sp = VSM()
>>> sp.add('A', {'foo':1, 'baa':1, 'baz':2})
>>> sp.add('B', {'baa':1, 'baz':1})
>>> sp.commit()
>>> sp.calcsim({'baa':1, 'baz':1}, {'baz':2})
0
>>> sp.findsim('A')
[(0, 'B')]
"""

    def __init__(self):
        self.tf = {}
        self.df = {}
        self.idf = None
        self.docs = {}
        return

    def add(self, key, feats):
        a = set()
        for (k,v) in feats.items():
            if k not in self.tf:
                self.tf[k] = 0
            self.tf[k] += v
            a.add(k)
        for k in a:
            if k not in self.df:
                self.df[k] = 0
            self.df[k] += 1
        self.docs[key] = feats
        self.idf = None
        return

    def commit(self):
        n = math.log(len(self.docs))
        self.idf = {None: n}
        for (k,v) in self.df.items():
            if v <= 1: continue
            self.idf[k] = n - math.log(v)
        return

    def calcsim(self, feats1, feats2):
        assert self.idf is not None
        D = self.idf[None]
        f1 = { k1: v1*self.idf.get(k1,D) for (k1,v1) in feats1.items() }
        f2 = { k2: v2*self.idf.get(k2,D) for (k2,v2) in feats2.items() }
        n1 = sum( v*v for v in f1.values() )
        n2 = sum( v*v for v in f2.values() )
        if n1 == 0 or n2 == 0: return 0
        dot = sum( f1[k]*f2[k] for k in f1.keys() if k in f2 )
        return dot/math.sqrt(n1*n2)

    def findsim(self, k0, threshold=0):
        f0 = self.docs[k0]
        maxs = -10
        maxk = None
        a = []
        for (k1,f1) in self.docs.items():
            if k1 == k0: continue
            sim = self.calcsim(f0, f1)
            if sim < threshold: continue
            a.append((sim, k1))
        a.sort(reverse=True)
        return a