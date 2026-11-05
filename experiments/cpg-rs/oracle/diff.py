import re, subprocess, sys, collections, os
SCR = os.path.dirname(os.path.abspath(__file__))
DEFAULT_GOLD = 'goldens/c-tests/run1:goldens/c-tests/run2:goldens/c-tests/run3'
BIN = os.environ.get('CPGDUMP', os.path.join(SCR, '..', 'target', 'release', 'cpgdump'))
SRC = os.environ.get('SRC', os.path.join(SCR, '..', '..', '..', 'cpg-language-cxx', 'src', 'test', 'resources', 'c'))
LINE = re.compile(r"^(\s*)#(\d+) (\w+) name='(.*?)' type='(.*?)' loc=(\S+) code='(.*)'(.*)$")
FIELDS = sys.argv[1].split(',') if len(sys.argv) > 1 else ['cls', 'name', 'loc']
ONLY = sys.argv[2] if len(sys.argv) > 2 else None

def parse(text):
    out = []
    for l in text.splitlines():
        m = LINE.match(l)
        if not m: continue
        ind, nid, cls, name, typ, loc, code, rest = m.groups()
        if cls == 'TranslationUnit': name = os.path.basename(name)  # relative to the invocation
        d = dict(cls=cls, name=name, type=typ, loc=loc, code=code, depth=len(ind)//2, id=nid, rest=rest)
        out.append(d)
    sig = {d['id']: '%s|%s|%s' % (d['cls'], d['name'], d['loc']) for d in out}
    for d in out:
        r = re.search(r"refersTo=(#\d+|-|ext:\w+)", d['rest'])
        d['refersTo'] = (sig.get(r.group(1)[1:], '?') if r.group(1).startswith('#') else r.group(1)) if r else ''
        e = re.search(r"eog=\[(.*?)\]", d['rest'])
        d['eog'] = ','.join(sorted((sig.get(x.split(':')[0][1:], '?') + ('|' + x.split(':')[1] if ':' in x else '')) for x in e.group(1).split(',') if x)) if e else ''
        v = re.search(r"invokes=\[(.*?)\]", d['rest'])
        d['invokes'] = ','.join(sorted(sig.get(x[1:], '?') for x in v.group(1).split(',') if x)) if v else ''
    return out
TYPE_CLASSES = os.environ.get('TYPE_CLASSES', 'ALL')
TYPE_CLASSES = None if TYPE_CLASSES == 'ALL' else set(TYPE_CLASSES.split(','))
def key(d): return tuple((d[f] if f != 'type' or (TYPE_CLASSES is None or d['cls'] in TYPE_CLASSES) else '') for f in FIELDS)

tot_k = tot_r = tot_m = 0
missing = collections.Counter(); extra = collections.Counter()
perfect = 0; rows = []
GOLDS = [g for g in os.environ.get('GOLD', DEFAULT_GOLD).split(':') if g]
files = sorted(f for f in os.listdir(os.path.join(SCR, GOLDS[0])) if f.endswith('.kt.txt'))
for f in files:
    base = f[:-len('.kt.txt')]
    if ONLY and ONLY != base: continue
    r = subprocess.run([BIN, os.path.join(SRC, base)], capture_output=True, text=True)
    rs = parse(r.stdout)
    best = None
    for gd in GOLDS:
        gp = os.path.join(SCR, gd, f)
        if not os.path.exists(gp): continue
        cand = parse(open(gp).read())
        score = sum((collections.Counter(map(key, cand)) & collections.Counter(map(key, rs))).values())
        if best is None or score > best[0]: best = (score, cand)
    k = best[1]
    kc = collections.Counter(map(key, k)); rc = collections.Counter(map(key, rs))
    m = sum((kc & rc).values())
    tot_k += len(k); tot_r += len(rs); tot_m += m
    for x, c in (kc - rc).items(): missing[(x[0], ) + ((x[1],) if len(x) > 1 else ())] += c
    for x, c in (rc - kc).items(): extra[(x[0], ) + ((x[1],) if len(x) > 1 else ())] += c
    if kc == rc and [key(x) for x in k] == [key(x) for x in rs]: perfect += 1
    rows.append((base, len(k), len(rs), m))
    if ONLY:
        print('--- missing (Kotlin only)'); [print(' ', x, c) for x, c in (kc - rc).items()]
        print('--- extra (Rust only)'); [print(' ', x, c) for x, c in (rc - kc).items()]
print(f"fields={FIELDS}  files={len(rows)}  identical-in-order={perfect}")
print(f"kotlin nodes {tot_k}, rust nodes {tot_r}, matched {tot_m}  recall {tot_m/tot_k:.1%}  precision {tot_m/max(tot_r,1):.1%}")
if not ONLY:
    print('worst files:', sorted(rows, key=lambda x: x[3]/max(x[1],1))[:6])
    def by(c):
        t = collections.Counter()
        for k0, n in c.items(): t[k0[0]] += n
        return t.most_common(12)
    print('missing (Kotlin only) by class:', by(missing))
    print('extra   (Rust only)   by class:', by(extra))
