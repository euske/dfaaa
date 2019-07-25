#!/usr/bin/env python
import sys

# main
def main(argv):
    import fileinput
    import getopt
    def usage():
        print('usage: %s [-v] [-t type] [graph ...]' % argv[0])
        return 100
    try:
        (opts, args) = getopt.getopt(argv[1:], 'vt:')
    except getopt.GetoptError:
        return usage()
    verbose = 0
    target = None
    for (k, v) in opts:
        if k == '-v': verbose += 1
        elif k == '-t': target = v
    if not args: return usage()

    feats = {}
    d = None
    for line in fileinput.input(args):
        if line.startswith('! '):
            data = eval(line[2:])
            assert isinstance(data, tuple)
            item = data[0:2]
            if target is not None and data[0] != target:
                d = None
            elif item in feats:
                d = feats[item]
            else:
                d = feats[item] = {}
        elif line.startswith('+ '):
            if d is not None:
                data = eval(line[2:])
                assert isinstance(data, tuple)
                key = data[0:4]
                value = data[4]
                if verbose:
                    if key in d:
                        a = d[key]
                    else:
                        a = d[key] = []
                    a.append(value)
                else:
                    if key in d:
                        d[key] += 1
                    else:
                        d[key] = 1
        else:
            print(line, end='')
    #
    for item in sorted(feats.keys()):
        print('!', item)
        d = feats[item]
        for key in sorted(d.keys()):
            values = d[key]
            if verbose:
                print('+', len(values), key)
                print('#', values)
            else:
                print('+', values, key)
    total = sum( len(d) for d in feats.values() )
    print('%d items, %d keys' % (len(feats), total), file=sys.stderr)
    return 0

if __name__ == '__main__': sys.exit(main(sys.argv))
